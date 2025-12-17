package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.wherigo.openwig.Engine;
import cgeo.geocaching.wherigo.openwig.platform.FileHandle;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class WherigoSaveFileHandler implements FileHandle {

    private static final WherigoSaveFileHandler INSTANCE = new WherigoSaveFileHandler();

    public enum State { IDLE, LOADING, SAVING }

    private ContentStorage.FileInformation cartridgeFileInfo;
    //private String cartridgeNameBase;

    private State state = State.IDLE;
    private WherigoSavegameInfo saveGame = null;

    private long lastSaveTime = 0;
    private String lastSaveThingState = null;
    private long lastUnsavedSafeWorthyAction = 0;

    public static WherigoSaveFileHandler get() {
        return INSTANCE;
    }

    private WherigoSaveFileHandler() {
        //singleton

        //start autosave-thread
        AndroidRxUtils.runPeriodically(Schedulers.single(), this::checkAutosave, 0, 60000);
    }

    public void setCartridge(final ContentStorage.FileInformation cartridgeFileInfo) {
        this.cartridgeFileInfo = cartridgeFileInfo;
    }

    public void initLoad(final WherigoSavegameInfo saveGame) {
        state = State.LOADING;
        this.saveGame = saveGame;
    }

    public void save(final WherigoSavegameInfo saveGame) {
        state = State.SAVING;
        this.saveGame = saveGame;
        Engine.requestSync();
    }

    public void reset() {
        state = State.IDLE;
        saveGame = null;
    }

    public void saveFinished() {
        //mark game as saved
        this.lastSaveTime = System.currentTimeMillis();
        this.lastSaveThingState = WherigoThingType.getVisibleThingState();
        this.lastUnsavedSafeWorthyAction = 0;

        reset();
    }

    public void markSafeWorthyAction() {
        this.lastUnsavedSafeWorthyAction = System.currentTimeMillis();
    }

    public void checkAutosave() {
        if (!WherigoGame.get().isPlaying() || state != State.IDLE || System.currentTimeMillis() - lastSaveTime < 30000) {
            return;
        }
        final boolean safeWorthyAction = lastUnsavedSafeWorthyAction > 0 && System.currentTimeMillis() - lastUnsavedSafeWorthyAction > 10000;
        final boolean thingVisibilityChanged = !Objects.equals(WherigoThingType.getVisibleThingState(), lastSaveThingState);
        if (safeWorthyAction || thingVisibilityChanged) {
            Log.iForce("WHERIGO: c-geo-triggered autosave, safeWorthyAction=" + safeWorthyAction + ", thingVisibilityChanged=" + thingVisibilityChanged);
            save(null);
        }
    }

    @Override
    public void create() {
        //do nothing, files are created on-the-fly as necessary
    }

    @Override
    public void delete() {
        //do nothing, savefiles are never deleted via Wherigo-Methods
    }

    @Override
    public boolean exists() {
        //existance is handles elsewhere, just return true
        return true;
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        //this must only be called if we are currently LOADING
        if (state != State.LOADING) {
            Log.e("WHERIGO: LOAD called while not loading");
            throw new IOException("Not in a loading state");
        }
        Log.iForce("WHERIGO: load save game: " + saveGame);
       if (saveGame == null || saveGame.fileInfo == null || saveGame.fileInfo.uri == null) {
            throw new IOException("File does not exist: " + saveGame);
        }
        final InputStream is = ContentStorage.get().openForRead(saveGame.fileInfo.uri);
        return new DataInputStream(new BufferedInputStream(is));
    }

    @Override
    public DataOutputStream openDataOutputStream() {
        //this might be called for autosave
        WherigoSavegameInfo saveGameToUse = saveGame;
        if (state != State.SAVING || saveGameToUse == null) {
            saveGameToUse = WherigoSavegameInfo.getAutoSavefile(cartridgeFileInfo);
        }
        ViewUtils.showToast(null, LocalizationUtils.getString(R.string.waypoint_save) + ": " + saveGameToUse.getUserDisplayableName());

        final ContentStorage.FileInformation saveFileInfo = ContentStorage.get().getFileInfo(cartridgeFileInfo.parentFolder, saveGameToUse.getSavefileName());
        final Uri uri;
        if (saveFileInfo == null || saveFileInfo.uri == null) {
            //file does not exist yet, create it
            uri = ContentStorage.get().create(cartridgeFileInfo.parentFolder, saveGameToUse.getSavefileName());
        } else {
            uri = saveFileInfo.uri;
        }
        Log.iForce("WHERIGO: SAVE Game in folder " + cartridgeFileInfo.parentFolder + "/'" + saveGameToUse + "': " + uri);
        final OutputStream os = ContentStorage.get().openForWrite(uri, false);
        return new DataOutputStream(new BufferedOutputStream(os));
    }

    @Override
    public void truncate(final long len) {
        //used by OpenWIG to reduce an existing savefile to 0. Handles, elsewhere, can be ignored here
        Log.d("truncate(" + len + ") not supported");
    }

}
