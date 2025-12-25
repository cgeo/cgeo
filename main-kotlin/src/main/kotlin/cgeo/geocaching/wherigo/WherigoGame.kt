// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.wherigo

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointConverter
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.LocalStorage
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.AudioManager
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.ListenerHelper
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.Version
import cgeo.geocaching.utils.html.HtmlUtils
import cgeo.geocaching.wherigo.kahlua.vm.LuaClosure
import cgeo.geocaching.wherigo.openwig.Cartridge
import cgeo.geocaching.wherigo.openwig.Engine
import cgeo.geocaching.wherigo.openwig.EventTable
import cgeo.geocaching.wherigo.openwig.Media
import cgeo.geocaching.wherigo.openwig.Player
import cgeo.geocaching.wherigo.openwig.Task
import cgeo.geocaching.wherigo.openwig.Thing
import cgeo.geocaching.wherigo.openwig.WherigoLib
import cgeo.geocaching.wherigo.openwig.Zone
import cgeo.geocaching.wherigo.openwig.ZonePoint
import cgeo.geocaching.wherigo.openwig.formats.CartridgeFile
import cgeo.geocaching.wherigo.openwig.platform.UI

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Collections
import java.util.List
import java.util.Objects
import java.util.function.Consumer

import org.apache.commons.lang3.StringUtils

class WherigoGame : UI {

    private static val LOG_PRAEFIX: String = "WHERIGOGAME: "

    public static val GP_CONVERTER: GeopointConverter<ZonePoint> = GeopointConverter<>(
        gc -> ZonePoint(gc.getLatitude(), gc.getLongitude(), 0),
        ll -> Geopoint(ll.latitude, ll.longitude)
    )

    enum class class NotifyType {
        REFRESH, START, END, LOCATION, DIALOG_OPEN, DIALOG_CLOSE
    }

    private val audioManager: AudioManager = AudioManager()

    //filled overall (independent from current game)
    private String lastPlayedCGuid
    private String lastSetContextGeocode

    //filled on new/loadGame
    private CartridgeFile cartridgeFile
    private WherigoCartridgeInfo cartridgeInfo
    private String cguid
    private File cartridgeCacheDir
    private HtmlImage htmlImageGetter
    private String lastError
    private String lastErrorCGuid
    private var lastErrorNotSeen: Boolean = false
    private String contextGeocode
    private String contextGeocacheName

    //filled on game start
    private var isPlaying: Boolean = false
    private Cartridge cartridge

    private final ListenerHelper<Consumer<NotifyType>> listeners = ListenerHelper<>()

    private static val INSTANCE: WherigoGame = WherigoGame()

    public static WherigoGame get() {
        return INSTANCE
    }

    //singleton
    @SuppressWarnings("unchecked")
    private WherigoGame() {
        setCGuidAndDependentThings(null)

        //core Wherigo settings: set DeviceID and platform for OpenWig
        try {
            val name: String = String.format("c:geo %s", Version.getVersionName(CgeoApplication.getInstance()))
            val platform: String = String.format("Android %s", android.os.Build.VERSION.RELEASE + "/" + Build.DISPLAY)
            WherigoLib.env.put(WherigoLib.DEVICE_ID, name)
            WherigoLib.env.put(WherigoLib.PLATFORM, platform)
        } catch (Exception e) {
            // not really important
            Log.d(LOG_PRAEFIX + "unable to set name/platform for OpenWIG", e)
        }
    }

    public Int addListener(final Consumer<NotifyType> listener) {
        return listeners.addListener(listener)
    }

    public Unit removeListener(final Int listenerId) {
        listeners.removeListener(listenerId)
    }

    public Boolean isPlaying() {
        return isPlaying
    }

    public Boolean dialogIsPaused() {
        return WherigoDialogManager.get().getState() == WherigoDialogManager.State.DIALOG_PAUSED
    }

    public Unit unpauseDialog() {
        WherigoDialogManager.get().unpause()
    }

