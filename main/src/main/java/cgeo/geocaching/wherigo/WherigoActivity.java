package cgeo.geocaching.wherigo;

import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.CustomMenuEntryActivity;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.databinding.WherigoActivityBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import cz.matejcik.openwig.Zone;

public class WherigoActivity extends CustomMenuEntryActivity {

    private static final String PARAM_WHERIGO_GUID = "wherigo_guid";

    private final WherigoDownloader wherigoDownloader = new WherigoDownloader(this, this::handleDownloadResult);

    private WherigoActivityBinding binding;
    private int wherigoListenerId;

    private final SimpleItemListModel<WherigoThingType> wherigoThingTypeModel = new SimpleItemListModel<WherigoThingType>()
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setDisplayViewMapper(WherigoUtils.getWherigoThingTypeDisplayMapper(),
                (item, itemGroup) -> item == null  ? "" : item.toUserDisplayableString());

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
        this.wherigoListenerId = WherigoGame.get().addListener(type -> refreshGui());

        binding = WherigoActivityBinding.inflate(getLayoutInflater());
        setThemeAndContentView(binding);

        binding.wherigoThingTypeList.setModel(wherigoThingTypeModel);
        wherigoThingTypeModel.addSingleSelectListener(type -> {
            WherigoDialogManager.get().display(new WherigoThingListDialogProvider(type));
        });

        refreshGui();
        binding.startGame.setOnClickListener(v -> startGame());
        binding.saveGame.setOnClickListener(v -> saveGame());
        binding.stopGame.setOnClickListener(v -> stopGame());
        binding.download.setOnClickListener(v -> downloadCartridge(null));
        binding.map.setOnClickListener(v -> showOnMap());

        binding.cartridgeDetails.setOnClickListener(v -> {
            final WherigoCartridgeInfo info = WherigoGame.get().getCartridgeInfo();
            if (info != null) {
                WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(info));
            }
        });

        final String guid = getIntent().getExtras() == null ? null : getIntent().getExtras().getString(PARAM_WHERIGO_GUID);
        if (guid != null) {
            final WherigoCartridgeInfo cguidCartridge = WherigoCartridgeInfo.getCartridgeForCGuid(guid);
            if (cguidCartridge == null) {
                downloadCartridge(guid);
            } else {
                SimpleDialog.of(this).setTitle(TextParam.text("Wherigo")).setMessage(TextParam.text("Cartridge already available")).show();
            }
        }
    }

    private void startGame() {
        chooseCartridge();
    }

    private void chooseCartridge() {
        final List<WherigoCartridgeInfo> cartridges = WherigoCartridgeInfo.getAvailableCartridges(null);
        Collections.sort(cartridges, Comparator.comparing(f -> f.getFileInfo().name));

        final Geopoint currentLocation = LocationDataProvider.getInstance().currentGeo().getCoords();

        final SimpleDialog.ItemSelectModel<WherigoCartridgeInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(cartridges)
            .setDisplayViewMapper(WherigoUtils.getCartridgeDisplayMapper(), (item, itemGroup) -> item == null || item.getCartridgeFile() == null ? "" : item.getCartridgeFile().name)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this)
            .setTitle(TextParam.text("Choose a Cartridge"))
            .selectSingle(model, cartridgeInfo -> {
                WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(cartridgeInfo));
            });
    }

    private void saveGame() {
        SimpleDialog.of(this)
            .setTitle(TextParam.text("Save Game"))
            .setMessage(TextParam.text("Enter Save Game Id"))
            .input(null, saveName -> {
                WherigoGame.get().saveGame(saveName);
            });
    }

    private void stopGame() {
        WherigoGame.get().stopGame();
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

    private void downloadCartridge(final String guid) {
        SimpleDialog.of(this).setTitle(TextParam.text("Download"))
            .input(new SimpleDialog.InputOptions().setLabel("Enter CGID").setInitialValue(guid == null ? "f6002f33-c68a-4966-82ca-3c392a85d892" : guid), input -> {
                wherigoDownloader.downloadWherigo(input, name -> ContentStorage.get().create(PersistableFolder.WHERIGO, name));
            });
    }

    private void handleDownloadResult(final StatusResult result) {
        SimpleDialog.of(this).setTitle(TextParam.text("Download result"))
            .setMessage(TextParam.text("Download result:" + result)).show();
    }

    private void refreshGui() {
        final WherigoGame game = WherigoGame.get();
        final WherigoCartridgeInfo info = game.getCartridgeInfo();

        wherigoThingTypeModel.setItems(Arrays.asList(WherigoThingType.LOCATION, WherigoThingType.INVENTORY, WherigoThingType.TASK, WherigoThingType.ITEM, WherigoThingType.THING));

        binding.wherigoThingTypeList.setVisibility(game.isPlaying() ? View.VISIBLE : View.GONE);
        binding.wherigoCartridgeInfos.setVisibility(game.isPlaying() ? View.VISIBLE : View.GONE);
        binding.cartridgeTitle.setText(info == null || info.getCartridgeFile() == null ? "" : info.getCartridgeFile().name);

        binding.startGame.setEnabled(!game.isPlaying());
        binding.saveGame.setEnabled(game.isPlaying());
        binding.stopGame.setEnabled(game.isPlaying());
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
