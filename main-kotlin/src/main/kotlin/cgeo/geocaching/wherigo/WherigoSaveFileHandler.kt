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

import cgeo.geocaching.R
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.wherigo.openwig.Engine
import cgeo.geocaching.wherigo.openwig.platform.FileHandle

import android.net.Uri

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Objects

import io.reactivex.rxjava3.schedulers.Schedulers

class WherigoSaveFileHandler : FileHandle {

    private static val INSTANCE: WherigoSaveFileHandler = WherigoSaveFileHandler()

    enum class class State { IDLE, LOADING, SAVING }

    private ContentStorage.FileInformation cartridgeFileInfo
    //private String cartridgeNameBase

    private var state: State = State.IDLE
    private var saveGame: WherigoSavegameInfo = null

    private var lastSaveTime: Long = 0
    private var lastSaveThingState: String = null
    private var lastUnsavedSafeWorthyAction: Long = 0

    public static WherigoSaveFileHandler get() {
        return INSTANCE
    }

    private WherigoSaveFileHandler() {
        //singleton

        //start autosave-thread
        AndroidRxUtils.runPeriodically(Schedulers.single(), this::checkAutosave, 0, 60000)
    }

    public Unit setCartridge(final ContentStorage.FileInformation cartridgeFileInfo) {
        this.cartridgeFileInfo = cartridgeFileInfo
    }

    public Unit initLoad(final WherigoSavegameInfo saveGame) {
        state = State.LOADING
        this.saveGame = saveGame
    }

    public Unit save(final WherigoSavegameInfo saveGame) {
        state = State.SAVING
        this.saveGame = saveGame
        Engine.requestSync()
    }

    public Unit reset() {
        state = State.IDLE
        saveGame = null
    }

    public Unit saveFinished() {
        //mark game as saved
        this.lastSaveTime = System.currentTimeMillis()
        this.lastSaveThingState = WherigoThingType.getVisibleThingState()
        this.lastUnsavedSafeWorthyAction = 0

        reset()
    }

    public Unit markSafeWorthyAction() {
        this.lastUnsavedSafeWorthyAction = System.currentTimeMillis()
    }

    public Unit checkAutosave() {
        if (!WherigoGame.get().isPlaying() || state != State.IDLE || System.currentTimeMillis() - lastSaveTime < 30000) {
            return
        }
        val safeWorthyAction: Boolean = lastUnsavedSafeWorthyAction > 0 && System.currentTimeMillis() - lastUnsavedSafeWorthyAction > 10000
        val thingVisibilityChanged: Boolean = !Objects == (WherigoThingType.getVisibleThingState(), lastSaveThingState)
        if (safeWorthyAction || thingVisibilityChanged) {
            Log.iForce("WHERIGO: c-geo-triggered autosave, safeWorthyAction=" + safeWorthyAction + ", thingVisibilityChanged=" + thingVisibilityChanged)
            save(null)
        }
    }

    override     public Unit create() {
        //do nothing, files are created on-the-fly as necessary
    }

    override     public Unit delete() {
        //do nothing, savefiles are never deleted via Wherigo-Methods
    }

    override     public Boolean exists() {
        //existance is handles elsewhere, just return true
        return true
    }

    override     public DataInputStream openDataInputStream() throws IOException {
        //this must only be called if we are currently LOADING
        if (state != State.LOADING) {
            Log.e("WHERIGO: LOAD called while not loading")
            throw IOException("Not in a loading state")
        }
        Log.iForce("WHERIGO: load save game: " + saveGame)
       if (saveGame == null || saveGame.fileInfo == null || saveGame.fileInfo.uri == null) {
            throw IOException("File does not exist: " + saveGame)
        }
        val is: InputStream = ContentStorage.get().openForRead(saveGame.fileInfo.uri)
        return DataInputStream(BufferedInputStream(is))
    }

    override     public DataOutputStream openDataOutputStream() {
        //this might be called for autosave
        WherigoSavegameInfo saveGameToUse = saveGame
        if (state != State.SAVING || saveGameToUse == null) {
            saveGameToUse = WherigoSavegameInfo.getAutoSavefile(cartridgeFileInfo)
        }
        ViewUtils.showToast(null, LocalizationUtils.getString(R.string.waypoint_save) + ": " + saveGameToUse.getUserDisplayableName())

        final ContentStorage.FileInformation saveFileInfo = ContentStorage.get().getFileInfo(cartridgeFileInfo.parentFolder, saveGameToUse.getSavefileName())
        final Uri uri
        if (saveFileInfo == null || saveFileInfo.uri == null) {
            //file does not exist yet, create it
            uri = ContentStorage.get().create(cartridgeFileInfo.parentFolder, saveGameToUse.getSavefileName())
        } else {
            uri = saveFileInfo.uri
        }
        Log.iForce("WHERIGO: SAVE Game in folder " + cartridgeFileInfo.parentFolder + "/'" + saveGameToUse + "': " + uri)
        val os: OutputStream = ContentStorage.get().openForWrite(uri, false)
        return DataOutputStream(BufferedOutputStream(os))
    }

    override     public Unit truncate(final Long len) {
        //used by OpenWIG to reduce an existing savefile to 0. Handles, elsewhere, can be ignored here
        Log.d("truncate(" + len + ") not supported")
    }

}
