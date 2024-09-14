package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.CustomMenuEntryActivity;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.databinding.WherigoActivityBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Zone;

public class WherigoActivity extends CustomMenuEntryActivity {

    private static final String PARAM_WHERIGO_GUID = "wherigo_guid";
    private static final String PARAM_WHERIGO_SCREENID = "wherigo_screenId";

    private final WherigoDownloader wherigoDownloader = new WherigoDownloader(this, this::handleDownloadResult);

    private WherigoActivityBinding binding;
    private int wherigoListenerId;
    private final SimpleItemListModel<Pair<WherigoObjectType, EventTable>> wherigoThingsModel = new SimpleItemListModel<Pair<WherigoObjectType, EventTable>>()
        .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
        .setDisplayMapper(p -> {
            final EventTable t = p.second;
            CharSequence msg = WherigoUtils.eventTableToString(t, false);
            if (!WherigoUtils.isVisibleToPlayer(t)) {
                msg = TextUtils.setSpan(msg, new ForegroundColorSpan(Color.GRAY));
            }
            return TextParam.text(msg);
        })
        .setDisplayIconMapper(et -> {
            final Bitmap bm = WherigoUtils.getEventTableIcon(et.second);
            return bm == null ? ImageParam.id(et.first.getIconId()) : ImageParam.drawable(new BitmapDrawable(getResources(), bm));
        });

    public static void start(final Activity parent, final boolean hideNavigationBar, final int itemTypeOpen) {
        startInternal(parent, intent -> intent.putExtra(PARAM_WHERIGO_SCREENID, itemTypeOpen), hideNavigationBar);
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

    public WherigoActivity() {
        this.wherigoThingsModel.activateGrouping(p -> p.first)
            .setGroupDisplayMapper(gi -> TextParam.text(gi.getGroup().toUserDisplayableString()))
            .setGroupDisplayIconMapper(gi -> ImageParam.id(gi.getGroup().getIconId()))
            .setGroupComparator((g1, g2) -> g1.ordinal() - g2.ordinal(), true);
    }

    @Override
    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.wherigoListenerId = WherigoGame.get().addListener(type -> refreshGui());

        binding = WherigoActivityBinding.inflate(getLayoutInflater());
        setThemeAndContentView(binding);

        final int screenId = getIntent().getExtras() == null ? 0 : getIntent().getExtras().getInt(PARAM_WHERIGO_SCREENID);

        binding.wherigoThingsList.setModel(wherigoThingsModel);
        wherigoThingsModel.addSingleSelectListener(et -> {
            if (et.second.hasEvent("OnClick")) {
                Engine.callEvent(et.second, "OnClick", null);
            } else {
                WherigoDialogManager.get().display(new WherigoThingDialogProvider(et.second));
            }
        });

        refreshGui();
        binding.startGame.setOnClickListener(v -> startGame());
        binding.saveGame.setOnClickListener(v -> saveGame());
        binding.stopGame.setOnClickListener(v -> stopGame());
        binding.download.setOnClickListener(v -> downloadCartridge(null));
        binding.map.setOnClickListener(v -> showOnMap());

        final String guid = getIntent().getExtras() == null ? null : getIntent().getExtras().getString(PARAM_WHERIGO_GUID);
        if (guid != null) {
            final List<WherigoUtils.WherigoCartridgeInfo> infos = WherigoUtils.getAvailableCartridges(PersistableFolder.WHERIGO.getFolder(), info -> guid.equals(info.guid), false, false);
            if (infos.isEmpty()) {
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
        final List<WherigoUtils.WherigoCartridgeInfo> cartridges = WherigoUtils.getAvailableCartridges(PersistableFolder.WHERIGO.getFolder(), null, true, true);
        Collections.sort(cartridges, Comparator.comparing(f -> f.fileInfo.name));

        final Geopoint currentLocation = LocationDataProvider.getInstance().currentGeo().getCoords();

        final SimpleDialog.ItemSelectModel<WherigoUtils.WherigoCartridgeInfo> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(cartridges)
            .setDisplayMapper(info -> TextParam.text(
            info.fileInfo.name + ", " + info.cartridgeFile.name + ", " +
                 info.cartridgeFile.type + ", " + info.cartridgeFile.author + ", " +
                 "v" + info.cartridgeFile.version + ", " +
                 Units.getDistanceFromKilometers(currentLocation.distanceTo(new Geopoint(info.cartridgeFile.latitude, info.cartridgeFile.longitude))) + " away"))
            .setDisplayIconMapper(info -> info.icon != null ? ImageParam.drawable(new BitmapDrawable(getResources(), info.icon)) : ImageParam.id(R.drawable.icon_whereyougo))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this)
            .setTitle(TextParam.text("Choose a Cartridge"))
            .selectSingle(model, this::chooseSavefile);
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
        final List<Zone> zones;
        if (Settings.enableFeatureWherigoDebug()) {
            zones = WherigoGame.get().getZones();
        } else {
            zones = WherigoGame.get().getZones().stream().filter(WherigoUtils::isVisibleToPlayer).collect(Collectors.toList());
        }
        final Viewport viewport = WherigoUtils.getZonesViewport(zones);
        if (viewport != null && !viewport.isJustADot()) {
            DefaultMap.startActivityViewport(this, viewport);
        }
    }

    private void chooseSavefile(final WherigoUtils.WherigoCartridgeInfo cartridge) {

        final Map<String, Date> saveGames = WherigoUtils.getAvailableSaveGames(cartridge.fileInfo);
        if (saveGames.isEmpty()) {
            WherigoGame.get().newGame(cartridge.fileInfo);
            return;
        }

        final List<String> saveGameList = new ArrayList<>(saveGames.keySet());
        Collections.sort(saveGameList);
        saveGameList.add(0, null);

        final SimpleDialog.ItemSelectModel<String> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(saveGameList)
            .setDisplayMapper(s ->  TextParam.text(s == null ? "<New Game>" : s +
                " (" + (saveGames.get(s) == null ? "-" : Formatter.formatDateForFilename(Objects.requireNonNull(saveGames.get(s)).getTime())) + ")"))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this)
            .setTitle(TextParam.text("Choose a Savegame to load"))
            .selectSingle(model, s -> {
                WherigoGame.get().loadGame(cartridge.fileInfo, s);
            });
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
        final List<Pair<WherigoObjectType, EventTable>> allEvents = new ArrayList<>();
        if (game.isPlaying()) {
            addMatching(allEvents, game.getZones(), WherigoObjectType.LOCATION);
            addMatching(allEvents, game.getInventory(), WherigoObjectType.INVENTORY);
            addMatching(allEvents, game.getTasks(), WherigoObjectType.TASK);
            addMatching(allEvents, game.getItems(), WherigoObjectType.ITEM);
            if (Settings.enableFeatureWherigoDebug()) {
                addMatching(allEvents, game.getThings(), WherigoObjectType.THING);
            }
        }
        wherigoThingsModel.setItems(allEvents);

        binding.startGame.setEnabled(!game.isPlaying());
        binding.saveGame.setEnabled(game.isPlaying());
        binding.stopGame.setEnabled(game.isPlaying());
        binding.map.setEnabled(game.isPlaying() && !game.getZones().isEmpty());

    }

    private static void addMatching(final List<Pair<WherigoObjectType, EventTable>> target, final List<? extends EventTable> source, final WherigoObjectType objType) {
        final boolean debug = Settings.enableFeatureWherigoDebug();
        target.addAll(source.stream().filter(et -> debug || WherigoUtils.isVisibleToPlayer(et)).map(et -> new Pair<>(objType, (EventTable) et)).collect(Collectors.toList()));
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
