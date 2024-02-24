/*
 * This file is part of WhereYouGo.
 * 
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package cgeo.geocaching.wherigo;

import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.matejcik.openwig.Engine;
import cz.matejcik.openwig.platform.FileHandle;

public class WherigoSaveFileHandler implements FileHandle {

    private static final WherigoSaveFileHandler INSTANCE = new WherigoSaveFileHandler();

    public enum State { IDLE, LOADING, SAVING }

    private Folder folder;
    private String cartridgeNameBase;

    private State state = State.IDLE;
    private String fileName = null;

    public static WherigoSaveFileHandler get() {
        return INSTANCE;
    }

    private WherigoSaveFileHandler() {
        //singleton
    }

    public static Map<String, Date> getAvailableSaveFiles(final Folder cartridgeFolder, final String cartridgeName) {
        if (cartridgeFolder == null || cartridgeName == null) {
            return Collections.emptyMap();
        }
        final String cartridgeNameBase = getCartridgeNameBase(cartridgeName);
        final List<ContentStorage.FileInformation> files = ContentStorage.get().list(cartridgeFolder);
        return files.stream()
            .filter(fi -> fi.name.startsWith(cartridgeNameBase + "-") && fi.name.endsWith(".sav"))
            .collect(Collectors.toMap(fi -> fi.name.substring(cartridgeNameBase.length() + 1, fi.name.length() - 4), fi -> new Date(fi.lastModified)));
    }

    private String getSavefileName(final String fileName) {
        return cartridgeNameBase + "-" + fileName + ".sav";
    }

    private static String getCartridgeNameBase(final String cartridgeName) {
        final int idx = cartridgeName.lastIndexOf(".");
        if (idx > 0) {
            return cartridgeName.substring(0, idx);
        }
        return cartridgeName;
    }

    public void setCartridge(final Folder cartridgeFolder, final String cartridgeName) {
        this.folder = cartridgeFolder;
        this.cartridgeNameBase = getCartridgeNameBase(cartridgeName);
    }

    public void initLoad(final String loadFile) {
        state = State.LOADING;
        fileName = loadFile;
    }

    public void save(final String saveFile) {
        state = State.SAVING;
        fileName = saveFile;
        Engine.requestSync();
    }

    public void loadSaveFinished() {
        state = State.IDLE;
        fileName = null;
    }

    @Override
    public void create() throws IOException {
        //do nothing, files are created on-the-fly as necessary
    }

    @Override
    public void delete() throws IOException {
        //do nothing, savefiles are never deleted via Wherigo-Methods
    }

    @Override
    public boolean exists() throws IOException {
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
        Log.iForce("WHERIGO: load save game: " + fileName);
        final ContentStorage.FileInformation saveFileInfo = ContentStorage.get().getFileInfo(folder, getSavefileName(fileName));
        if (saveFileInfo == null || saveFileInfo.uri == null) {
            throw new IOException("File does not exist: " + getSavefileName(fileName));
        }
        final InputStream is = ContentStorage.get().openForRead(saveFileInfo.uri);
        return new DataInputStream(new BufferedInputStream(is));
    }

    @Override
    public DataOutputStream openDataOutputStream() {
        //this might be called for autosave
        String saveFileToUse = fileName;
        if (state != State.SAVING || saveFileToUse == null) {
            saveFileToUse = "autosave";
        }
        final String saveFileName = getSavefileName(saveFileToUse);
        Uri uri = null;
        final ContentStorage.FileInformation saveFileInfo = ContentStorage.get().getFileInfo(folder, saveFileName);
        if (saveFileInfo == null || saveFileInfo.uri == null) {
            //file does not exist yet, create it
            uri = ContentStorage.get().create(folder, saveFileName);
        } else {
            uri = saveFileInfo.uri;
        }
        Log.iForce("WHERIGO: SAVE Game in folder " + folder + "/'" + saveFileName + "': " + uri);
        final OutputStream os = ContentStorage.get().openForWrite(uri, false);
        return new DataOutputStream(new BufferedOutputStream(os));
    }

    @Override
    public void truncate(final long len) throws IOException {
        //used by OpenWIG to reduce an existing savefile to 0. Handles, elsewhere, can be ignored here
        Log.d("truncate(" + len + ") not supported");
    }

}
