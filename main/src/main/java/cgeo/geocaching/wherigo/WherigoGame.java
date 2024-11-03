package cgeo.geocaching.wherigo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AudioClip;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.html.HtmlUtils;

import android.app.Activity;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.apache.commons.lang3.StringUtils;
import se.krka.kahlua.vm.LuaClosure;

public class WherigoGame implements UI {

    private static final String LOG_PRAEFIX = "WHERIGOGAME: ";

    private static final Set<String> OPENWIG_ENGINE_ERROR_PREFIXES = new HashSet<>(Arrays.asList(
        "You hit a bug! Please report this to whereyougo@cgeo.org with a screenshot of this message.".toLowerCase(Locale.ROOT),
        "You hit a bug! please report at openwig.googlecode.com and i'll fix it for you!".toLowerCase(Locale.ROOT)
    ));

    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    public enum NotifyType {
        REFRESH, START, END, LOCATION, DIALOG_OPEN, DIALOG_CLOSE
    }

    //filled overall (independent from current game)
    private String lastPlayedCGuid;
    private String lastSetContextGeocode;

    //filled on new/loadGame
    private CartridgeFile cartridgeFile;
    private WherigoCartridgeInfo cartridgeInfo;
    private String cguid;
    private File cartridgeCacheDir;
    private HtmlImage htmlImageGetter;
    private String lastError;
    private String contextGeocode;
    private String contextGeocacheName;

    //filled on game start
    private boolean isPlaying = false;
    private Cartridge cartridge;

    private static final AtomicInteger LISTENER_ID_PROVIDER = new AtomicInteger(0);
    private final Map<Integer, Consumer<NotifyType>> listeners = new HashMap<>();


    private static final WherigoGame INSTANCE = new WherigoGame();

    public static WherigoGame get() {
        return INSTANCE;
    }

    //singleton
    private WherigoGame() {
        setCGuidAndDependentThings(null);
    }

    public boolean openOnlyInWherigo() {
        return false; //TODO: replace with a setting later
    }

    public int addListener(final Consumer<NotifyType> listener) {
        final int listenerId = LISTENER_ID_PROVIDER.addAndGet(1);
        synchronized (listeners) {
            this.listeners.put(listenerId, listener);
        }
        return listenerId;
    }

