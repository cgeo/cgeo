package cgeo.geocaching.wherigo;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Cartridge;
import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.EventTable;
import cz.matejcik.openwig.Media;
import cz.matejcik.openwig.Player;
import cz.matejcik.openwig.Task;
import cz.matejcik.openwig.Thing;
import cz.matejcik.openwig.Zone;
import cz.matejcik.openwig.ZonePoint;
import cz.matejcik.openwig.formats.CartridgeFile;
import cz.matejcik.openwig.platform.UI;
import se.krka.kahlua.vm.LuaClosure;

public class WherigoGame implements UI {

    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    public enum NotifyType {
        REFRESH, START, END, LOCATION
    }

    private final Object mutex = new Object();

    private CartridgeFile cartridgeFile;
    private Cartridge cartridge;

    private boolean isPlaying = false;

    private static final AtomicInteger LISTENER_ID_PROVIDER = new AtomicInteger(0);
    private final Map<Integer, Consumer<NotifyType>> listeners = new HashMap<>();


    private static final WherigoGame INSTANCE = new WherigoGame();

    public static WherigoGame get() {
        return INSTANCE;
    }

    private WherigoGame() {
        //singleton
    }

    public int addListener(final Consumer<NotifyType> listener) {
        final int listenerId = LISTENER_ID_PROVIDER.addAndGet(1);
        this.listeners.put(listenerId, listener);
        return listenerId;
    }