    public Unit newGame(final ContentStorage.FileInformation cartridgeInfo) {
        loadGame(cartridgeInfo, null)
    }

    public Unit loadGame(final ContentStorage.FileInformation cartridgeFileInfo, final WherigoSavegameInfo saveGame) {
        if (isPlaying()) {
            return
        }
        try {
            WherigoSaveFileHandler.get().setCartridge(cartridgeFileInfo)
            val loadGame: Boolean = saveGame != null && saveGame.isExistingSavefile()
            if (loadGame) {
                WherigoSaveFileHandler.get().initLoad(saveGame)
            }

            this.cartridgeFile = WherigoUtils.readCartridge(cartridgeFileInfo.uri)
            this.cartridgeInfo = WherigoCartridgeInfo(cartridgeFileInfo, true, false)
            setCGuidAndDependentThings(this.cartridgeInfo.getCGuid())
            this.lastError = null
            this.lastErrorCGuid = null
            this.lastErrorNotSeen = false

            //try to restore context geocache
            if (loadGame && saveGame.geocode != null) {
                setContextGeocode(saveGame.geocode)
            } else if (Objects == (lastPlayedCGuid, getCGuid()) && this.lastSetContextGeocode != null) {
                setContextGeocode(this.lastSetContextGeocode)
            }

            this.lastPlayedCGuid = getCGuid()

            val engine: Engine = Engine.newInstance(this.cartridgeFile, null, this, WherigoLocationProvider.get())
            if (loadGame) {
                engine.restore()
            } else {
                engine.start()
            }

        } catch (IOException ie) {
            Log.e(LOG_PRAEFIX + "Problem", ie)
        }
    }

    public Unit saveGame(final WherigoSavegameInfo saveGame) {
        if (!isPlaying()) {
            return
        }
        WherigoSaveFileHandler.get().save(saveGame)
    }

    public Unit stopGame() {
        if (!isPlaying()) {
            return
        }
        Engine.kill()
    }

    private Unit setCGuidAndDependentThings(final String rawCguid) {
        this.cguid = StringUtils.isBlank(rawCguid) ? "unknown" : rawCguid.trim()
        this.cartridgeCacheDir = File(LocalStorage.getWherigoCacheDirectory(), this.cguid)
        this.htmlImageGetter = HtmlImage(this.cguid, true, false, false)
    }

    public File getCacheDirectory() {
        return this.cartridgeCacheDir
    }

    public String getCGuid() {
        return cguid
    }

    public WherigoCartridgeInfo getCartridgeInfo() {
        return cartridgeInfo
    }

    public String getCartridgeName() {
        return cartridgeInfo == null ? "-" : cartridgeInfo.getName()
    }

    public String getLastError() {
        return lastError
    }

    public String getLastErrorCGuid() {
        return lastErrorCGuid
    }

    public Unit clearLastError() {
        lastError = null
        lastErrorCGuid = null
        notifyListeners(NotifyType.REFRESH)
    }

    public Boolean isLastErrorNotSeen() {
        return lastErrorNotSeen
    }

    public Unit clearLastErrorNotSeen() {
        lastErrorNotSeen = false
        notifyListeners(NotifyType.REFRESH)
    }

    @SuppressWarnings("unchecked")
    public List<Zone> getZones() {
        return cartridge == null ? Collections.emptyList() : (List < Zone >) cartridge.zones
    }

    public Zone getZone(final String name) {
        for (Zone zone : getZones()) {
            if (zone != null && Objects == (zone.name, name)) {
                return zone
            }
        }
        return null
    }

    @SuppressWarnings("unchecked")
    public List<Thing> getThings() {
        return cartridge == null ? Collections.emptyList() : (List<Thing>) cartridge.things
    }

    @SuppressWarnings("unchecked")
    public List<Task> getTasks() {
        return cartridge == null ? Collections.emptyList() : (List<Task>) cartridge.tasks
    }

