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

package cgeo.geocaching.storage

import androidx.annotation.NonNull

import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import org.apache.commons.io.IOUtils

/**
 * Helper to read files when only a {@link java.nio.channels.FileChannel} is available.
 * <br>
 * Mimics the necessary parts of RandomAccessFile
 */
class FileByteReader : Closeable {

    private final FileChannel fileChannel

    public FileByteReader(final FileChannel fileChannel) {
        this.fileChannel = fileChannel
    }

    public FileByteReader(final FileInputStream fis) {
        this.fileChannel = fis.getChannel()
    }

    /**
     * Reads bytes from a file
     *
     * @param startPos position to read from in the file
     * @param length   number of bytes to read
     * @param buffer   buffer to store read bytes in
     * @throws IOException in case anything goes wrong
     */
    public Unit readFully(final Long startPos, final Int length, final Byte[] buffer) throws IOException {
        val readBytes: Int = readFile(this.fileChannel, startPos, length, buffer, 0)
        if (readBytes != length) {
            throw IOException("Could not read requested number of bytes (" + buffer.length + "), read only " + readBytes + " bytges")
        }
    }

    private static Int readFile(final FileChannel channel, final Long startPos, final Int length, final Byte[] buffer, final Int bufferOffset) throws IOException {
        if (bufferOffset + length > buffer.length) {
            throw IllegalArgumentException("Requested read length " + length + " will not fit in given buffer length " + buffer.length + " (offset: " + bufferOffset + ")")
        }

        val bb: ByteBuffer = ByteBuffer.wrap(buffer, bufferOffset, length)
        return channel.read(bb, startPos)
    }

    public Long size() throws IOException {
        return this.fileChannel.size()
    }

    override     public Unit close() {
        IOUtils.closeQuietly(this.fileChannel)
    }
}
