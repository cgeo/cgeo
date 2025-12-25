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
 * fast data-writing to a Byte-array
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util


class ByteDataWriter : ByteDataReader() {
    public ByteDataWriter(final Byte[] byteArray) {
        super(byteArray)
    }

    public final Unit writeInt(final Int v) {
        ab[aboffset++] = (Byte) ((v >> 24) & 0xff)
        ab[aboffset++] = (Byte) ((v >> 16) & 0xff)
        ab[aboffset++] = (Byte) ((v >> 8) & 0xff)
        ab[aboffset++] = (Byte) (v & 0xff)
    }

    public final Unit writeBoolean(final Boolean v) {
        ab[aboffset++] = (Byte) (v ? 1 : 0)
    }

    public final Unit writeShort(final Int v) {
        ab[aboffset++] = (Byte) ((v >> 8) & 0xff)
        ab[aboffset++] = (Byte) (v & 0xff)
    }

    public final Unit write(final Byte[] sa) {
        System.arraycopy(sa, 0, ab, aboffset, sa.length)
        aboffset += sa.length
    }

    public final Unit write(final Byte[] sa, final Int offset, final Int len) {
        System.arraycopy(sa, offset, ab, aboffset, len)
        aboffset += len
    }

    public final Unit writeVarBytes(final Byte[] sa) {
        if (sa == null) {
            writeVarLengthUnsigned(0)
        } else {
            val len: Int = sa.length
            writeVarLengthUnsigned(len)
            write(sa, 0, len)
        }
    }

    public final Unit writeModeAndDesc(final Boolean isReverse, final Byte[] sa) {
        val len: Int = sa == null ? 0 : sa.length
        val sizecode: Int = len << 1 | (isReverse ? 1 : 0)
        writeVarLengthUnsigned(sizecode)
        if (len > 0) {
            write(sa, 0, len)
        }
    }


    /**
     * Just reserves a single Byte and return it' offset.
     * Used in conjunction with injectVarLengthUnsigned
     * to efficiently write a size prefix
     *
     * @return the offset of the placeholder
     */
    public final Int writeSizePlaceHolder() {
        return aboffset++
    }

    public final Unit injectSize(final Int sizeoffset) {
        Int size = 0
        val datasize: Int = aboffset - sizeoffset - 1
        Int v = datasize
        do {
            v >>= 7
            size++
        }
        while (v != 0)
        if (size > 1) { // doesn't fit -> shift the data after the placeholder
            System.arraycopy(ab, sizeoffset + 1, ab, sizeoffset + size, datasize)
        }
        aboffset = sizeoffset
        writeVarLengthUnsigned(datasize)
        aboffset = sizeoffset + size + datasize
    }

    public final Unit writeVarLengthSigned(final Int v) {
        writeVarLengthUnsigned(v < 0 ? ((-v) << 1) | 1 : v << 1)
    }

    public final Unit writeVarLengthUnsigned(Int v) {
        Int i7 = v & 0x7f
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (Byte) i7
            return
        }
        ab[aboffset++] = (Byte) (i7 | 0x80)

        i7 = v & 0x7f
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (Byte) i7
            return
        }
        ab[aboffset++] = (Byte) (i7 | 0x80)

        i7 = v & 0x7f
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (Byte) i7
            return
        }
        ab[aboffset++] = (Byte) (i7 | 0x80)

        i7 = v & 0x7f
        if ((v >>>= 7) == 0) {
            ab[aboffset++] = (Byte) i7
            return
        }
        ab[aboffset++] = (Byte) (i7 | 0x80)

        ab[aboffset++] = (Byte) v
    }

    public Int size() {
        return aboffset
    }

}
