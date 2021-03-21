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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import cz.matejcik.openwig.platform.SeekableFile;
import menion.android.whereyougo.utils.Logger;

public class WSeekableFile implements SeekableFile {

    private static final String TAG = "WSeekableFile";
    private RandomAccessFile raf;

    public WSeekableFile(File file) {
        try {
            this.raf = new RandomAccessFile(file, "rw");
        } catch (Exception e) {
            Logger.e(TAG, "WSeekableFile(" + file.getAbsolutePath() + ")", e);
        }
    }

    private static double readDouble(byte[] buffer, int start, int len) {
        long result = 0;
        for (int i = 0; i < len; i++) {
            result |= ((long) (buffer[start + i] & 0xff)) << (i * 8);
        }
        return Double.longBitsToDouble(result);
    }

    private static int readInt(byte[] buffer, int start, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result += (buffer[start + i] & 0xFF) << (i * 8);
        }

        return result;
    }

    private static long readLong(byte[] buffer, int start, int len) {
        long result = 0;
        for (int i = 0; i < len; i++) {
            result |= ((long) (buffer[start + i] & 0xff)) << (i * 8);
        }
        return result;
    }

    public long position() throws IOException {
        // Logger.i(TAG, "position(), res:" + raf.getFilePointer());
        return raf.getFilePointer();
    }

    public int read() throws IOException {
        return raf.read();
    }

    public double readDouble() throws IOException {
        try {
            byte[] data = new byte[8];
            raf.read(data);
            return readDouble(data, 0, 8);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void readFully(byte[] buf) throws IOException {
        raf.read(buf);
    }

    public int readInt() throws IOException {
        try {
            byte[] data = new byte[4];
            raf.read(data);
            return readInt(data, 0, 4);
        } catch (Exception e) {
            return 0;
        }
    }

    public long readLong() throws IOException {
        byte[] buffer = new byte[8];
        raf.read(buffer);
        return readLong(buffer, 0, 8);
    }

    public short readShort() throws IOException {
        byte[] r = new byte[2];
        raf.read(r);
        return (short) ((r[1] << 8) | (r[0] & 0xff));
    }


    public String readString() throws IOException {
        StringBuilder sb = new StringBuilder();
        int b = raf.read();
        while (b > 0) {
            sb.append((char) b);
            b = raf.read();
        }
        return sb.toString();
    }

    public void seek(long pos) throws IOException {
        // Logger.i(TAG, "seek(" + pos + ")");
        raf.seek(pos);
    }

    public long skip(long what) throws IOException {
        // Logger.i(TAG, "skip(" + what + ")");
        return raf.skipBytes((int) what);
    }

}
