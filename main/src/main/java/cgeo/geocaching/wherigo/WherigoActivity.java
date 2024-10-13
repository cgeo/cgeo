package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.CustomMenuEntryActivity;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.databinding.WherigoActivityBinding;
import cgeo.geocaching.databinding.WherigolistItemBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.wherigo.WherigoUtils.getDisplayableDistance;
import static cgeo.geocaching.wherigo.WherigoUtils.getDrawableForImageData;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Zone;

public class WherigoActivity extends CustomMenuEntryActivity {

    private static final String PARAM_WHERIGO_GUID = "wherigo_guid";

    private static final List<WherigoThingType> THING_TYPE_LIST = Arrays.asList(WherigoThingType.LOCATION, WherigoThingType.INVENTORY, WherigoThingType.TASK, WherigoThingType.ITEM);
    private static final List<WherigoThingType> THING_TYPE_LIST_DEBUG = Arrays.asList(WherigoThingType.LOCATION, WherigoThingType.INVENTORY, WherigoThingType.TASK, WherigoThingType.ITEM, WherigoThingType.THING);

    private final WherigoDownloader wherigoDownloader = new WherigoDownloader(this, this::handleDownloadResult);

    private WherigoActivityBinding binding;
    private int wherigoListenerId;

    private final SimpleItemListModel<WherigoThingType> wherigoThingTypeModel = new SimpleItemListModel<WherigoThingType>()
        .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
        .setDisplayViewMapper(R.layout.wherigolist_item, (type, group, view) -> {
            view.setBackground(null);
            final List<EventTable> things = type.getThingsForUserDisplay();
            final String name = type.toUserDisplayableString() + " (" + things.size() + ")";
            final CharSequence description = TextUtils.join(things, i -> {
                final String thingName = i.name;
                return !WherigoUtils.isVisibleToPlayer(i) ? thingName : TextUtils.setSpan(thingName, new ForegroundColorSpan(Color.BLUE));
            }, ", ");

            final WherigolistItemBinding typeBinding = WherigolistItemBinding.bind(view);
            typeBinding.name.setText(name);
            typeBinding.description.setText(description);
            ImageParam.id(type.getIconId()).applyTo(typeBinding.icon);
        }, (item, itemGroup) -> item == null  ? "" : item.toUserDisplayableString())      ;

    public static void start(final Activity parent, final boolean hideNavigationBar) {
        startInternal(parent, null, hideNavigationBar);
    }

    public static void startForGuid(final Activity parent, final String guid, final boolean hideNavigationBar) {
        startInternal(parent, intent -> intent.putExtra(PARAM_WHERIGO_GUID, guid), hideNavigationBar);
    }

