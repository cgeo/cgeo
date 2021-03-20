/**
 * fast data-writing to a byte-array
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util;


public class ByteDataWriter extends ByteDataReader {
    public ByteDataWriter(byte[] byteArray) {
        super(byteArray);
    }

    public final void writeInt(int v) {
        ab[aboffset++] = (byte) ((v >> 24) & 0xff);
        ab[aboffset++] = (byte) ((v >> 16) & 0xff);
        ab[aboffset++] = (byte) ((v >> 8) & 0xff);
        ab[aboffset++] = (byte) ((v) & 0xff);
    }

    public final void writeLong(long v) {
        ab[aboffset++] = (byte) ((v >> 56) & 0xff);
        ab[aboffset++] = (byte) ((v >> 48) & 0xff);
        ab[aboffset++] = (byte) ((v >> 40) & 0xff);
        ab[aboffset++] = (byte) ((v >> 32) & 0xff);
        ab[aboffset++] = (byte) ((v >> 24) & 0xff);
        ab[aboffset++] = (byte) ((v >> 16) & 0xff);
        ab[aboffset++] = (byte) ((v >> 8) & 0xff);
        ab[aboffset++] = (byte) ((v) & 0xff);
    }

    public final void writeBoolean(boolean v) {
        ab[aboffset++] = (byte) (v ? 1 : 0);
    }

    public final void writeByte(int v) {
        ab[aboffset++] = (byte) ((v) & 0xff);
    }

    public final void writeShort(int v) {
        ab[aboffset++] = (byte) ((v >> 8) & 0xff);
        ab[aboffset++] = (byte) ((v) & 0xff);
    }

    public final void write(byte[] sa) {
        System.arraycopy(sa, 0, ab, aboffset, sa.length);
        aboffset += sa.length;
    }

    public final void write(byte[] sa, int offset, int len) {
        System.arraycopy(sa, offset, ab, aboffset, len);
        aboffset += len;
    }

    public final void writeVarBytes(byte[] sa) {
        if (sa == null) {
            writeVarLengthUnsigned(0);
        } else {
            int len = sa.length;
            writeVarLengthUnsigned(len);
            write(sa, 0, len);
        }
    }

    public final void writeModeAndDesc(boolean isReverse, byte[] sa) {
        int len = sa == null ? 0 : sa.length;
        int sizecode = len << 1 | (isReverse ? 1 : 0);
        writeVarLengthUnsigned(sizecode);
        if (len > 0) {
            write(sa, 0, len);
        }
    }


    public final byte[] toByteArray() {
        byte[] c = new byte[aboffset];
        System.arraycopy(ab, 0, c, 0, aboffset);
        return c;
    }


    /**
     * Just reserves a single byte and return it' offset.
     * Used in conjunction with injectVarLengthUnsigned
     * to efficiently write a size prefix
     *
     * @return the offset of the placeholder
     */
    public final int writeSizePlaceHolder() {
        return aboffset++;
    }

    public final void injectSize(int sizeoffset) {
        int size = 0;
        int datasize = aboffset - sizeoffset - 1;
        int v = datasize;
        do
        {
            v >>= 7;
            size++;
        }
        while (v != 0);
        if (size > 1) // doesn't fit -> shift the data after the placeholder
        {
            System.arraycopy(ab, sizeoffset + 1, ab, sizeoffset + size, datasize);
        }
        aboffset = sizeoffset;
        writeVarLengthUnsigned(datasize);
        aboffset = sizeoffset + size + datasize;
    }

    public final void writeVarLengthSigned(int v) {
        writeVarLengthUnsigned(v < 0 ? ((-v) << 1) | 1 : v << 1);
    }

    public final void writeVarLengthUnsigned(int v) {
        int i7 = v & 0x7f;
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (byte) (i7);
            return;
        }
        ab[aboffset++] = (byte) (i7 | 0x80);

        i7 = v & 0x7f;
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (byte) (i7);
            return;
        }
        ab[aboffset++] = (byte) (i7 | 0x80);

        i7 = v & 0x7f;
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (byte) (i7);
            return;
        }
        ab[aboffset++] = (byte) (i7 | 0x80);

        i7 = v & 0x7f;
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (byte) (i7);
            return;
        }
        ab[aboffset++] = (byte) (i7 | 0x80);

        ab[aboffset++] = (byte) (v);
        return;
    }

    public int size() {
        return aboffset;
    }

}
