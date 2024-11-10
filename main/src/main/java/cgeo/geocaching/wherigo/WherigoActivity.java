package cgeo.geocaching.wherigo;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.CustomMenuEntryActivity;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.databinding.WherigoActivityBinding;
import cgeo.geocaching.databinding.WherigolistItemBinding;
import cgeo.geocaching.enumerations.QuickLaunchItem;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;
import static cgeo.geocaching.wherigo.WherigoUtils.getDisplayableDistance;
import static cgeo.geocaching.wherigo.WherigoUtils.getDrawableForImageData;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import cz.matejcik.openwig.Zone;

public class WherigoActivity extends CustomMenuEntryActivity {

    private static final String PARAM_WHERIGO_GUID = "wherigo_guid";
    private static final String PARAM_WHERIGO_GEOCODE = "wherigo_geocode";

    private final WherigoDownloader wherigoDownloader = new WherigoDownloader(this, this::handleDownloadResult);

    private WherigoActivityBinding binding;
    private int wherigoListenerId;

    private SimpleItemListModel<WherigoThingType> wherigoThingTypeModel;

    public static void start(final Activity parent, final boolean forceHideNavigationBar) {
        startInternal(parent, null, forceHideNavigationBar);
    }

    public static void startForGuid(final Activity parent, final String guid, final String geocode, final boolean forceHideNavigationBar) {
        startInternal(parent, intent -> {
            intent.putExtra(PARAM_WHERIGO_GUID, guid);
            intent.putExtra(PARAM_WHERIGO_GEOCODE, geocode);
        }, forceHideNavigationBar);
    }