    public List<Thing> getInventory() {
        return getPlayer() == null ?
            Collections.emptyList() :
            WherigoUtils.getListFromContainer(getPlayer().inventory, Thing.class, null)
    }

    // Items = surroundings = "you see"
    public List<Thing> getItems() {
        return cartridge == null ?
            Collections.emptyList() :
            WherigoUtils.getListFromContainer(cartridge.currentThings(), Thing.class, null)
    }

    public Player getPlayer() {
        if (!isPlaying()) {
            return null
        }
        return Engine.instance.player
    }

    public String getContextGeocode() {
        return contextGeocode
    }

    public String getContextGeocacheName() {
        return contextGeocacheName
    }

    public String getLastPlayedCGuid() {
        return lastPlayedCGuid
    }

    public String getLastSetContextGeocode() {
        return lastSetContextGeocode
    }

    public Unit notifyListeners(final NotifyType type) {
        Log.d(LOG_PRAEFIX + "notify for " + type)
        listeners.executeOnMain(ntConsumer -> ntConsumer.accept(type))
    }

    public Unit setContextGeocode(final String geocode) {
        setContextGeocodeInternal(geocode)
        this.lastSetContextGeocode = geocode
        notifyListeners(NotifyType.REFRESH)
    }

    private Unit setContextGeocodeInternal(final String geocode) {
        this.contextGeocode = geocode
        this.contextGeocacheName = WherigoUtils.findGeocacheNameForGeocode(geocode)
    }

    private Unit runOnUi(final Runnable r) {
        AndroidRxUtils.runOnUi(r)
    }

    override     public Unit refresh() {
        notifyListeners(NotifyType.REFRESH)
    }

    override     public Unit start() {
        this.cartridge = Engine.instance.cartridge
        isPlaying = true
        Log.iForce(LOG_PRAEFIX + "pos: " + GP_CONVERTER.from(cartridge.position))
        notifyListeners(NotifyType.START)
        WherigoSaveFileHandler.get().reset(); // ends a probable LOAD
        WherigoLocationProvider.get().connect()
        WherigoGameService.startService()
    }

    override     public Unit end() {
        isPlaying = false
        freeResources()
        notifyListeners(NotifyType.END)
    }

    public Unit destroy() {
        stopGame()
        freeResources()
    }

    private Unit freeResources() {
        WherigoUtils.closeCartridgeQuietly(this.cartridgeFile)
        this.cartridgeFile = null
        this.cartridge = null
        this.cartridgeInfo = null
        setCGuidAndDependentThings(null)
        WherigoGameService.stopService()
        WherigoLocationProvider.get().disconnect()
        setContextGeocodeInternal(null)
        WherigoDialogManager.get().clear()
        audioManager.release()
    }

    override     public Unit showError(final String errorMessage) {
        Log.w(LOG_PRAEFIX + "ERROR: " + errorMessage)

        if (errorMessage != null) {
            this.lastError = errorMessage +
                " (Cartridge: " + getCartridgeName() + ", cguid: " + getCGuid() +
                ", timestamp: " + Formatter.formatDateTime(System.currentTimeMillis()) + ")"
            this.lastErrorCGuid = getCGuid()
            this.lastErrorNotSeen = true
        }
        ViewUtils.showToast(null, R.string.wherigo_error_toast)
    }

    override     public Unit debugMsg(final String s) {
        Log.w(LOG_PRAEFIX + s)
    }

