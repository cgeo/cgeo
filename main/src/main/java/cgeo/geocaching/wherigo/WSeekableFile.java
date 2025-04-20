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

import cgeo.geocaching.utils.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import cz.matejcik.openwig.platform.SeekableFile;
import org.oscim.utils.IOUtils;

public class WSeekableFile implements SeekableFile {

    private final FileChannel fileChannel;

    public WSeekableFile(final FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public long readLong() {
        final byte[] buffer = readInternal(8);
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result |= ((long) (buffer[i] & 0xff)) << (i * 8);
        }
        return result;
    }

    public int readInt() {
        final byte[] buffer = readInternal(4);
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result += (buffer[i] & 0xFF) << (i * 8);
        }

        return result;
    }

    private byte[] readInternal(final int length) {
        return readInternal(new byte[length]);
    }

    private byte[] readInternal(final byte[] array) {
        try {
            final ByteBuffer bb = ByteBuffer.wrap(array);
            fileChannel.read(bb);
            return bb.array();
        } catch (IOException ioe) {
            Log.e("Problem reading", ioe);
            return null;
        }
    }

    public long position() throws IOException {
        // Logger.i(TAG, "position(), res:" + raf.getFilePointer());
        return fileChannel.position();
    }

    public int read() {
        final byte[] r = readInternal(1);
        return r[0];
    }

    public void readFully(final byte[] buf) {
        readInternal(buf);
    }

    public short readShort() {
        final byte[] r = readInternal(2);
        return (short) ((r[1] << 8) | (r[0] & 0xff));
    }


    public String readString() {
        final StringBuilder sb = new StringBuilder();
        byte[] b = readInternal(1);
        while (b[0] > 0) {
            sb.append((char) b[0]);
            b = readInternal(1);
        }
        return sb.toString();
    }

    public void seek(final long pos) throws IOException {
        // Logger.i(TAG, "seek(" + pos + ")");
        fileChannel.position(pos);
    }

    public long skip(final long what) throws IOException {
        // Logger.i(TAG, "skip(" + what + ")");
        seek(position() + what);
        return what;
    }

    public void close() {
        IOUtils.closeQuietly(fileChannel);
    }

}