    private static void startInternal(final Activity parent, final Consumer<Intent> intentModifier, final boolean forceHideNavigationBar) {
        final Intent intent = new Intent(parent, WherigoActivity.class);
        if (intentModifier != null) {
            intentModifier.accept(intent);
        }
        final boolean wherigoIsCustomItem = Settings.getCustomBNitem() == QuickLaunchItem.VALUES.WHERIGO.id;
        final boolean hideNavBar = !wherigoIsCustomItem || forceHideNavigationBar;

        AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavBar);
        parent.startActivity(intent);
    }

    @Override
    protected void checkIntentHideNavigationBar() {
        //by default show nav bar if wherigo is custom select item
        checkIntentHideNavigationBar(Settings.getCustomBNitem() != QuickLaunchItem.VALUES.WHERIGO.id);
    }

    @Override
    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialogs.basicOneTimeMessage(this, OneTimeDialogs.DialogType.WHERIGO_PLAYER_SHORTCUTS);

        this.wherigoListenerId = WherigoGame.get().addListener(type -> refreshGui());

        binding = WherigoActivityBinding.inflate(getLayoutInflater());
        setThemeAndContentView(binding);

        wherigoThingTypeModel = WherigoViewUtils.createThingTypeTable(this, binding.wherigoThingTypeList, thing -> {
            WherigoViewUtils.displayThing(this, thing, false);
        });

        refreshGui();

        binding.viewCartridges.setOnClickListener(v -> startGame());
        binding.resumeDialog.setOnClickListener(v -> WherigoDialogManager.get().unpause());
        binding.loadGame.setOnClickListener(v -> loadGame());
        binding.saveGame.setOnClickListener(v -> saveGame());
        binding.stopGame.setOnClickListener(v -> stopGame());
        binding.download.setOnClickListener(v -> manualCartridgeDownload());
        binding.reportProblem.setOnClickListener(v -> WherigoViewUtils.showErrorDialog(this));
        binding.map.setOnClickListener(v -> showOnMap());
        binding.cacheContextGotocache.setOnClickListener(v -> {
            goToCache(WherigoGame.get().getContextGeocode());
        });
        binding.cacheContextRemove.setOnClickListener(v -> {
            WherigoGame.get().setContextGeocode(null);
        });

        binding.revokeFixedLocation.setOnClickListener(v -> {
            WherigoLocationProvider.get().setFixedLocation(null);
        });

        if (getIntent().getExtras() != null) {
            final String guid = getIntent().getExtras().getString(PARAM_WHERIGO_GUID);
            if (guid != null) {
                handleCGuidInput(guid);
            }
        }
        ViewUtils.addBadge(binding.resumeDialog, false, -1);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.wherigo_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int menuItem = item.getItemId();
        if (menuItem == R.id.menu_show_cartridge) {
            final WherigoCartridgeInfo info = WherigoGame.get().getCartridgeInfo();
            if (info != null) {
                WherigoDialogManager.displayDirect(this, new WherigoCartridgeDialogProvider(info, true));
            } else {
                SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_player))
                        .setMessage(TextParam.id(R.string.wherigo_no_game_running)).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                final ImageParam icon = iconData == null ? ImageParam.id(R.drawable.type_marker_wherigo) :
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
                WherigoDialogManager.displayDirect(this, new WherigoCartridgeDialogProvider(cartridgeInfo, false));
            });
    }

    private void loadGame() {
        final WherigoCartridgeInfo cartridgeInfo = WherigoGame.get().getCartridgeInfo();
        if (cartridgeInfo == null) {
            return;
        }
        WherigoUtils.ensureNoGameRunning(this, () -> {
            WherigoUtils.loadGame(this, cartridgeInfo);
        });
    }

    private void saveGame() {
        WherigoUtils.saveGame(this);
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

    private void goToCache(final String geocode) {
        CacheDetailActivity.startActivity(this, geocode);
    }

    private void manualCartridgeDownload() {
        SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_manual_download_title))
            .input(new SimpleDialog.InputOptions().setLabel(LocalizationUtils.getString(R.string.wherigo_manual_download_cguid_label)).setInitialValue(""), input -> {
                wherigoDownloader.downloadWherigo(input, name -> ContentStorage.get().create(PersistableFolder.WHERIGO, name));
            });
    }

    private void handleCGuidInput(@NonNull final String cguid) {
        final WherigoCartridgeInfo cguidCartridge = WherigoCartridgeInfo.getCartridgeForCGuid(cguid);
        if (cguidCartridge == null) {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_download_title))
                .setMessage(TextParam.id(R.string.wherigo_download_message, cguid))
                .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
                .confirm(() -> {
                    wherigoDownloader.downloadWherigo(cguid, name -> ContentStorage.get().create(PersistableFolder.WHERIGO, name));
                });
        } else {
            final String geocode = getStartingGeocode();
            if (geocode != null) {
                WherigoGame.get().setContextGeocode(geocode);
            }
            WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(cguidCartridge, false));
        }
    }

    private String getStartingGeocode() {
        final Intent intent = getIntent();
        return intent == null || intent.getExtras() == null ? null : intent.getExtras().getString(PARAM_WHERIGO_GEOCODE);
    }

    private void handleDownloadResult(final String cguid, final StatusResult result) {
        final WherigoCartridgeInfo cartridgeInfo = WherigoCartridgeInfo.getCartridgeForCGuid(cguid);
        if (result.isOk() && cartridgeInfo != null) {
            ActivityMixin.showToast(this, R.string.wherigo_download_successful_title);
            final String geocode = getStartingGeocode();
            if (geocode != null) {
                WherigoGame.get().setContextGeocode(geocode);
            }
            WherigoDialogManager.displayDirect(this, new WherigoCartridgeDialogProvider(cartridgeInfo, false));
        } else {
            SimpleDialog.of(this).setTitle(TextParam.id(R.string.wherigo_download_failed_title))
                .setMessage(TextParam.id(R.string.wherigo_download_failed_message, cguid, String.valueOf(result)))
                .show();
        }
    }

    private void refreshGui() {
        final WherigoGame game = WherigoGame.get();

        WherigoViewUtils.updateThingTypeTable(wherigoThingTypeModel, binding.wherigoThingTypeList);

        binding.wherigoCartridgeInfos.setVisibility(game.isPlaying() ? View.VISIBLE : View.GONE);

        binding.gameLocation.setText(LocalizationUtils.getString(R.string.cache_location) + ": " +
                WherigoLocationProvider.get().toUserDisplayableString());
        binding.revokeFixedLocation.setVisibility(game.isDebugModeForCartridge() && WherigoLocationProvider.get().hasFixedLocation() ? View.VISIBLE : View.GONE);

        binding.viewCartridges.setEnabled(true);
        binding.download.setEnabled(true);
        binding.reportProblem.setEnabled(true);

        binding.resumeDialog.setVisibility(game.dialogIsPaused() ? View.VISIBLE : View.GONE);
        binding.saveGame.setEnabled(game.isPlaying());
        binding.loadGame.setEnabled(game.isPlaying() && WherigoSavegameInfo.getLoadableSavegames(game.getCartridgeInfo().getFileInfo()).size() > 1);
        binding.stopGame.setEnabled(game.isPlaying());
        binding.map.setEnabled(game.isPlaying() && !WherigoThingType.LOCATION.getThingsForUserDisplay().isEmpty());

        this.setTitle(game.isPlaying() ? game.getCartridgeName() : getString(R.string.wherigo_player));

        binding.cacheContextBox.setVisibility(game.getContextGeocode() != null ? View.VISIBLE : View.GONE);
        binding.cacheContextName.setText(game.getContextGeocacheName());
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