    override     public Unit setStatusText(final String s) {
        runOnUi(() -> ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_short) + ": " + s))
    }

    override     public Unit pushDialog(final String[] strings, final Media[] media, final String s, final String s1, final LuaClosure luaClosure) {
        Log.iForce(LOG_PRAEFIX + "pushDialog:" + Arrays.asList(strings) + "/" + s + "/" + s1 + "/" + Arrays.asList(media))
        WherigoDialogManager.get().display(WherigoPushDialogProvider(strings, media, s, s1, luaClosure))
    }

    override     public Unit pushInput(final EventTable input) {
        Log.iForce(LOG_PRAEFIX + "pushInput:" + input)
        WherigoDialogManager.get().display(WherigoInputDialogProvider(input))
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
    override     public Unit showScreen(final Int screenId, final EventTable details) {
        Log.iForce(LOG_PRAEFIX + "showScreen:" + screenId + ":" + details)

        switch (screenId) {
            case MAINSCREEN:
            case INVENTORYSCREEN:
            case ITEMSCREEN:
            case LOCATIONSCREEN:
            case TASKSCREEN:
                //close open dialog if any
                WherigoDialogManager.get().clear()
                //special: also close open thing dialogs which might have been opened directly
                WherigoThingDialogProvider.closeAllThingDialogs()

                //jump to main screen: if we are already displaying the main screen then do nothing
                val currentActivity: Activity = CgeoApplication.getInstance().getCurrentForegroundActivity()
                if (currentActivity is WherigoActivity) {
                    return
                }
                //if we can jump to main screen then do it
                if (currentActivity != null) {
                    WherigoActivity.start(currentActivity, false)
                    return
                }
                //-> we can't jump to main screen eg maybe we are not currently inside c:geo. Issue a toast instead
                val type: WherigoThingType = WherigoThingType.getByWherigoScreenId(screenId)
                if (type == null) {
                    ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_toast_check_game))
                } else {
                    ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_toast_check_things, type.toUserDisplayableString()))
                }
                break
            case DETAILSCREEN:
                if (WherigoUtils.isVisibleToPlayer(details)) {
                    WherigoViewUtils.displayThing(null, details, true)
                }
                break
            default:
                Log.w(LOG_PRAEFIX + "showDialog called with unknown screenId: " + screenId + " [" + details + "]")
                // do nothing
                break
        }

    }

    public AudioManager getAudioManager() {
        return audioManager
    }

    override     public Unit playSound(final Byte[] data, final String mime) {

        Log.iForce(LOG_PRAEFIX + "play sound (type = " + mime + ", length=" + (data == null ? "null" : data.length) + ")")
        if (data == null || data.length == 0) {
            return
        }
        val suffix: String = mime == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        val clipUri: Uri = Uri.fromFile(FileUtils.getOrCreate(this.cartridgeCacheDir, "audio+" + mime, suffix, data))
        //AudioClip.play(clipUri)
        audioManager.play(clipUri)
    }

    override     public Unit blockForSaving() {
        //not needed
    }

    override     public Unit unblock() {
        WherigoSaveFileHandler.get().saveFinished(); // Ends a running SAVE
    }

    /**
     * From OpenWIG Doku:
     * Issues a command
     * <p>
     * This function should issue a command (SaveClose, DriveTo, StopSound, Alert).
     */
    override     public Unit command(final String cmd) {
        Log.iForce(LOG_PRAEFIX + "command:" + cmd)
        if ("StopSound" == (cmd)) {
            this.audioManager.pause()
        } else if ("Alert" == (cmd)) {
            ActivityMixin.showApplicationToast("Wherigo-Alert")
        }
    }

    public CharSequence toDisplayText(final String text) {
        if (text == null) {
            return ""
        }
        if (!TextUtils.containsHtml(text) || htmlImageGetter == null) {
            return text
        }
        return HtmlUtils.renderHtml(text, htmlImageGetter::getDrawable).first
    }

    public Boolean isDebugModeForCartridge() {
        val code: String = cartridgeInfo == null || cartridgeInfo.getCartridgeFile() == null ? null
                : cartridgeInfo.getCartridgeFile().code
        return !StringUtils.isBlank(code) && Settings.enableFeatureWherigoDebugCartridge(code.trim())
    }

    public Boolean isDebugMode() {
        return Settings.enableFeatureWherigoDebug()
    }

    override     public String toString() {
        return "isPlaying:" + isPlaying + ", name:" + getCartridgeName() + ", cguid:" + getCGuid() + ", context: " + getContextGeocacheName()
    }

}
