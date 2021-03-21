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

package menion.android.whereyougo.openwig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cz.matejcik.openwig.platform.FileHandle;
import menion.android.whereyougo.utils.Logger;

public class WSaveFile implements FileHandle {

    private static final String TAG = "WSaveFile";

    private final File file;

    public WSaveFile(File cartridgeFile) {
        file =
                new File(cartridgeFile.getAbsolutePath().substring(0,
                        cartridgeFile.getAbsolutePath().lastIndexOf("."))
                        + ".ows");
    }

    public void create() throws IOException {
        file.createNewFile();
    }

    public void delete() throws IOException {
        file.delete();
    }

    public boolean exists() throws IOException {
        return file.exists();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void truncate(long len) throws IOException {
        Logger.d(TAG, "truncate()");
    }

}
