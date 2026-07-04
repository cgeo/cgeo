package cgeo.geocaching.wherigo.openwig.formats;

import cgeo.geocaching.wherigo.openwig.platform.SeekableFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a synthetic, byte-exact GWC cartridge file in memory, matching the binary layout that
 * {@link CartridgeFile} expects (as implemented by the production {@code WSeekableFile}: every
 * multi-byte number is little-endian, and strings are raw bytes terminated by a byte &lt;= 0).
 * Lets tests exercise the real parsing/extraction logic without touching the filesystem.
 */
final class GwcFixture {

    private static final byte[] CART_ID = {0x02, 0x0a, 'C', 'A', 'R', 'T', 0x00};

    private byte[] bytecode = new byte[0];
    private final List<Integer> ids = new ArrayList<>();
    private final List<byte[]> contents = new ArrayList<>();

    double latitude, longitude;
    short splashId, iconId;
    String type = "", member = "", name = "", guid = "", description = "", startdesc = "",
        version = "", author = "", url = "", device = "", code = "";

    GwcFixture withBytecode(final byte[] bytes) {
        this.bytecode = bytes;
        return this;
    }

    GwcFixture withResource(final int id, final byte[] content) {
        ids.add(id);
        contents.add(content);
        return this;
    }

    /** Encodes the fixture into the GWC binary format understood by {@link CartridgeFile#read}. */
    byte[] build() throws IOException {
        // file entry 0 is always the bytecode; its "id" is never looked up via getFile(), so any
        // value distinct from real resource ids (which start at 1) works - the real format uses 0
        final ByteArrayOutputStream fileBodies = new ByteArrayOutputStream();
        final int[] offsets = new int[1 + ids.size()];
        final int[] entryIds = new int[1 + ids.size()];

        final ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeInt(header, 0); // header length, read but never used by CartridgeFile
        writeDouble(header, latitude);
        writeDouble(header, longitude);
        header.write(new byte[8]); // zeroes
        header.write(new byte[8]); // two unknown long values
        writeShort(header, splashId);
        writeShort(header, iconId);
        writeString(header, type);
        writeString(header, member);
        header.write(new byte[8]); // two unknown long values
        writeString(header, name);
        writeString(header, guid);
        writeString(header, description);
        writeString(header, startdesc);
        writeString(header, version);
        writeString(header, author);
        writeString(header, url);
        writeString(header, device);
        header.write(new byte[4]); // unknown long value
        writeString(header, code);

        final int offsetsTableLength = 6 * offsets.length; // each entry: short id + int offset
        final int fileEntriesStart = CART_ID.length + 2 + offsetsTableLength + header.size();

        int pos = fileEntriesStart;
        entryIds[0] = 0;
        offsets[0] = pos;
        writeInt(fileBodies, bytecode.length);
        fileBodies.write(bytecode);
        pos += 4 + bytecode.length;

        for (int i = 0; i < ids.size(); i++) {
            entryIds[i + 1] = ids.get(i);
            offsets[i + 1] = pos;
            final byte[] content = contents.get(i);
            fileBodies.write(1); // presence marker: non-deleted
            writeInt(fileBodies, 0); // resource type, unused by CartridgeFile
            writeInt(fileBodies, content.length);
            fileBodies.write(content);
            pos += 1 + 4 + 4 + content.length;
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(CART_ID);
        writeShort(out, (short) offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            writeShort(out, (short) entryIds[i]);
            writeInt(out, offsets[i]);
        }
        header.writeTo(out);
        fileBodies.writeTo(out);
        return out.toByteArray();
    }

    SeekableFile toSeekableFile() throws IOException {
        return new InMemorySeekableFile(build());
    }

    /** Wraps arbitrary raw bytes, e.g. to feed a deliberately-malformed cartridge to a test. */
    static SeekableFile fromRawBytes(final byte[] data) {
        return new InMemorySeekableFile(data);
    }

    private static void writeShort(final ByteArrayOutputStream out, final short v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void writeInt(final ByteArrayOutputStream out, final int v) {
        for (int i = 0; i < 4; i++) {
            out.write((v >> (i * 8)) & 0xFF);
        }
    }

    private static void writeDouble(final ByteArrayOutputStream out, final double d) {
        final long bits = Double.doubleToLongBits(d);
        for (int i = 0; i < 8; i++) {
            out.write((int) ((bits >> (i * 8)) & 0xFF));
        }
    }

    private static void writeString(final ByteArrayOutputStream out, final String s) {
        out.write(s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), 0, s.length());
        out.write(0);
    }

    /** In-memory {@link SeekableFile}, encoding/decoding numbers the same way as the real,
     * Android-specific {@code WSeekableFile} implementation (little-endian; raw-byte strings). */
    private static final class InMemorySeekableFile implements SeekableFile {
        private final byte[] data;
        private int pos;

        InMemorySeekableFile(final byte[] data) {
            this.data = data;
        }

        public void seek(final long p) {
            pos = (int) p;
        }

        public long position() {
            return pos;
        }

        public long skip(final long what) {
            pos += (int) what;
            return what;
        }

        public int read() {
            return data[pos++];
        }

        public void readFully(final byte[] buf) {
            System.arraycopy(data, pos, buf, 0, buf.length);
            pos += buf.length;
        }

        public short readShort() {
            final int b0 = data[pos] & 0xFF;
            final int b1 = data[pos + 1];
            pos += 2;
            return (short) ((b1 << 8) | b0);
        }

        public int readInt() {
            int result = 0;
            for (int i = 0; i < 4; i++) {
                result += (data[pos + i] & 0xFF) << (i * 8);
            }
            pos += 4;
            return result;
        }

        public long readLong() {
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result |= ((long) (data[pos + i] & 0xFF)) << (i * 8);
            }
            pos += 8;
            return result;
        }

        public double readDouble() {
            return Double.longBitsToDouble(readLong());
        }

        public String readString() {
            final StringBuilder sb = new StringBuilder();
            byte b = data[pos++];
            while (b > 0) {
                sb.append((char) b);
                b = data[pos++];
            }
            return sb.toString();
        }
    }
}