    public void removeListener(final int listenerId) {
        this.listeners.remove(listenerId);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public static Map<ContentStorage.FileInformation, CartridgeFile> getAvailableCartridges(final Folder folder) {
        final List<ContentStorage.FileInformation> candidates = ContentStorage.get().list(folder).stream()
            .filter(fi -> fi.name.endsWith(".gwc")).collect(Collectors.toList());
        final Map<ContentStorage.FileInformation, CartridgeFile> result = new HashMap<>();
        for (ContentStorage.FileInformation candidate : candidates) {
            final CartridgeFile cartridgeFile = WherigoUtils.safeReadCartridge(candidate.uri);
            if (cartridgeFile != null) {
                result.put(candidate, cartridgeFile);
            }
        }
        return result;
    }

    public static Map<String, Date> getAvailableSaveGames(@NonNull final ContentStorage.FileInformation cartridgeInfo) {
        return WherigoSaveFileHandler.getAvailableSaveFiles(cartridgeInfo.parentFolder, cartridgeInfo.name);
    }

    public void newGame(@NonNull final ContentStorage.FileInformation cartridgeInfo) {
        loadGame(cartridgeInfo, null);
    }

    public void loadGame(@NonNull final ContentStorage.FileInformation cartridgeInfo, @Nullable final String saveGame) {
        if (isPlaying()) {
            return;
        }
        try {
            WherigoSaveFileHandler.get().setCartridge(cartridgeInfo.parentFolder, cartridgeInfo.name);
            if (saveGame != null) {
                WherigoSaveFileHandler.get().initLoad(saveGame);
            }
            this.cartridgeFile = WherigoUtils.readCartridge(cartridgeInfo.uri);

            final Engine engine = Engine.newInstance(this.cartridgeFile, null, this, new WLocationService());
            if (saveGame != null) {
                engine.restore();
            } else {
                engine.start();
            }
        } catch (IOException ie) {
            Log.e("Problem", ie);
        }
    }

    public void saveGame(final String saveGame) {
        if (!isPlaying()) {
            return;
        }
        WherigoSaveFileHandler.get().save(saveGame);
    }

    public void stopGame() {
        if (!isPlaying()) {
            return;
        }
        Engine.kill();
    }

    @SuppressWarnings("unchecked")
    public List<Zone> getZones() {
        return cartridge == null ? Collections.emptyList() : (List < Zone >) cartridge.zones;
    }

    public Zone getZone(final String name) {
        for (Zone zone : getZones()) {
            if (zone != null && Objects.equals(zone.name, name)) {
                return zone;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Thing> getThings() {
        return cartridge == null ? Collections.emptyList() : (List<Thing>) cartridge.things;
    }

    @SuppressWarnings("unchecked")
    public List<Task> getTasks() {
        return cartridge == null ? Collections.emptyList() : (List<Task>) cartridge.tasks;
    }

    public Player getPlayer() {
        return Engine.instance.player;
    }



    public List<EventTable> getAllEventTables() {
        final List<EventTable> result = new ArrayList<>();
        result.addAll(getZones());
        result.addAll(getThings());
        result.addAll(getTasks());
        return result;
    }

    public void notifyListeners(final NotifyType type) {
        runOnUi(() -> {
            Log.d("WHERIGOGAME: notify for " + type);
            for (Consumer<NotifyType> listener : listeners.values()) {
                listener.accept(type);
            }
        });
    }

    private void runOnUi(final Runnable r) {
        AndroidRxUtils.runOnUi(r);
    }

    @Override
    public void refresh() {
        notifyListeners(NotifyType.REFRESH);
    }

    @Override
    public void start() {
        this.cartridge = Engine.instance.cartridge;
        isPlaying = true;
        Log.iForce("WHERIGO pos: " + GP_CONVERTER.from(cartridge.position));
        notifyListeners(NotifyType.START);
        WherigoSaveFileHandler.get().loadSaveFinished(); // ends a probable LOAD
    }

    @Override
    public void end() {
        isPlaying = false;
        cartridge = null;
        notifyListeners(NotifyType.END);
        WherigoUtils.closeCartridgeQuietly(this.cartridgeFile);
    }

    @Override
    public void showError(final String s) {
        setStatusText("ERROR:" + s);
    }

    @Override
    public void debugMsg(final String s) {
        Log.w("WHERIGO: " + s);
    }

    @Override
    public void setStatusText(final String s) {
        runOnUi(() -> ActivityMixin.showApplicationToast("WHERIGO:" + s));
    }

    @Override
    public void pushDialog(final String[] strings, final Media[] media, final String s, final String s1, final LuaClosure luaClosure) {
        WherigoDialogManager.get().display(new WherigoPushDialogProvider(strings, media, s, s1, luaClosure));
    }

    @Override
    public void pushInput(final EventTable input) {
        /** Request an input from the user.
         * <p>
         * If another dialog or input is open, it should be closed
         * before displaying this input.
         * <p>
         * The <code>input</code> table must contain a "Type" field,
         * which can be either "Text" (then the UI should offer an one-line text input),
         * or "MultipleChoice". In that case, "Choices" field holds
         * another Lua table with list of strings representing the individual choices.
         * UI can then offer either a button for each choice, or some other
         * method of choosing one answer (such as combo box, radio buttons).
         * <p>
         * "Text" field holds a text of this query - this should be displayed above the
         * input field or the choices. "Media" field holds the associated <code>Media</code>.
         * <p>
         * This EventTable has an event "OnGetInput". When the input is processed, this
         * event should be called with a string parameter - either text of the selected choice,
         * or text from the input line. If the input is closed by another API call, the event
         * should be called with null parameter.
         * @param input Lua table describing the input parameters
         */
        WherigoDialogManager.get().display(new WherigoInputDialogProvider(input));
    }

    @Override
    public void showScreen(final int i, final EventTable eventTable) {
        //NEEDED???
        Log.e("WHERIGO showScreen called?");
    }

    @Override
    public void playSound(final byte[] bytes, final String s) {
        //TODO: later
    }

    @Override
    public void blockForSaving() {
        //not needed
    }

    @Override
    public void unblock() {
        WherigoSaveFileHandler.get().loadSaveFinished(); // Ends a running SAVE
    }

    @Override
    public void command(final String cmd) {
        //From WhereYouGo
        if ("StopSound".equals(cmd)) {
         //   UtilsAudio.stopSound();
        } else if ("Alert".equals(cmd)) {
         //   UtilsAudio.playBeep(1);
        }
    }



}
