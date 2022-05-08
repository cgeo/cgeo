package cgeo.geocaching.storage;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;

/**
 * Helper to read files when only a {@link java.nio.channels.FileChannel} is available.
 *
 * Mimics the necessary parts of RandomAccessFile
 */
public class FileByteReader implements Closeable {

    private final FileChannel fileChannel;

    public FileByteReader(final FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    public FileByteReader(final FileInputStream fis) {
        this.fileChannel = fis.getChannel();
    }

    /**
     * Reads bytes from a file
     *
     * @param startPos position to read from in the file
     * @param length   number of bytes to read
     * @param buffer   buffer to store read bytes in
     * @throws IOException in case anything goes wrong
     */
    public void readFully(final long startPos, final int length, @NonNull final byte[] buffer) throws IOException {
        final int readBytes = readFile(this.fileChannel, startPos, length, buffer, 0);
        if (readBytes != length) {
            throw new IOException("Could not read requested number of bytes (" + buffer.length + "), read only " + readBytes + " bytges");
        }
    }

    private static int readFile(@NonNull final FileChannel channel, final long startPos, final int length, @NonNull final byte[] buffer, final int bufferOffset) throws IOException {
        if (bufferOffset + length > buffer.length) {
            throw new IllegalArgumentException("Requested read length " + length + " will not fit in given buffer length " + buffer.length + " (offset: " + bufferOffset + ")");
        }

        final ByteBuffer bb = ByteBuffer.wrap(buffer, bufferOffset, length);
        return channel.read(bb, startPos);
    }

    public long size() throws IOException {
        return this.fileChannel.size();
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(this.fileChannel);
    }
}
