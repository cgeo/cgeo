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
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AudioManager;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.ListenerHelper;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.Version;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.openwig.Cartridge;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.EventTable;
import cgeo.geocaching.wherigo.openwig.Media;
import cgeo.geocaching.wherigo.openwig.Player;
import cgeo.geocaching.wherigo.openwig.Task;
import cgeo.geocaching.wherigo.openwig.Thing;
import cgeo.geocaching.wherigo.openwig.WherigoLib;
import cgeo.geocaching.wherigo.openwig.Zone;
import cgeo.geocaching.wherigo.openwig.ZonePoint;
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile;
import cgeo.geocaching.wherigo.openwig.platform.UI;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public class WherigoGame implements UI {

    private static final String LOG_PRAEFIX = "WHERIGOGAME: ";

    public static final GeopointConverter<ZonePoint> GP_CONVERTER = new GeopointConverter<>(
        gc -> new ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    public enum NotifyType {
        REFRESH, START, END, LOCATION, DIALOG_OPEN, DIALOG_CLOSE
    }

    private final AudioManager audioManager = new AudioManager();

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
    private boolean lastErrorNotSeen = false;
    private String contextGeocode;
    private String contextGeocacheName;

    //filled on game start
    private boolean isPlaying = false;
    private Cartridge cartridge;

    private final ListenerHelper<Consumer<NotifyType>> listeners = new ListenerHelper<>();

    private static final WherigoGame INSTANCE = new WherigoGame();

    public static WherigoGame get() {
        return INSTANCE;
    }

    //singleton
    @SuppressWarnings("unchecked")
    private WherigoGame() {
        setCGuidAndDependentThings(null);

        //core Wherigo settings: set DeviceID and platform for OpenWig
        try {
            final String name = String.format("c:geo %s", Version.getVersionName(CgeoApplication.getInstance()));
            final String platform = String.format("Android %s", android.os.Build.VERSION.RELEASE + "/" + Build.DISPLAY);
            WherigoLib.env.put(WherigoLib.DEVICE_ID, name);
            WherigoLib.env.put(WherigoLib.PLATFORM, platform);
        } catch (Exception e) {
            // not really important
            Log.d(LOG_PRAEFIX + "unable to set name/platform for OpenWIG", e);
        }
    }

    public int addListener(final Consumer<NotifyType> listener) {
        return listeners.addListener(listener);
    }

    public void removeListener(final int listenerId) {
        listeners.removeListener(listenerId);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean dialogIsPaused() {
        return WherigoDialogManager.get().getState() == WherigoDialogManager.State.DIALOG_PAUSED;
    }

    public void unpauseDialog() {
        WherigoDialogManager.get().unpause();
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
            this.lastErrorNotSeen = false;

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

    public void clearLastError() {
        lastError = null;
        notifyListeners(NotifyType.REFRESH);
    }

    public boolean isLastErrorNotSeen() {
        return lastErrorNotSeen;
    }

    public void clearLastErrorNotSeen() {
        lastErrorNotSeen = false;
        notifyListeners(NotifyType.REFRESH);
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
        Log.d(LOG_PRAEFIX + "notify for " + type);
        listeners.executeOnMain(ntConsumer -> ntConsumer.accept(type));
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
        isPlaying = false;
        freeResources();
        notifyListeners(NotifyType.END);
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
        WherigoDialogManager.get().clear();
        audioManager.release();
    }

    @Override
    public void showError(final String errorMessage) {
        Log.w(LOG_PRAEFIX + "ERROR: " + errorMessage);

        if (errorMessage != null) {
            this.lastError = errorMessage +
                " (Cartridge: " + getCartridgeName() + ", cguid: " + getCGuid() +
                ", timestamp: " + Formatter.formatDateTime(System.currentTimeMillis()) + ")";
            this.lastErrorNotSeen = true;
        }
        ViewUtils.showToast(null, R.string.wherigo_error_toast);
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

    @NonNull
    public AudioManager getAudioManager() {
        return audioManager;
    }

    @Override
    public void playSound(final byte[] data, final String mime) {

        Log.iForce(LOG_PRAEFIX + "play sound (type = " + mime + ", length=" + (data == null ? "null" : data.length) + ")");
        if (data == null || data.length == 0) {
            return;
        }
        final String suffix = mime == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        final Uri clipUri = Uri.fromFile(FileUtils.getOrCreate(this.cartridgeCacheDir, "audio+" + mime, suffix, data));
        //AudioClip.play(clipUri);
        audioManager.play(clipUri);
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
        if ("StopSound".equals(cmd)) {
            this.audioManager.pause();
        } else if ("Alert".equals(cmd)) {
            ActivityMixin.showApplicationToast("Wherigo-Alert");
        }
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
