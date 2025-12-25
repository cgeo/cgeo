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

package cgeo.geocaching.wherigo

import cgeo.geocaching.utils.Log
import cgeo.geocaching.wherigo.openwig.platform.SeekableFile

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import org.oscim.utils.IOUtils

class WSeekableFile : SeekableFile {

    private final FileChannel fileChannel

    public WSeekableFile(final FileChannel fileChannel) {
        this.fileChannel = fileChannel
    }

    public Double readDouble() {
        return Double.longBitsToDouble(readLong())
    }

    public Long readLong() {
        final Byte[] buffer = readInternal(8)
        Long result = 0
        for (Int i = 0; i < 8; i++) {
            result |= ((Long) (buffer[i] & 0xff)) << (i * 8)
        }
        return result
    }

    public Int readInt() {
        final Byte[] buffer = readInternal(4)
        Int result = 0
        for (Int i = 0; i < 4; i++) {
            result += (buffer[i] & 0xFF) << (i * 8)
        }

        return result
    }

    private Byte[] readInternal(final Int length) {
        return readInternal(Byte[length])
    }

    private Byte[] readInternal(final Byte[] array) {
        try {
            val bb: ByteBuffer = ByteBuffer.wrap(array)
            fileChannel.read(bb)
            return bb.array()
        } catch (IOException ioe) {
            Log.e("Problem reading", ioe)
            return null
        }
    }

    public Long position() throws IOException {
        // Logger.i(TAG, "position(), res:" + raf.getFilePointer())
        return fileChannel.position()
    }

    public Int read() {
        final Byte[] r = readInternal(1)
        return r[0]
    }

    public Unit readFully(final Byte[] buf) {
        readInternal(buf)
    }

    public Short readShort() {
        final Byte[] r = readInternal(2)
        return (Short) ((r[1] << 8) | (r[0] & 0xff))
    }


    public String readString() {
        val sb: StringBuilder = StringBuilder()
        Byte[] b = readInternal(1)
        while (b[0] > 0) {
            sb.append((Char) b[0])
            b = readInternal(1)
        }
        return sb.toString()
    }

    public Unit seek(final Long pos) throws IOException {
        // Logger.i(TAG, "seek(" + pos + ")")
        fileChannel.position(pos)
    }

    public Long skip(final Long what) throws IOException {
        // Logger.i(TAG, "skip(" + what + ")")
        seek(position() + what)
        return what
    }

    public Unit close() {
        IOUtils.closeQuietly(fileChannel)
    }

}
