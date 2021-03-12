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
    private int tot;
    private int[] freqs;
    private int noisybits;
    private StatCoderContext bc;
    private int pass;

    /**
     * Create a decoder and read the noisy-bit count from the gibe context
     */
    public NoisyDiffCoder(final StatCoderContext bc) {
        noisybits = bc.decodeVarBits();
        this.bc = bc;
    }

    /**
     * Create an encoder for 3-pass-encoding
     */
    public NoisyDiffCoder() {
    }

    /**
     * encodes a signed int (pass3 only, stats collection in pass2)
     */
    public void encodeSignedValue(final int value) {
        if (pass == 3) {
            bc.encodeNoisyDiff(value, noisybits);
        } else if (pass == 2) {
            count(value < 0 ? -value : value);
        }
    }

    /**
     * decodes a signed int
     */
    public int decodeSignedValue() {
        return bc.decodeNoisyDiff(noisybits);
    }

    /**
     * Starts a new encoding pass and (in pass3) calculates the noisy-bit count
     * from the stats collected in pass2 and writes that to the given context
     */
    public void encodeDictionary(final StatCoderContext bc) {
        if (++pass == 3) {
            // how many noisy bits?
            for (noisybits = 0; noisybits < 14 && tot > 0; noisybits++) {
                if (freqs[noisybits] < (tot >> 1)) {
                    break;
                }
            }
            bc.encodeVarBits(noisybits);
        }
        this.bc = bc;
    }

    private void count(final int value) {
        if (freqs == null) {
            freqs = new int[14];
        }
        int bm = 1;
        for (int i = 0; i < 14; i++) {
            if (value < bm) {
                break;
            } else {
                freqs[i]++;
            }
            bm <<= 1;
        }
        tot++;
    }
}