    private static void startInternal(final Activity parent, final Consumer<Intent> intentModifier, final boolean hideNavigationBar) {
        final Intent intent = new Intent(parent, WherigoActivity.class);
        if (intentModifier != null) {
            intentModifier.accept(intent);
        }
        AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavigationBar);
        parent.startActivity(intent);
    }

    @Override
    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialogs.basicOneTimeMessage(this, OneTimeDialogs.DialogType.WHERIGO_PLAYER_SHORTCUTS);

        this.wherigoListenerId = WherigoGame.get().addListener(type -> refreshGui());

        binding = WherigoActivityBinding.inflate(getLayoutInflater());
        setThemeAndContentView(binding);

        binding.wherigoThingTypeList.setModel(wherigoThingTypeModel);
        wherigoThingTypeModel.addSingleSelectListener(type -> {
            WherigoDialogManager.get().display(new WherigoThingListDialogProvider(type));
        });

        refreshGui();
        binding.viewCartridges.setOnClickListener(v -> startGame());
        binding.saveGame.setOnClickListener(v -> saveGame());
        binding.stopGame.setOnClickListener(v -> stopGame());
        binding.download.setOnClickListener(v -> manualCartridgeDownload());
        binding.reportProblem.setOnClickListener(v -> {
            WherigoDialogManager.get().display(new WherigoErrorDialogProvider());
        });

        binding.map.setOnClickListener(v -> showOnMap());

        binding.cartridgeDetails.setOnClickListener(v -> {
            final WherigoCartridgeInfo info = WherigoGame.get().getCartridgeInfo();
            if (info != null) {
                WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(info));
            }
        });
        binding.revokeFixedLocation.setOnClickListener(v -> {
            WherigoLocationProvider.get().setFixedLocation(null);
        });

        final String guid = getIntent().getExtras() == null ? null : getIntent().getExtras().getString(PARAM_WHERIGO_GUID);
        if (guid != null) {
            handleCGuidInput(guid);

        }
    }

    private void startGame() {
        chooseCartridge();
    }

    private void chooseCartridge() {
        final List<WherigoCartridgeInfo> cartridges = WherigoCartridgeInfo.getAvailableCartridges(null);
        Collections.sort(cartridges, Comparator.comparing(f -> f.getCartridgeFile().name));

        final SimpleDialog.ItemSelectModel<WherigoCartridgeInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(cartridges)
            .setDisplayViewMapper(R.layout.wherigolist_item, (info, group, view) -> {

                final String name = info.getCartridgeFile().name;
                final CharSequence description = "v" + info.getCartridgeFile().version + ", " + info.getCartridgeFile().author + ", " +
                        getDisplayableDistance(LocationDataProvider.getInstance().currentGeo().getCoords(),
                                new Geopoint(info.getCartridgeFile().latitude, info.getCartridgeFile().longitude));
                final byte[] iconData = info.getIconData();
                final ImageParam icon = iconData == null ? ImageParam.id(R.drawable.type_wherigo) :
                        ImageParam.drawable(getDrawableForImageData(null, iconData));

                final WherigolistItemBinding binding = WherigolistItemBinding.bind(view);
                binding.name.setText(name);
                binding.description.setText(description);
                icon.applyTo(binding.icon);
            }, (item, itemGroup) -> item == null || item.getCartridgeFile() == null ? "" : item.getCartridgeFile().name)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this)
            .setTitle(TextParam.id(R.string.wherigo_choose_cartridge))
            .selectSingle(model, cartridgeInfo -> {
                WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(cartridgeInfo));
            });
    }

    private void saveGame() {
        final WherigoCartridgeInfo cartridgeInfo = WherigoGame.get().getCartridgeInfo();
        if (cartridgeInfo == null) {
            return;
        }

        final SimpleDialog.ItemSelectModel<WherigoSavegameInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(cartridgeInfo.getSavegameSlots())
            .setDisplayViewMapper(R.layout.wherigolist_item, (si, group, view) -> {

                final WherigolistItemBinding itemBinding = WherigolistItemBinding.bind(view);
                itemBinding.name.setText(si.getUserDisplayableName());
                itemBinding.description.setText(si.getUserDisplayableSaveDate());
                itemBinding.icon.setImageResource(R.drawable.ic_menu_save);
                }, (item, itemGroup) -> item == null || item.name == null ? "" : item.name)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this)
            .setTitle(TextParam.id(R.string.wherigo_choose_savegame_slot))
            .selectSingle(model, s -> {
                if (s.saveDate == null) {
                    WherigoGame.get().saveGame(s.name);
                } else {
                    SimpleDialog.of(this)
                        .setTitle(TextParam.id(R.string.wherigo_confirm_overwrite_savegame_slot_title))
                        .setMessage(TextParam.id(R.string.wherigo_confirm_overwrite_savegame_slot_title, s.getUserDisplayableSaveDate()))
                        .confirm(() -> WherigoGame.get().saveGame(s.name));
                }
            });
    }

    private void stopGame() {
        WherigoUtils.ensureNoGameRunning(this, null);
    }

    private void showOnMap() {
        if (!WherigoGame.get().isPlaying()) {
            return;
        }
        final List<Zone> zones = WherigoThingType.LOCATION.getThingsForUserDisplay(Zone.class);
        final Viewport viewport = WherigoUtils.getZonesViewport(zones);
        if (viewport != null && !viewport.isJustADot()) {
            DefaultMap.startActivityViewport(this, viewport);
        }
    }

    private void manualCartridgeDownload() {
        SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_manual_download_title))
            .input(new SimpleDialog.InputOptions().setLabel(LocalizationUtils.getString(R.string.wherigo_manual_download_cguid_label)).setInitialValue(""), input -> {
                wherigoDownloader.downloadWherigo(input, name -> ContentStorage.get().create(PersistableFolder.WHERIGO, name));
            });
    }

    private void handleCGuidInput(final String cguid) {
        final WherigoCartridgeInfo cguidCartridge = WherigoCartridgeInfo.getCartridgeForCGuid(cguid);
        if (cguidCartridge == null) {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_download_title))
                .setMessage(TextParam.id(R.string.wherigo_download_message, cguid))
                .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
                .confirm(() -> {
                    wherigoDownloader.downloadWherigo(cguid, name -> ContentStorage.get().create(PersistableFolder.WHERIGO, name));
                });
        } else {
            WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(cguidCartridge));
        }
    }

    private void handleDownloadResult(final String cguid, final StatusResult result) {
        final WherigoCartridgeInfo cartridgeInfo = WherigoCartridgeInfo.getCartridgeForCGuid(cguid);
        if (result.isOk() && cartridgeInfo != null) {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_download_successful_title))
                .setMessage(TextParam.id(R.string.wherigo_download_successful_message, cguid))
                .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
                .confirm(() -> WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(cartridgeInfo)));
        } else {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_download_failed_title))
                .setMessage(TextParam.id(R.string.wherigo_download_failed_message, cguid, String.valueOf(result)))
                .show();
        }
    }

    private void refreshGui() {
        final WherigoGame game = WherigoGame.get();

        wherigoThingTypeModel.setItems(game.isDebugModeForCartridge() ? THING_TYPE_LIST_DEBUG : THING_TYPE_LIST);

        binding.wherigoThingTypeList.setVisibility(game.isPlaying() ? View.VISIBLE : View.GONE);
        binding.wherigoCartridgeInfos.setVisibility(game.isPlaying() ? View.VISIBLE : View.GONE);
        binding.cartridgeTitle.setText(game.getCartridgeName());

        binding.gameLocation.setText(LocalizationUtils.getString(R.string.cache_location) + ": " +
                WherigoLocationProvider.get().toUserDisplayableString());
        binding.revokeFixedLocation.setVisibility(game.isDebugModeForCartridge() && WherigoLocationProvider.get().hasFixedLocation() ? View.VISIBLE : View.GONE);

        binding.viewCartridges.setEnabled(true);
        binding.saveGame.setEnabled(game.isPlaying());
        binding.stopGame.setEnabled(game.isPlaying());
        binding.reportProblem.setEnabled(game.isPlaying());
        binding.map.setEnabled(game.isPlaying() && !game.getZones().isEmpty());

    }

    @Override
    public final void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public final void onResume() {
        super.onResume();
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        WherigoGame.get().removeListener(wherigoListenerId);
    }

}
