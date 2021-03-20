/**
 * fast data-reading from a byte-array
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util;


public class ByteDataReader {
    protected byte[] ab;
    protected int aboffset;
    protected int aboffsetEnd;

    public ByteDataReader(byte[] byteArray) {
        ab = byteArray;
        aboffsetEnd = ab == null ? 0 : ab.length;
    }

    public ByteDataReader(byte[] byteArray, int offset) {
        ab = byteArray;
        aboffset = offset;
        aboffsetEnd = ab == null ? 0 : ab.length;
    }

    public final void reset(byte[] byteArray) {
        ab = byteArray;
        aboffset = 0;
        aboffsetEnd = ab == null ? 0 : ab.length;
    }


    public final int readInt() {
        int i3 = ab[aboffset++] & 0xff;
        int i2 = ab[aboffset++] & 0xff;
        int i1 = ab[aboffset++] & 0xff;
        int i0 = ab[aboffset++] & 0xff;
        return (i3 << 24) + (i2 << 16) + (i1 << 8) + i0;
    }

    public final long readLong() {
        long i7 = ab[aboffset++] & 0xff;
        long i6 = ab[aboffset++] & 0xff;
        long i5 = ab[aboffset++] & 0xff;
        long i4 = ab[aboffset++] & 0xff;
        long i3 = ab[aboffset++] & 0xff;
        long i2 = ab[aboffset++] & 0xff;
        long i1 = ab[aboffset++] & 0xff;
        long i0 = ab[aboffset++] & 0xff;
        return (i7 << 56) + (i6 << 48) + (i5 << 40) + (i4 << 32) + (i3 << 24) + (i2 << 16) + (i1 << 8) + i0;
    }

    public final boolean readBoolean() {
        int i0 = ab[aboffset++] & 0xff;
        return i0 != 0;
    }

    public final byte readByte() {
        int i0 = ab[aboffset++] & 0xff;
        return (byte) (i0);
    }

    public final short readShort() {
        int i1 = ab[aboffset++] & 0xff;
        int i0 = ab[aboffset++] & 0xff;
        return (short) ((i1 << 8) | i0);
    }

    /**
     * Read a size value and return a pointer to the end of a data section of that size
     *
     * @return the pointer to the first byte after that section
     */
    public final int getEndPointer() {
        int size = readVarLengthUnsigned();
        return aboffset + size;
    }

    public final byte[] readDataUntil(int endPointer) {
        int size = endPointer - aboffset;
        if (size == 0) {
            return null;
        }
        byte[] data = new byte[size];
        readFully(data);
        return data;
    }

    public final byte[] readVarBytes() {
        int len = readVarLengthUnsigned();
        if (len == 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        readFully(bytes);
        return bytes;
    }

    public final int readVarLengthSigned() {
        int v = readVarLengthUnsigned();
        return (v & 1) == 0 ? v >> 1 : -(v >> 1);
    }

    public final int readVarLengthUnsigned() {
        byte b;
        int v = (b = ab[aboffset++]) & 0x7f;
        if (b >= 0)
            return v;
        v |= ((b = ab[aboffset++]) & 0x7f) << 7;
        if (b >= 0)
            return v;
        v |= ((b = ab[aboffset++]) & 0x7f) << 14;
        if (b >= 0)
            return v;
        v |= ((b = ab[aboffset++]) & 0x7f) << 21;
        if (b >= 0)
            return v;
        v |= ((b = ab[aboffset++]) & 0xf) << 28;
        return v;
    }

    public final void readFully(byte[] ta) {
        System.arraycopy(ab, aboffset, ta, 0, ta.length);
        aboffset += ta.length;
    }

    public final boolean hasMoreData() {
        return aboffset < aboffsetEnd;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ab.length; i++)
            sb.append(i == 0 ? " " : ", ").append(Integer.toString(ab[i]));
        sb.append(" ]");
        return sb.toString();
    }

}
