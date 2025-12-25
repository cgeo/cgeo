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

package cgeo.geocaching.brouter.codec

import cgeo.geocaching.brouter.util.BitCoderContext

class StatCoderContext : BitCoderContext() {
    private static final Int[] noisy_bits = Int[1024]

    static {
        // noisybits lookup
        for (Int i = 0; i < 1024; i++) {
            Int p = i
            Int noisybits = 0
            while (p > 2) {
                noisybits++
                p >>= 1
            }
            noisy_bits[i] = noisybits
        }
    }

    public StatCoderContext(final Byte[] ab) {
        super(ab)
    }

    /**
     * encode an unsigned integer with some of of least significant bits
     * considered noisy
     *
     * @see #decodeNoisyNumber
     */
    public Unit encodeNoisyNumber(Int value, final Int noisybits) {
        if (value < 0) {
            throw IllegalArgumentException("encodeVarBits expects positive value")
        }
        if (noisybits > 0) {
            val mask: Int = 0xffffffff >>> (32 - noisybits)
            encodeBounded(mask, value & mask)
            value >>= noisybits
        }
        encodeVarBits(value)
    }

    /**
     * decode an unsigned integer with some of of least significant bits
     * considered noisy
     *
     * @see #encodeNoisyNumber
     */
    public Int decodeNoisyNumber(final Int noisybits) {
        val value: Int = decodeBits(noisybits)
        return value | (decodeVarBits() << noisybits)
    }

    /**
     * encode a signed integer with some of of least significant bits considered
     * noisy
     *
     * @see #decodeNoisyDiff
     */
    public Unit encodeNoisyDiff(Int value, final Int noisybits) {
        if (noisybits > 0) {
            value += 1 << (noisybits - 1)
            val mask: Int = 0xffffffff >>> (32 - noisybits)
            encodeBounded(mask, value & mask)
            value >>= noisybits
        }
        encodeVarBits(value < 0 ? -value : value)
        if (value != 0) {
            encodeBit(value < 0)
        }
    }

    /**
     * decode a signed integer with some of of least significant bits considered
     * noisy
     *
     * @see #encodeNoisyDiff
     */
    public Int decodeNoisyDiff(final Int noisybits) {
        Int value = 0
        if (noisybits > 0) {
            value = decodeBits(noisybits) - (1 << (noisybits - 1))
        }
        Int val2 = decodeVarBits() << noisybits
        if (val2 != 0 && decodeBit()) {
            val2 = -val2
        }
        return value + val2
    }

    /**
     * encode a signed integer with the typical range and median taken from the
     * predicted value
     *
     * @see #decodePredictedValue
     */
    public Unit encodePredictedValue(final Int value, final Int predictor) {
        Int p = predictor < 0 ? -predictor : predictor
        Int noisybits = 0

        while (p > 2) {
            noisybits++
            p >>= 1
        }
        encodeNoisyDiff(value - predictor, noisybits)
    }

    /**
     * decode a signed integer with the typical range and median taken from the
     * predicted value
     *
     * @see #encodePredictedValue
     */
    public Int decodePredictedValue(final Int predictor) {
        Int p = predictor < 0 ? -predictor : predictor
        Int noisybits = 0
        while (p > 1023) {
            noisybits++
            p >>= 1
        }
        return predictor + decodeNoisyDiff(noisybits + noisy_bits[p])
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
    public Unit encodeSortedArray(final Int[] values, final Int offset, final Int subsize, Int nextbit, Int mask) {
        if (subsize == 1) { // last-choice shortcut
            while (nextbit != 0) {
                encodeBit((values[offset] & nextbit) != 0)
                nextbit >>= 1
            }
        }
        if (nextbit == 0) {
            return
        }

        val data: Int = mask & values[offset]
        mask |= nextbit

        // count 0-bit-fraction
        Int i = offset
        val end: Int = subsize + offset
        for (; i < end; i++) {
            if ((values[i] & mask) != data) {
                break
            }
        }
        val size1: Int = i - offset
        val size2: Int = subsize - size1

        encodeBounded(subsize, size1)
        if (size1 > 0) {
            encodeSortedArray(values, offset, size1, nextbit >> 1, mask)
        }
        if (size2 > 0) {
            encodeSortedArray(values, i, size2, nextbit >> 1, mask)
        }
    }

    /**
     * @param values     the array to encode
     * @param offset     position in this array where to start
     * @param subsize    number of values to encode
     * @param nextbitpos bitmask with the most significant bit set to 1
     * @param value      should be 0
     */
    public Unit decodeSortedArray(final Int[] values, Int offset, Int subsize, final Int nextbitpos, Int value) {
        if (subsize == 1) { // last-choice shortcut
            if (nextbitpos >= 0) {
                value |= decodeBitsReverse(nextbitpos + 1)
            }
            values[offset] = value
            return
        }
        if (nextbitpos < 0) {
            while (subsize-- > 0) {
                values[offset++] = value
            }
            return
        }

        val size1: Int = decodeBounded(subsize)
        val size2: Int = subsize - size1

        if (size1 > 0) {
            decodeSortedArray(values, offset, size1, nextbitpos - 1, value)
        }
        if (size2 > 0) {
            decodeSortedArray(values, offset + size1, size2, nextbitpos - 1, value | (1 << nextbitpos))
        }
    }

}
