package cgeo.geocaching.brouter.util;

public final class ByteArrayUnifier implements IByteArrayUnifier {
    private final byte[][] byteArrayCache;
    private int[] crcCrosscheck;
    private final int size;

    public ByteArrayUnifier(int size, boolean validateImmutability) {
        this.size = size;
        byteArrayCache = new byte[size][];
        if (validateImmutability)
            crcCrosscheck = new int[size];
    }

    /**
     * Unify a byte array in order to reuse instances when possible.
     * The byte arrays are assumed to be treated as immutable,
     * allowing the reuse
     *
     * @param ab the byte array to unify
     * @return the cached instance or the input instanced if not cached
     */
    public byte[] unify(byte[] ab) {
        return unify(ab, 0, ab.length);
    }

    public byte[] unify(byte[] ab, int offset, int len) {
        int crc = Crc32.crc(ab, offset, len);
        int idx = (crc & 0xfffffff) % size;
        byte[] abc = byteArrayCache[idx];
        if (abc != null && abc.length == len) {
            int i = 0;
            while (i < len) {
                if (ab[offset + i] != abc[i])
                    break;
                i++;
            }
            if (i == len)
                return abc;
        }
        if (crcCrosscheck != null) {
            if (byteArrayCache[idx] != null) {
                byte[] abold = byteArrayCache[idx];
                int crcold = Crc32.crc(abold, 0, abold.length);
                if (crcold != crcCrosscheck[idx])
                    throw new IllegalArgumentException("ByteArrayUnifier: immutablity validation failed!");
            }
            crcCrosscheck[idx] = crc;
        }
        byte[] nab = new byte[len];
        System.arraycopy(ab, offset, nab, 0, len);
        byteArrayCache[idx] = nab;
        return nab;
    }
}
