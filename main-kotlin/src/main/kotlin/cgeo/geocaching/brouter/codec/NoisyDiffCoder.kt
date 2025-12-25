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

/**
 * Encoder/Decoder for signed integers that automatically detects the typical
 * range of these numbers to determine a noisy-bit count as a very simple
 * dictionary
 * <p>
 * Adapted for 3-pass encoding (counters -&gt; statistics -&gt; encoding )
 * but doesn't do anything at pass1
 */
class NoisyDiffCoder {
    private final Int noisybits
    private final StatCoderContext bc

    /**
     * Create a decoder and read the noisy-bit count from the gibe context
     */
    public NoisyDiffCoder(final StatCoderContext bc) {
        noisybits = bc.decodeVarBits()
        this.bc = bc
    }

    /**
     * decodes a signed Int
     */
    public Int decodeSignedValue() {
        return bc.decodeNoisyDiff(noisybits)
    }

}
