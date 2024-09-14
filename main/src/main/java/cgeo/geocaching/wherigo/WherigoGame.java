package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AudioClip;
import cgeo.geocaching.utils.Log;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    private static final String LOG_PRAEFIX = "WHERIGOGAME: ";

    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    public enum NotifyType {
        REFRESH, START, END, LOCATION
    }

    private CartridgeFile cartridgeFile;

    private boolean isPlaying = false;
    private Cartridge cartridge;

    private static final AtomicInteger LISTENER_ID_PROVIDER = new AtomicInteger(0);
    private final Map<Integer, Consumer<NotifyType>> listeners = new HashMap<>();


    private static final WherigoGame INSTANCE = new WherigoGame();

    public static WherigoGame get() {
        return INSTANCE;
    }

    private WherigoGame() {
        //singleton
    }

    public boolean openOnlyInWherigo() {
        return false; //TODO: replace with a setting later
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

            final Engine engine = Engine.newInstance(this.cartridgeFile, null, this, WherigoLocationProvider.get());
            if (saveGame != null) {
                engine.restore();
            } else {
                WherigoDialogManager.get().display(new WherigoCartridgeDialogProvider(this.cartridgeFile, engine::start));
            }
        } catch (IOException ie) {
            Log.e(LOG_PRAEFIX + "Problem", ie);
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

    public List<Thing> getInventory() {
        return getPlayer() == null ?
            Collections.emptyList() :
            WherigoUtils.getListFromContainer(getPlayer().inventory, Thing.class, null);
    }

    // Items = surroundings = "you see"
    public List<Thing> getItems() {
        return cartridge == null ?
            Collections.emptyList() :
            WherigoUtils.getListFromContainer(cartridge.currentThings(), Thing.class, null);
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
            Log.d(LOG_PRAEFIX + "notify for " + type);
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
        Log.iForce(LOG_PRAEFIX + "pos: " + GP_CONVERTER.from(cartridge.position));
        notifyListeners(NotifyType.START);
        WherigoSaveFileHandler.get().loadSaveFinished(); // ends a probable LOAD
        WherigoLocationProvider.get().connect();
        WherigoGameService.startService();
    }

    @Override
    public void end() {
        notifyListeners(NotifyType.END);
        isPlaying = false;
        freeResources();
    }

    public void destroy() {
        stopGame();
        freeResources();
    }

    private void freeResources() {
        WherigoUtils.closeCartridgeQuietly(this.cartridgeFile);
        this.cartridgeFile = null;
        this.cartridge = null;
        WherigoGameService.stopService();
        WherigoLocationProvider.get().disconnect();
    }

    @Override
    public void showError(final String s) {
        Log.w(LOG_PRAEFIX + "ERROR" + s);
        setStatusText("ERROR:" + s);
    }

    @Override
    public void debugMsg(final String s) {
        Log.w(LOG_PRAEFIX + s);
    }

    @Override
    public void setStatusText(final String s) {
        runOnUi(() -> ActivityMixin.showApplicationToast("WHERIGO:" + s));
    }

    @Override
    public void pushDialog(final String[] strings, final Media[] media, final String s, final String s1, final LuaClosure luaClosure) {
        Log.iForce(LOG_PRAEFIX + "pushDialog:" + Arrays.asList(strings));
        WherigoDialogManager.get().display(new WherigoPushDialogProvider(strings, media, s, s1, luaClosure));
    }

    @Override
    public void pushInput(final EventTable input) {
        Log.iForce(LOG_PRAEFIX + "pushInput:" + input);
        WherigoDialogManager.get().display(new WherigoInputDialogProvider(input));
    }

    @Override
    public void showScreen(final int screenId, final EventTable eventTable) {
        Log.iForce(LOG_PRAEFIX + "showScreen:" + screenId + ":" + eventTable);

        switch (screenId) {
            case MAINSCREEN:
            case INVENTORYSCREEN:
            case ITEMSCREEN:
            case LOCATIONSCREEN:
            case TASKSCREEN:
                final Activity currentActivity = CgeoApplication.getInstance().getCurrentForegroundActivity();
                if (currentActivity instanceof WherigoActivity) {
                    return;
                }
                if (currentActivity != null && !openOnlyInWherigo()) {
                    WherigoActivity.start(currentActivity, false, screenId);
                }
                break;
            case DETAILSCREEN:
                WherigoDialogManager.get().display(new WherigoThingDialogProvider(eventTable));
                break;
            default:
                Log.w(LOG_PRAEFIX + "showDialog called with unknown screenId: " + screenId + " [" + eventTable + "]");
                // do nothing
                break;
        }

    }

    @Override
    public void playSound(final byte[] data, final String s) {

        Log.iForce(LOG_PRAEFIX + "play sound (type = " + s + ", length=" + data.length + ")");
        AudioClip.play(data);
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
        Log.iForce(LOG_PRAEFIX + "command:" + cmd);
        //From WhereYouGo
        //if ("StopSound".equals(cmd)) {
         //   UtilsAudio.stopSound();
        //} else if ("Alert".equals(cmd)) {
         //   UtilsAudio.playBeep(1);
        //}
    }



}
