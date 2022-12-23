/**
 * DataOutputStream for fast-compact encoding of number sequences
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public final class MixCoderDataOutputStream extends DataOutputStream {
    private int lastValue;
    private int lastLastValue;
    private int repCount;
    private int diffshift;

    private int bm = 1; // byte mask (write mode)
    private int b = 0;

    public static int[] diffs = new int[100];
    public static int[] counts = new int[100];

    public MixCoderDataOutputStream(final OutputStream os) {
        super(os);
    }

    public void writeMixed(final int v) throws IOException {
        if (v != lastValue && repCount > 0) {
            int d = lastValue - lastLastValue;
            lastLastValue = lastValue;

            encodeBit(d < 0);
            if (d < 0) {
                d = -d;
            }
            encodeVarBits(d - diffshift);
            encodeVarBits(repCount - 1);

            if (d < 100) {
                diffs[d]++;
            }
            if (repCount < 100) {
                counts[repCount]++;
            }

            diffshift = 1;
            repCount = 0;
        }
        lastValue = v;
        repCount++;
    }

    @Override
    public void flush() throws IOException {
        final int v = lastValue;
        writeMixed(v + 1);
        lastValue = v;
        repCount = 0;
        if (bm > 1) {
            writeByte((byte) b); // flush bit-coding
        }
    }

    public void encodeBit(final boolean value) throws IOException {
        if (bm == 0x100) {
            writeByte((byte) b);
            bm = 1;
            b = 0;
        }
        if (value) {
            b |= bm;
        }
        bm <<= 1;
    }

    public void encodeVarBits(int value) throws IOException {
        int range = 0;
        while (value > range) {
            encodeBit(false);
            value -= range + 1;
            range = 2 * range + 1;
        }
        encodeBit(true);
        encodeBounded(range, value);
    }

    public void encodeBounded(int max, final int value) throws IOException {
        int im = 1; // integer mask
        while (im <= max) {
            if (bm == 0x100) {
                writeByte((byte) b);
                bm = 1;
                b = 0;
            }
            if ((value & im) != 0) {
                b |= bm;
                max -= im;
            }
            bm <<= 1;
            im <<= 1;
        }
    }

    public static void stats() {
        for (int i = 1; i < 100; i++) {
            System.out.println("diff[" + i + "] = " + diffs[i]);
        }
        for (int i = 1; i < 100; i++) {
            System.out.println("counts[" + i + "] = " + counts[i]);
        }
    }
}
