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

package menion.android.whereyougo.utils;

import java.io.File;
import java.io.FileOutputStream;

public class FileSystemDataWritter extends Thread {

    private static final String TAG = "FileSystemDataWritter";

    private final String fileToWrite;
    private final byte[] dataToWrite;
    private final long bytePos;

    /**
     * Save bytes to file.
     *
     * @param fileToWrite
     * @param dataToWrite
     * @param bytePos     position at write data or <br>
     *                    -1 if write as whole new file<br>
     *                    -2 if write at the end of file
     */
    public FileSystemDataWritter(String fileToWrite, byte[] dataToWrite, long bytePos) {
        this.fileToWrite = fileToWrite;
        this.dataToWrite = dataToWrite;
        this.bytePos = bytePos;
        this.start();
    }

    public void run() {
        try {
            FileSystem.checkFolders(fileToWrite);

            // create if not exist
            // Log.w(TAG, "write to file: " + fileToWrite);
            File file = new File(fileToWrite);
            if (!file.exists()) {
                file.createNewFile();
            }

            if (dataToWrite != null) {
                FileOutputStream os;
                if (bytePos == -1) {
                    os = new FileOutputStream(file, false);
                } else if (bytePos == -2) {
                    os = new FileOutputStream(file, true);
                } else {
                    os = new FileOutputStream(file, true);
                    os.getChannel().position(bytePos);
                }

                os.write(dataToWrite);
                os.close();
            }

        } catch (Exception e) {
            Logger.e(TAG, "run(" + fileToWrite + ")", e);
        }
    }
}
