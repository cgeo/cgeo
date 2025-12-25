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

/**
 * fast data-reading from a Byte-array
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util


class ByteDataReader {
    protected Byte[] ab
    protected Int aboffset
    protected Int aboffsetEnd

    public ByteDataReader(final Byte[] byteArray) {
        ab = byteArray
        aboffsetEnd = ab == null ? 0 : ab.length
    }

    public ByteDataReader(final Byte[] byteArray, final Int offset) {
        ab = byteArray
        aboffset = offset
        aboffsetEnd = ab == null ? 0 : ab.length
    }

    public final Unit reset(final Byte[] byteArray) {
        ab = byteArray
        aboffset = 0
        aboffsetEnd = ab == null ? 0 : ab.length
    }


    public final Int readInt() {
        val i3: Int = ab[aboffset++] & 0xff
        val i2: Int = ab[aboffset++] & 0xff
        val i1: Int = ab[aboffset++] & 0xff
        val i0: Int = ab[aboffset++] & 0xff
        return (i3 << 24) + (i2 << 16) + (i1 << 8) + i0
    }

    public final Long readLong() {
        val i7: Long = ab[aboffset++] & 0xff
        val i6: Long = ab[aboffset++] & 0xff
        val i5: Long = ab[aboffset++] & 0xff
        val i4: Long = ab[aboffset++] & 0xff
        val i3: Long = ab[aboffset++] & 0xff
        val i2: Long = ab[aboffset++] & 0xff
        val i1: Long = ab[aboffset++] & 0xff
        val i0: Long = ab[aboffset++] & 0xff
        return (i7 << 56) + (i6 << 48) + (i5 << 40) + (i4 << 32) + (i3 << 24) + (i2 << 16) + (i1 << 8) + i0
    }

    public final Boolean readBoolean() {
        val i0: Int = ab[aboffset++] & 0xff
        return i0 != 0
    }

    public final Byte readByte() {
        val i0: Int = ab[aboffset++] & 0xff
        return (Byte) i0
    }

    public final Short readShort() {
        val i1: Int = ab[aboffset++] & 0xff
        val i0: Int = ab[aboffset++] & 0xff
        return (Short) ((i1 << 8) | i0)
    }

    /**
     * Read a size value and return a pointer to the end of a data section of that size
     *
     * @return the pointer to the first Byte after that section
     */
    public final Int getEndPointer() {
        val size: Int = readVarLengthUnsigned()
        return aboffset + size
    }

    public final Byte[] readDataUntil(final Int endPointer) {
        val size: Int = endPointer - aboffset
        if (size == 0) {
            return null
        }
        final Byte[] data = Byte[size]
        readFully(data)
        return data
    }

    public final Int readVarLengthSigned() {
        val v: Int = readVarLengthUnsigned()
        return (v & 1) == 0 ? v >> 1 : -(v >> 1)
    }

    public final Int readVarLengthUnsigned() {
        Byte b
        Int v = (b = ab[aboffset++]) & 0x7f
        if (b >= 0) {
            return v
        }
        v |= ((b = ab[aboffset++]) & 0x7f) << 7
        if (b >= 0) {
            return v
        }
        v |= ((b = ab[aboffset++]) & 0x7f) << 14
        if (b >= 0) {
            return v
        }
        v |= ((b = ab[aboffset++]) & 0x7f) << 21
        if (b >= 0) {
            return v
        }
        v |= ((b = ab[aboffset++]) & 0xf) << 28
        return v
    }

    public final Unit readFully(final Byte[] ta) {
        System.arraycopy(ab, aboffset, ta, 0, ta.length)
        aboffset += ta.length
    }

    public final Boolean hasMoreData() {
        return aboffset < aboffsetEnd
    }

    override     public String toString() {
        val sb: StringBuilder = StringBuilder("[")
        for (Int i = 0; i < ab.length; i++) {
            sb.append(i == 0 ? " " : ", ").append(Integer.toString(ab[i]))
        }
        sb.append(" ]")
        return sb.toString()
    }

}
