package cgeo.geocaching.brouter.codec;

/**
 * Encoder/Decoder for signed integers that automatically detects the typical
 * range of these numbers to determine a noisy-bit count as a very simple
 * dictionary
 * <p>
 * Adapted for 3-pass encoding (counters -&gt; statistics -&gt; encoding )
 * but doesn't do anything at pass1
 */
public final class NoisyDiffCoder {
    private final int noisybits;
    private final StatCoderContext bc;

    /**
     * Create a decoder and read the noisy-bit count from the gibe context
     */
    public NoisyDiffCoder(final StatCoderContext bc) {
        noisybits = bc.decodeVarBits();
        this.bc = bc;
    }

    /**
     * decodes a signed int
     */
    public int decodeSignedValue() {
        return bc.decodeNoisyDiff(noisybits);
    }

}
