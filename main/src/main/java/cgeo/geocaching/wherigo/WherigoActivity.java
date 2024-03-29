package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.CustomMenuEntryActivity;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.databinding.WherigoActivityBinding;
import cgeo.geocaching.location.Geopoint;
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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Zone;
import cz.matejcik.openwig.formats.CartridgeFile;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public class WherigoActivity extends CustomMenuEntryActivity {

    private final WherigoDownloader wherigoDownloader = new WherigoDownloader(this, result -> handleDownloadResult(result));

    private WherigoActivityBinding binding;
    private int wherigoListenerId;
    private final SimpleItemListModel<EventTable> wherigoThingsModel = new SimpleItemListModel<EventTable>()
        .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
        .setDisplayMapper(t -> {
            CharSequence msg = WherigoUtils.eventTableToString(t, false);
            if (!WherigoUtils.isVisibleToPlayer(t)) {
                msg = TextUtils.setSpan(msg, new ForegroundColorSpan(Color.GRAY));
            }
            return TextParam.text(msg);
        })
        .setDisplayIconMapper(et -> {
            final Bitmap bm = WherigoUtils.getEventTableIcon(et);
            return bm == null ? imageForEventType(et == null ? null : et.getClass()) : ImageParam.drawable(new BitmapDrawable(getResources(), bm));
        });

    public static void start(final Activity parent, final boolean hideNavigationBar) {
        final Intent intent = new Intent(parent, WherigoActivity.class);
        AbstractNavigationBarActivity.setIntentHideBottomNavigation(intent, hideNavigationBar);
        parent.startActivity(intent);
    }

    public WherigoActivity() {
        this.wherigoThingsModel.activateGrouping(EventTable::getClass)
            .setGroupDisplayMapper((c, cnt) -> TextParam.text(c.getSimpleName()))
            .setGroupDisplayIconMapper((c, cnt) -> WherigoActivity.imageForEventType(c));
    }

    private static <T extends EventTable> ImageParam imageForEventType(final Class<T> c) {
        if (Zone.class.isAssignableFrom(c)) {
            return ImageParam.id(R.drawable.ic_menu_mapmode);
        }
        if (Task.class.isAssignableFrom(c)) {
            return ImageParam.id(R.drawable.ic_menu_myplaces);
        }
        return ImageParam.id(R.drawable.ic_menu_list);
    }

    @Override
    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.wherigoListenerId = WherigoGame.get().addListener(type -> refreshGui());

        binding = WherigoActivityBinding.inflate(getLayoutInflater());
        setThemeAndContentView(binding);

        binding.wherigoThingsList.setModel(wherigoThingsModel);
        wherigoThingsModel.addSingleSelectListener(et -> {
            if (et.hasEvent("OnClick")) {
                Engine.callEvent(et, "OnClick", null);
            } else {
                WherigoDialogManager.get().display(new WherigoThingDialogProvider(et));
            }
        });

        refreshGui();
        binding.startGame.setOnClickListener(v -> startGame());
        binding.saveGame.setOnClickListener(v -> saveGame());
        binding.stopGame.setOnClickListener(v -> stopGame());
        binding.download.setOnClickListener(v -> downloadCartridge());
    }

    private void startGame() {
        chooseCartridge();
    }

    private void chooseCartridge() {
        final Map<ContentStorage.FileInformation, CartridgeFile> cartridges = WherigoGame.getAvailableCartridges(PersistableFolder.WHERIGO.getFolder());
        final Map<ContentStorage.FileInformation, ImmutableTriple<String, Bitmap, Geopoint>> displayDataMap = new HashMap<>();
        for (Map.Entry<ContentStorage.FileInformation, CartridgeFile> cart : cartridges.entrySet()) {
            final CartridgeFile file = cart.getValue();
            final String msg = cart.getKey().name + ", " + file.name + ", " + file.type + ", " + file.author + ", " + file.version;
            final Geopoint point = new Geopoint(file.latitude, file.longitude);
            final Bitmap bmp = WherigoUtils.getCartrdigeIcon(file);
            displayDataMap.put(cart.getKey(), new ImmutableTriple<>(msg, bmp, point));
            WherigoUtils.closeCartridgeQuietly(file);
        }
        final List<ContentStorage.FileInformation> files = new ArrayList<>(displayDataMap.keySet());
        Collections.sort(files, Comparator.comparing(f -> f.name));

        final SimpleDialog.ItemSelectModel<ContentStorage.FileInformation> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(files)
            .setDisplayMapper((s) -> TextParam.text(displayDataMap.get(s).left + ", " + displayDataMap.get(s).right))
            .setDisplayIconMapper(s -> displayDataMap.containsKey(s) && displayDataMap.get(s).middle != null ?
                ImageParam.drawable(new BitmapDrawable(getResources(), displayDataMap.get(s).middle)) : ImageParam.id(R.drawable.icon_whereyougo))
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

    private void chooseSavefile(final ContentStorage.FileInformation cartridge) {

        final Map<String, Date> saveGames = WherigoGame.getAvailableSaveGames(cartridge);
        if (saveGames.isEmpty()) {
            WherigoGame.get().newGame(cartridge);
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
                WherigoGame.get().loadGame(cartridge, s);
            });
    }

    private void downloadCartridge() {
        SimpleDialog.of(this).setTitle(TextParam.text("Download"))
            .input(new SimpleDialog.InputOptions().setLabel("Enter CGID").setInitialValue("f6002f33-c68a-4966-82ca-3c392a85d892"), input -> {
                wherigoDownloader.downloadWherigo(input, name -> ContentStorage.get().create(PersistableFolder.WHERIGO, name));
            });
    }

    private void handleDownloadResult(final StatusResult result) {
        SimpleDialog.of(this).setTitle(TextParam.text("Download result"))
            .setMessage(TextParam.text("Download result:" + result)).show();
    }

    private void refreshGui() {
        final WherigoGame game = WherigoGame.get();
        final List<EventTable> allEvents;
        if (!game.isPlaying()) {
            allEvents = Collections.emptyList();
        } else if (Settings.enableFeatureWherigoDebug()) {
            allEvents = game.getAllEventTables();
        } else {
            allEvents = game.getAllEventTables().stream().filter(WherigoUtils::isVisibleToPlayer).collect(Collectors.toList());
        }
        wherigoThingsModel.setItems(allEvents);

        binding.startGame.setEnabled(!game.isPlaying());
        binding.saveGame.setEnabled(game.isPlaying());
        binding.stopGame.setEnabled(game.isPlaying());

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
