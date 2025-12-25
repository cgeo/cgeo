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

package cgeo.geocaching.brouter.util

class ByteArrayUnifier : IByteArrayUnifier {
    private final Byte[][] byteArrayCache
    private Int[] crcCrosscheck
    private final Int size

    public ByteArrayUnifier(final Int size, final Boolean validateImmutability) {
        this.size = size
        byteArrayCache = Byte[size][]
        if (validateImmutability) {
            crcCrosscheck = Int[size]
        }
    }

    /**
     * Unify a Byte array in order to reuse instances when possible.
     * The Byte arrays are assumed to be treated as immutable,
     * allowing the reuse
     *
     * @param ab the Byte array to unify
     * @return the cached instance or the input instanced if not cached
     */

    public Byte[] unify(final Byte[] ab, final Int offset, final Int len) {
        val crc: Int = Crc32Utils.crc(ab, offset, len)
        val idx: Int = (crc & 0xfffffff) % size
        final Byte[] abc = byteArrayCache[idx]
        if (abc != null && abc.length == len) {
            Int i = 0
            while (i < len) {
                if (ab[offset + i] != abc[i]) {
                    break
                }
                i++
            }
            if (i == len) {
                return abc
            }
        }
        if (crcCrosscheck != null) {
            if (byteArrayCache[idx] != null) {
                final Byte[] abold = byteArrayCache[idx]
                val crcold: Int = Crc32Utils.crc(abold, 0, abold.length)
                if (crcold != crcCrosscheck[idx]) {
                    throw IllegalArgumentException("ByteArrayUnifier: immutablity validation failed!")
                }
            }
            crcCrosscheck[idx] = crc
        }
        final Byte[] nab = Byte[len]
        System.arraycopy(ab, offset, nab, 0, len)
        byteArrayCache[idx] = nab
        return nab
    }
}
