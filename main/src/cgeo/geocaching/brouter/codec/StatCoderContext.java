package cgeo.geocaching.brouter.codec;

import java.util.TreeMap;

import cgeo.geocaching.brouter.util.BitCoderContext;

public final class StatCoderContext extends BitCoderContext {
    private static final int[] noisy_bits = new int[1024];
    private static TreeMap<String, long[]> statsPerName;

    static {
        // noisybits lookup
        for (int i = 0; i < 1024; i++) {
            int p = i;
            int noisybits = 0;
            while (p > 2) {
                noisybits++;
                p >>= 1;
            }
            noisy_bits[i] = noisybits;
        }
    }

    private long lastbitpos = 0;


    public StatCoderContext(byte[] ab) {
        super(ab);
    }

    /**
     * Get a textual report on the bit-statistics
     *
     * @see #assignBits
     */
    public static String getBitReport() {
        if (statsPerName == null) {
            return "<empty bit report>";
        }
        StringBuilder sb = new StringBuilder();
        for (String name : statsPerName.keySet()) {
            long[] stats = statsPerName.get(name);
            sb.append(name + " count=" + stats[1] + " bits=" + stats[0] + "\n");
        }
        statsPerName = null;
        return sb.toString();
    }

    /**
     * assign the de-/encoded bits since the last call assignBits to the given
     * name. Used for encoding statistics
     *
     * @see #getBitReport
     */
    public void assignBits(String name) {
        long bitpos = getWritingBitPosition();
        if (statsPerName == null) {
            statsPerName = new TreeMap<String, long[]>();
        }
        long[] stats = statsPerName.get(name);
        if (stats == null) {
            stats = new long[2];
            statsPerName.put(name, stats);
        }
        stats[0] += bitpos - lastbitpos;
        stats[1] += 1;
        lastbitpos = bitpos;
    }

    /**
     * encode an unsigned integer with some of of least significant bits
     * considered noisy
     *
     * @see #decodeNoisyNumber
     */
    public void encodeNoisyNumber(int value, int noisybits) {
        if (value < 0) {
            throw new IllegalArgumentException("encodeVarBits expects positive value");
        }
        if (noisybits > 0) {
            int mask = 0xffffffff >>> (32 - noisybits);
            encodeBounded(mask, value & mask);
            value >>= noisybits;
        }
        encodeVarBits(value);
    }

    /**
     * decode an unsigned integer with some of of least significant bits
     * considered noisy
     *
     * @see #encodeNoisyNumber
     */
    public int decodeNoisyNumber(int noisybits) {
        int value = decodeBits(noisybits);
        return value | (decodeVarBits() << noisybits);
    }

    /**
     * encode a signed integer with some of of least significant bits considered
     * noisy
     *
     * @see #decodeNoisyDiff
     */
    public void encodeNoisyDiff(int value, int noisybits) {
        if (noisybits > 0) {
            value += 1 << (noisybits - 1);
            int mask = 0xffffffff >>> (32 - noisybits);
            encodeBounded(mask, value & mask);
            value >>= noisybits;
        }
        encodeVarBits(value < 0 ? -value : value);
        if (value != 0) {
            encodeBit(value < 0);
        }
    }

    /**
     * decode a signed integer with some of of least significant bits considered
     * noisy
     *
     * @see #encodeNoisyDiff
     */
    public int decodeNoisyDiff(int noisybits) {
        int value = 0;
        if (noisybits > 0) {
            value = decodeBits(noisybits) - (1 << (noisybits - 1));
        }
        int val2 = decodeVarBits() << noisybits;
        if (val2 != 0) {
            if (decodeBit()) {
                val2 = -val2;
            }
        }
        return value + val2;
    }

    /**
     * encode a signed integer with the typical range and median taken from the
     * predicted value
     *
     * @see #decodePredictedValue
     */
    public void encodePredictedValue(int value, int predictor) {
        int p = predictor < 0 ? -predictor : predictor;
        int noisybits = 0;

        while (p > 2) {
            noisybits++;
            p >>= 1;
        }
        encodeNoisyDiff(value - predictor, noisybits);
    }

    /**
     * decode a signed integer with the typical range and median taken from the
     * predicted value
     *
     * @see #encodePredictedValue
     */
    public int decodePredictedValue(int predictor) {
        int p = predictor < 0 ? -predictor : predictor;
        int noisybits = 0;
        while (p > 1023) {
            noisybits++;
            p >>= 1;
        }
        return predictor + decodeNoisyDiff(noisybits + noisy_bits[p]);
    }

    /**
     * encode an integer-array making use of the fact that it is sorted. This is
     * done, starting with the most significant bit, by recursively encoding the
     * number of values with the current bit being 0. This yields an number of
     * bits per value that only depends on the typical distance between subsequent
     * values and also benefits
     *
     * @param values  the array to encode
     * @param offset  position in this array where to start
     * @param subsize number of values to encode
     * @param nextbit bitmask with the most significant bit set to 1
     * @param mask    should be 0
     */
    public void encodeSortedArray(int[] values, int offset, int subsize, int nextbit, int mask) {
        if (subsize == 1) // last-choice shortcut
        {
            while (nextbit != 0) {
                encodeBit((values[offset] & nextbit) != 0);
                nextbit >>= 1;
            }
        }
        if (nextbit == 0) {
            return;
        }

        int data = mask & values[offset];
        mask |= nextbit;

        // count 0-bit-fraction
        int i = offset;
        int end = subsize + offset;
        for (; i < end; i++) {
            if ((values[i] & mask) != data) {
                break;
            }
        }
        int size1 = i - offset;
        int size2 = subsize - size1;

        encodeBounded(subsize, size1);
        if (size1 > 0) {
            encodeSortedArray(values, offset, size1, nextbit >> 1, mask);
        }
        if (size2 > 0) {
            encodeSortedArray(values, i, size2, nextbit >> 1, mask);
        }
    }

    /**
     * @param values  the array to encode
     * @param offset  position in this array where to start
     * @param subsize number of values to encode
     * @param nextbit bitmask with the most significant bit set to 1
     * @param value   should be 0
     * @see #encodeSortedArray
     */
    public void decodeSortedArray(int[] values, int offset, int subsize, int nextbitpos, int value) {
        if (subsize == 1) // last-choice shortcut
        {
            if (nextbitpos >= 0) {
                value |= decodeBitsReverse(nextbitpos + 1);
            }
            values[offset] = value;
            return;
        }
        if (nextbitpos < 0) {
            while (subsize-- > 0) {
                values[offset++] = value;
            }
            return;
        }

        int size1 = decodeBounded(subsize);
        int size2 = subsize - size1;

        if (size1 > 0) {
            decodeSortedArray(values, offset, size1, nextbitpos - 1, value);
        }
        if (size2 > 0) {
            decodeSortedArray(values, offset + size1, size2, nextbitpos - 1, value | (1 << nextbitpos));
        }
    }

}