    public void removeListener(final int listenerId) {
        synchronized (listeners) {
            this.listeners.remove(listenerId);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void newGame(@NonNull final ContentStorage.FileInformation cartridgeInfo) {
        loadGame(cartridgeInfo, null);
    }

    public void loadGame(@NonNull final ContentStorage.FileInformation cartridgeFileInfo, @Nullable final WherigoSavegameInfo saveGame) {
        if (isPlaying()) {
            return;
        }
        try {
            WherigoSaveFileHandler.get().setCartridge(cartridgeFileInfo);
            final boolean loadGame = saveGame != null && saveGame.isExistingSavefile();
            if (loadGame) {
                WherigoSaveFileHandler.get().initLoad(saveGame);
            }

            this.cartridgeFile = WherigoUtils.readCartridge(cartridgeFileInfo.uri);
            this.cartridgeInfo = new WherigoCartridgeInfo(cartridgeFileInfo, true, false);
            setCGuidAndDependentThings(this.cartridgeInfo.getCGuid());
            this.lastError = null;

            //try to restore context geocache
            if (loadGame && saveGame.geocode != null) {
                setContextGeocode(saveGame.geocode);
            } else if (Objects.equals(lastPlayedCGuid, getCGuid()) && this.lastSetContextGeocode != null) {
                setContextGeocode(this.lastSetContextGeocode);
            }

            this.lastPlayedCGuid = getCGuid();

            final Engine engine = Engine.newInstance(this.cartridgeFile, null, this, WherigoLocationProvider.get());
            if (loadGame) {
                engine.restore();
            } else {
                engine.start();
            }
        } catch (IOException ie) {
            Log.e(LOG_PRAEFIX + "Problem", ie);
        }
    }

    public void saveGame(final WherigoSavegameInfo saveGame) {
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

    private void setCGuidAndDependentThings(@Nullable final String rawCguid) {
        this.cguid = StringUtils.isBlank(rawCguid) ? "unknown" : rawCguid.trim();
        this.cartridgeCacheDir = new File(LocalStorage.getWherigoCacheDirectory(), this.cguid);
        this.htmlImageGetter = new HtmlImage(this.cguid, true, false, false);
    }

    @NonNull
    public File getCacheDirectory() {
        return this.cartridgeCacheDir;
    }

    @NonNull
    public String getCGuid() {
        return cguid;
    }

    @Nullable
    public WherigoCartridgeInfo getCartridgeInfo() {
        return cartridgeInfo;
    }

    @NonNull
    public String getCartridgeName() {
        return cartridgeInfo == null ? "-" : cartridgeInfo.getName();
    }

    @Nullable
    public String getLastError() {
        return lastError;
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

    @Nullable
    public Player getPlayer() {
        if (!isPlaying()) {
            return null;
        }
        return Engine.instance.player;
    }

    @Nullable
    public String getContextGeocode() {
        return contextGeocode;
    }

    @Nullable
    public String getContextGeocacheName() {
        return contextGeocacheName;
    }

    @Nullable
    public String getLastPlayedCGuid() {
        return lastPlayedCGuid;
    }

    @Nullable
    public String getLastSetContextGeocode() {
        return lastSetContextGeocode;
    }

    public void notifyListeners(final NotifyType type) {
        final Collection<Consumer<NotifyType>> listenerCopy;
        synchronized (listeners) {
            listenerCopy = new ArrayList<>(listeners.values());
        }
        runOnUi(() -> {
            Log.d(LOG_PRAEFIX + "notify for " + type);
            for (Consumer<NotifyType> listener : listenerCopy) {
                listener.accept(type);
            }
        });
    }

    public void setContextGeocode(final String geocode) {
        setContextGeocodeInternal(geocode);
        this.lastSetContextGeocode = geocode;
        notifyListeners(NotifyType.REFRESH);
    }

    private void setContextGeocodeInternal(final String geocode) {
        this.contextGeocode = geocode;
        this.contextGeocacheName = WherigoUtils.findGeocacheNameForGeocode(geocode);
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
        this.cartridgeInfo = null;
        setCGuidAndDependentThings(null);
        WherigoGameService.stopService();
        WherigoLocationProvider.get().disconnect();
        setContextGeocodeInternal(null);
    }

    @Override
    public void showError(final String errorMessage) {
        Log.w(LOG_PRAEFIX + "ERROR: " + errorMessage);

        if (errorMessage != null) {
            //Remove common prefixed
            String errorMsg = errorMessage;
            for (String commonPrefix : OPENWIG_ENGINE_ERROR_PREFIXES) {
                if (errorMsg.toLowerCase(Locale.ROOT).startsWith(commonPrefix)) {
                    errorMsg = "OpenWIG error: " + errorMsg.substring(commonPrefix.length());
                    break;
                }
            }
            this.lastError = errorMsg;
        }
        WherigoDialogManager.get().display(new WherigoErrorDialogProvider());
    }

    @Override
    public void debugMsg(final String s) {
        Log.w(LOG_PRAEFIX + s);
    }

    @Override
    public void setStatusText(final String s) {
        runOnUi(() -> ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_short) + ": " + s));
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

    /**
     * From OpenWIG doku:
     * Shows a specified screen
     * <p>
     * The screen specified by screenId should be made visible.
     * If a dialog or an input is open, it must be closed before
     * showing the screen.
     * @param screenId the screen to be shown
     * @param details if screenId is DETAILSCREEN, details of this object will be displayed
     */
    @Override
    public void showScreen(final int screenId, final EventTable details) {
        Log.iForce(LOG_PRAEFIX + "showScreen:" + screenId + ":" + details);

        switch (screenId) {
            case MAINSCREEN:
            case INVENTORYSCREEN:
            case ITEMSCREEN:
            case LOCATIONSCREEN:
            case TASKSCREEN:
                WherigoDialogManager.get().clear();
                final Activity currentActivity = CgeoApplication.getInstance().getCurrentForegroundActivity();
                if (currentActivity instanceof WherigoActivity) {
                    return;
                }
                //don't open the screens here, just issue a toast advising user to check
                final WherigoThingType type = WherigoThingType.getByWherigoScreenId(screenId);
                if (type == null) {
                    ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_toast_check_game));
                } else {
                    ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_toast_check_things, type.toUserDisplayableString()));
                }
                break;
            case DETAILSCREEN:
                WherigoViewUtils.displayThing(null, details, true);
                break;
            default:
                Log.w(LOG_PRAEFIX + "showDialog called with unknown screenId: " + screenId + " [" + details + "]");
                // do nothing
                break;
        }

    }

    @Override
    public void playSound(final byte[] data, final String mime) {

        Log.iForce(LOG_PRAEFIX + "play sound (type = " + mime + ", length=" + (data == null ? "null" : data.length) + ")");
        if (data == null || data.length == 0) {
            return;
        }
        final String suffix = mime == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        final Uri clipUri = Uri.fromFile(FileUtils.getOrCreate(this.cartridgeCacheDir, "audio+" + mime, suffix, data));
        AudioClip.play(clipUri);
    }

    @Override
    public void blockForSaving() {
        //not needed
    }

    @Override
    public void unblock() {
        WherigoSaveFileHandler.get().loadSaveFinished(); // Ends a running SAVE
    }

    /**
     * From OpenWIG Doku:
     * Issues a command
     * <p>
     * This function should issue a command (SaveClose, DriveTo, StopSound, Alert).
     */
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

    @NonNull
    public CharSequence toDisplayText(final String text) {
        if (text == null) {
            return "";
        }
        if (!TextUtils.containsHtml(text)) {
            return text;
        }
        return HtmlUtils.renderHtml(text, htmlImageGetter::getDrawable).first;
    }

    public boolean isDebugModeForCartridge() {
        final String code = cartridgeInfo == null || cartridgeInfo.getCartridgeFile() == null ? null
                : cartridgeInfo.getCartridgeFile().code;
        return !StringUtils.isBlank(code) && Settings.enableFeatureWherigoDebugCartridge(code.trim());
    }

    public boolean isDebugMode() {
        return Settings.enableFeatureWherigoDebug();
    }

    @NonNull
    @Override
    public String toString() {
        return "isPlaying:" + isPlaying + ", name:" + getCartridgeName() + ", cguid:" + getCGuid() + ", context: " + getContextGeocacheName();
    }
}
