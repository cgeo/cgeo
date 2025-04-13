package cgeo.geocaching.wherigo;

import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.platform.FileHandle;

public class WherigoSaveFileHandler implements FileHandle {

    private static final WherigoSaveFileHandler INSTANCE = new WherigoSaveFileHandler();

    public enum State { IDLE, LOADING, SAVING }

    private ContentStorage.FileInformation cartridgeFileInfo;
    //private String cartridgeNameBase;

    private State state = State.IDLE;
    private WherigoSavegameInfo saveGame = null;

    public static WherigoSaveFileHandler get() {
        return INSTANCE;
    }

    private WherigoSaveFileHandler() {
        //singleton
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

    public void loadSaveFinished() {
        state = State.IDLE;
        saveGame = null;
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
