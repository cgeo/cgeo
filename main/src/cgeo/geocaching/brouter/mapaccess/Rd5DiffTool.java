/**
 * Calculate, add or merge rd5 delta files
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.codec.MicroCache2;
import cgeo.geocaching.brouter.codec.StatCoderContext;
import cgeo.geocaching.brouter.util.Crc32;
import cgeo.geocaching.brouter.util.ProgressListener;

final public class Rd5DiffTool implements ProgressListener {
    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            reEncode(new File(args[0]), new File(args[1]));
            return;
        }

        if (args[1].endsWith(".df5")) {
            if (args[0].endsWith(".df5")) {
                addDeltas(new File(args[0]), new File(args[1]), new File(args[2]));
            } else {
                recoverFromDelta(new File(args[0]), new File(args[1]), new File(args[2]), new Rd5DiffTool() /*, new File( args[3] ) */);
            }
        } else {
            diff2files(new File(args[0]), new File(args[1]), new File(args[2]));
        }
    }

    private static long[] readFileIndex(DataInputStream dis, DataOutputStream dos) throws Exception {
        long[] fileIndex = new long[25];
        for (int i = 0; i < 25; i++) {
            long lv = dis.readLong();
            fileIndex[i] = lv & 0xffffffffffffL;
            if (dos != null) {
                dos.writeLong(lv);
            }
        }
        return fileIndex;
    }

    private static long getTileStart(long[] index, int tileIndex) {
        return tileIndex > 0 ? index[tileIndex - 1] : 200L;
    }

    private static long getTileEnd(long[] index, int tileIndex) {
        return index[tileIndex];
    }

    private static int[] readPosIndex(DataInputStream dis, DataOutputStream dos) throws Exception {
        int[] posIndex = new int[1024];
        for (int i = 0; i < 1024; i++) {
            int iv = dis.readInt();
            posIndex[i] = iv;
            if (dos != null) {
                dos.writeInt(iv);
            }
        }
        return posIndex;
    }

    private static int getPosIdx(int[] posIdx, int idx) {
        return idx == -1 ? 4096 : posIdx[idx];
    }

    private static byte[] createMicroCache(int[] posIdx, int tileIdx, DataInputStream dis, boolean deltaMode) throws Exception {
        if (posIdx == null) {
            return null;
        }
        int size = getPosIdx(posIdx, tileIdx) - getPosIdx(posIdx, tileIdx - 1);
        if (size == 0) {
            return null;
        }
        if (deltaMode) {
            size = dis.readInt();
        }
        byte[] ab = new byte[size];
        dis.readFully(ab);
        return ab;
    }

    private static MicroCache createMicroCache(byte[] ab, DataBuffers dataBuffers) throws Exception {
        if (ab == null || ab.length == 0) {
            return MicroCache.emptyCache();
        }
        StatCoderContext bc = new StatCoderContext(ab);
        return new MicroCache2(bc, dataBuffers, 0, 0, 32, null, null);
    }

    /**
     * Compute the delta between 2 RD5 files and
     * show statistics on the expected size of the delta file
     */
    public static void diff2files(File f1, File f2, File outFile) throws Exception {
        byte[] abBuf1 = new byte[10 * 1024 * 1024];
        byte[] abBuf2 = new byte[10 * 1024 * 1024];

        int nodesDiff = 0;
        int diffedTiles = 0;

        long bytesDiff = 0L;

        DataInputStream dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        DataInputStream dis2 = new DataInputStream(new BufferedInputStream(new FileInputStream(f2)));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
        MCOutputStream mcOut = new MCOutputStream(dos, abBuf1);

        // copy header to outfile
        long[] fileIndex1 = readFileIndex(dis1, null);
        long[] fileIndex2 = readFileIndex(dis2, dos);

        long t0 = System.currentTimeMillis();

        try {
            DataBuffers dataBuffers = new DataBuffers();
            for (int subFileIdx = 0; subFileIdx < 25; subFileIdx++) {
                boolean hasData1 = getTileStart(fileIndex1, subFileIdx) < getTileEnd(fileIndex1, subFileIdx);
                boolean hasData2 = getTileStart(fileIndex2, subFileIdx) < getTileEnd(fileIndex2, subFileIdx);

                int[] posIdx1 = hasData1 ? readPosIndex(dis1, null) : null;
                int[] posIdx2 = hasData2 ? readPosIndex(dis2, dos) : null;

                for (int tileIdx = 0; tileIdx < 1024; tileIdx++) {
                    byte[] ab1 = createMicroCache(posIdx1, tileIdx, dis1, false);
                    byte[] ab2 = createMicroCache(posIdx2, tileIdx, dis2, false);

                    MicroCache mc;
                    if (Arrays.equals(ab1, ab2)) {
                        mc = MicroCache.emptyCache(); // empty diff
                    } else // calc diff of the 2 tiles
                    {
                        MicroCache mc1 = createMicroCache(ab1, dataBuffers);
                        MicroCache mc2 = createMicroCache(ab2, dataBuffers);
                        mc = new MicroCache2(mc1.getSize() + mc2.getSize(), abBuf2, 0, 0, 32);
                        mc.calcDelta(mc1, mc2);
                    }

                    int len = mcOut.writeMC(mc);
                    if (len > 0) {
                        bytesDiff += len;
                        nodesDiff += mc.getSize();
                        diffedTiles++;

/*                 // do some consistemcy checks on the encoding

                 byte[] bytes = new byte[len];
                 System.arraycopy( abBuf1, 0, bytes, 0, len );

                 // cross-check the encoding: decode again
                 MicroCache mcCheck = new MicroCache2( new StatCoderContext( bytes ), new DataBuffers( null ), 0, 0, 32, null, null );

                 // due to link-order ambiguity, for decoded we can only compare node-count and datasize
                 if ( mc.size() != mcCheck.size() )
                 {
                   throw new IllegalArgumentException( "re-decoded data-size mismatch!" );
                 }
                 if ( mc.getSize() != mcCheck.getSize() )
                 {
                   throw new IllegalArgumentException( "re-decoded node-count mismatch!" );
                 }

                 // .... so re-encode again
                 int len2 = mcCheck.encodeMicroCache( abBuf1 );
                 byte[] bytes2 = new byte[len2];
                 System.arraycopy( abBuf1, 0, bytes2, 0, len2 );

                 // and here we can compare byte-by-byte
                 if ( len != len2 )
                 {
                   throw new IllegalArgumentException( "decoded size mismatch!" );
                 }
                 for( int i=0; i<len; i++ )
                 {
                   if ( bytes[i] != bytes2[i] )
                   {
                     throw new IllegalArgumentException( "decoded data mismatch at i=" + i );
                   }
                 }
             */
                    }
                }
                mcOut.finish();
            }

            // write any remaining data to the output file
            for (; ; ) {
                int len = dis2.read(abBuf1);
                if (len < 0) {
                    break;
                }
                dos.write(abBuf1, 0, len);
            }
            long t1 = System.currentTimeMillis();
            System.out.println("nodesDiff=" + nodesDiff + " bytesDiff=" + bytesDiff + " diffedTiles=" + diffedTiles + " took " + (t1 - t0) + "ms");
        } finally {
            if (dis1 != null) {
                try {
                    dis1.close();
                } catch (Exception ee) {
                }
            }
            if (dis2 != null) {
                try {
                    dis2.close();
                } catch (Exception ee) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception ee) {
                }
            }
        }
    }

    public static void recoverFromDelta(File f1, File f2, File outFile, ProgressListener progress /* , File cmpFile */) throws Exception {
        if (f2.length() == 0L) {
            copyFile(f1, outFile, progress);
            return;
        }

        byte[] abBuf1 = new byte[10 * 1024 * 1024];
        byte[] abBuf2 = new byte[10 * 1024 * 1024];

        boolean canceled = false;

        long t0 = System.currentTimeMillis();

        DataInputStream dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        DataInputStream dis2 = new DataInputStream(new BufferedInputStream(new FileInputStream(f2)));
//    DataInputStream disCmp = new DataInputStream( new BufferedInputStream( new FileInputStream( cmpFile ) ) );
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

        // copy header to outfile
        long[] fileIndex1 = readFileIndex(dis1, null);
        long[] fileIndex2 = readFileIndex(dis2, dos);
//    long[] fileIndexCmp = readFileIndex( disCmp, null );

        int lastPct = -1;

        try {
            DataBuffers dataBuffers = new DataBuffers();
            MCInputStream mcIn = new MCInputStream(dis2, dataBuffers);

            for (int subFileIdx = 0; subFileIdx < 25; subFileIdx++) {
                boolean hasData1 = getTileStart(fileIndex1, subFileIdx) < getTileEnd(fileIndex1, subFileIdx); // has the basefile data
                boolean hasData2 = getTileStart(fileIndex2, subFileIdx) < getTileEnd(fileIndex2, subFileIdx); // has the *result* data

//         boolean hasDataCmp = getTileStart( fileIndexCmp, subFileIdx ) < getTileEnd( fileIndexCmp, subFileIdx );

                int[] posIdx1 = hasData1 ? readPosIndex(dis1, null) : null;
                int[] posIdx2 = hasData2 ? readPosIndex(dis2, dos) : null;

                //        int[] posIdxCmp = hasDataCmp ? readPosIndex( disCmp, null ) : null;

                for (int tileIdx = 0; tileIdx < 1024; tileIdx++) {
                    if (progress.isCanceled()) {
                        canceled = true;
                        return;
                    }
                    double bytesProcessed = getTileStart(fileIndex1, subFileIdx) + (posIdx1 == null ? 0 : getPosIdx(posIdx1, tileIdx - 1));
                    int pct = (int) (100. * bytesProcessed / getTileEnd(fileIndex1, 24) + 0.5);
                    if (pct != lastPct) {
                        progress.updateProgress("Applying delta: " + pct + "%");
                        lastPct = pct;
                    }

                    byte[] ab1 = createMicroCache(posIdx1, tileIdx, dis1, false);
                    MicroCache mc2 = mcIn.readMC();
                    int targetSize = posIdx2 == null ? 0 : getPosIdx(posIdx2, tileIdx) - getPosIdx(posIdx2, tileIdx - 1);

/*         int targetSizeCmp = getPosIdx( posIdxCmp, tileIdx ) - getPosIdx( posIdxCmp, tileIdx-1 );
           if ( targetSizeCmp != targetSize ) throw new IllegalArgumentException( "target size mismatch: "+ targetSize + "," + targetSizeCmp );
           byte[] abCmp = new byte[targetSizeCmp];
           disCmp.readFully( abCmp );
*/

                    // no-delta shortcut: just copy base data
                    if (mc2.getSize() == 0) {
                        if (ab1 != null) {
                            dos.write(ab1);
                        }
                        int newTargetSize = ab1 == null ? 0 : ab1.length;
                        if (targetSize != newTargetSize) {
                            throw new RuntimeException("size mismatch at " + subFileIdx + "/" + tileIdx + " " + targetSize + "!=" + newTargetSize);
                        }
                        continue;
                    }

                    // this is the real delta case (using decode->delta->encode )

                    MicroCache mc1 = createMicroCache(ab1, dataBuffers);

                    MicroCache mc = new MicroCache2(mc1.getSize() + mc2.getSize(), abBuf2, 0, 0, 32);
                    mc.addDelta(mc1, mc2, false);

                    if (mc.size() == 0) {
                        if (targetSize != 0) {
                            throw new RuntimeException("size mismatch at " + subFileIdx + "/" + tileIdx + " " + targetSize + ">0");
                        }
                        continue;
                    }

                    int len = mc.encodeMicroCache(abBuf1);

/*           System.out.println( "comparing for subFileIdx=" + subFileIdx + " tileIdx=" + tileIdx );
           boolean isequal = true;
           for( int i=0; i<len;i++ )
           {
             if ( isequal && abCmp[i] != abBuf1[i] )
             {
               System.out.println( "data mismatch at i=" + i + " " + abCmp[i] + "!=" + abBuf1[i]  + " targetSize=" + targetSize );
               isequal = false;

               MicroCache.debug = true;
               System.out.println( "**** decoding original cache ****" );
               createMicroCache( abCmp, dataBuffers );
               System.out.println( "**** decoding reconstructed cache ****" );
               createMicroCache( abBuf1, dataBuffers );
               System.exit(1);
             }
           }
*/
                    dos.write(abBuf1, 0, len);
                    dos.writeInt(Crc32.crc(abBuf1, 0, len) ^ 2);
                    if (targetSize != len + 4) {
                        throw new RuntimeException("size mismatch at " + subFileIdx + "/" + tileIdx + " " + targetSize + "<>" + (len + 4));
                    }

                }
                mcIn.finish();
            }
            // write any remaining data to the output file
            for (; ; ) {
                int len = dis2.read(abBuf1);
                if (len < 0) {
                    break;
                }
                dos.write(abBuf1, 0, len);
            }
            long t1 = System.currentTimeMillis();
            System.out.println("recovering from diffs took " + (t1 - t0) + "ms");
        } finally {
            if (dis1 != null) {
                try {
                    dis1.close();
                } catch (Exception ee) {
                }
            }
            if (dis2 != null) {
                try {
                    dis2.close();
                } catch (Exception ee) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception ee) {
                }
                if (canceled) {
                    outFile.delete();
                }
            }
        }
    }

    public static void copyFile(File f1, File outFile, ProgressListener progress) throws Exception {
        boolean canceled = false;
        DataInputStream dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
        int lastPct = -1;
        long sizeTotal = f1.length();
        long sizeRead = 0L;
        try {
            byte[] buf = new byte[65536];
            for (; ; ) {
                if (progress.isCanceled()) {
                    canceled = true;
                    return;
                }
                int pct = (int) ((100. * sizeRead) / (sizeTotal + 1) + 0.5);
                if (pct != lastPct) {
                    progress.updateProgress("Copying: " + pct + "%");
                    lastPct = pct;
                }
                int len = dis1.read(buf);
                if (len <= 0) {
                    break;
                }
                sizeRead += len;
                dos.write(buf, 0, len);
            }
        } finally {
            if (dis1 != null) {
                try {
                    dis1.close();
                } catch (Exception ee) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception ee) {
                }
                if (canceled) {
                    outFile.delete();
                }
            }
        }
    }

    public static void addDeltas(File f1, File f2, File outFile) throws Exception {
        byte[] abBuf1 = new byte[10 * 1024 * 1024];
        byte[] abBuf2 = new byte[10 * 1024 * 1024];

        DataInputStream dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        DataInputStream dis2 = new DataInputStream(new BufferedInputStream(new FileInputStream(f2)));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

        // copy subfile-header to outfile
        long[] fileIndex1 = readFileIndex(dis1, null);
        long[] fileIndex2 = readFileIndex(dis2, dos);

        long t0 = System.currentTimeMillis();

        try {
            DataBuffers dataBuffers = new DataBuffers();
            MCInputStream mcIn1 = new MCInputStream(dis1, dataBuffers);
            MCInputStream mcIn2 = new MCInputStream(dis2, dataBuffers);
            MCOutputStream mcOut = new MCOutputStream(dos, abBuf1);

            for (int subFileIdx = 0; subFileIdx < 25; subFileIdx++) {
                // copy tile-header to outfile
                boolean hasData1 = getTileStart(fileIndex1, subFileIdx) < getTileEnd(fileIndex1, subFileIdx);
                boolean hasData2 = getTileStart(fileIndex2, subFileIdx) < getTileEnd(fileIndex2, subFileIdx);
                int[] posIdx1 = hasData1 ? readPosIndex(dis1, null) : null;
                int[] posIdx2 = hasData2 ? readPosIndex(dis2, dos) : null;

                for (int tileIdx = 0; tileIdx < 1024; tileIdx++) {
                    MicroCache mc1 = mcIn1.readMC();
                    MicroCache mc2 = mcIn2.readMC();
                    MicroCache mc;
                    if (mc1.getSize() == 0 && mc2.getSize() == 0) {
                        mc = mc1;
                    } else {
                        mc = new MicroCache2(mc1.getSize() + mc2.getSize(), abBuf2, 0, 0, 32);
                        mc.addDelta(mc1, mc2, true);
                    }
                    mcOut.writeMC(mc);
                }
                mcIn1.finish();
                mcIn2.finish();
                mcOut.finish();
            }
            // write any remaining data to the output file
            for (; ; ) {
                int len = dis2.read(abBuf1);
                if (len < 0) {
                    break;
                }
                dos.write(abBuf1, 0, len);
            }
            long t1 = System.currentTimeMillis();
            System.out.println("adding diffs took " + (t1 - t0) + "ms");
        } finally {
            if (dis1 != null) {
                try {
                    dis1.close();
                } catch (Exception ee) {
                }
            }
            if (dis2 != null) {
                try {
                    dis2.close();
                } catch (Exception ee) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception ee) {
                }
            }
        }
    }

    public static void reEncode(File f1, File outFile) throws Exception {
        byte[] abBuf1 = new byte[10 * 1024 * 1024];

        DataInputStream dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(f1)));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));

        // copy header to outfile
        long[] fileIndex1 = readFileIndex(dis1, dos);

        long t0 = System.currentTimeMillis();

        try {
            DataBuffers dataBuffers = new DataBuffers();
            for (int subFileIdx = 0; subFileIdx < 25; subFileIdx++) {
                boolean hasData1 = getTileStart(fileIndex1, subFileIdx) < getTileEnd(fileIndex1, subFileIdx);

                int[] posIdx1 = hasData1 ? readPosIndex(dis1, dos) : null;

                for (int tileIdx = 0; tileIdx < 1024; tileIdx++) {
                    byte[] ab1 = createMicroCache(posIdx1, tileIdx, dis1, false);

                    if (ab1 == null)
                        continue;

                    MicroCache mc1 = createMicroCache(ab1, dataBuffers);

                    int len = mc1.encodeMicroCache(abBuf1);

                    dos.write(abBuf1, 0, len);
                    dos.writeInt(Crc32.crc(abBuf1, 0, len) ^ 2);
                }
            }
            // write any remaining data to the output file
            for (; ; ) {
                int len = dis1.read(abBuf1);
                if (len < 0) {
                    break;
                }
                dos.write(abBuf1, 0, len);
            }
            long t1 = System.currentTimeMillis();
            System.out.println("re-encoding took " + (t1 - t0) + "ms");
        } finally {
            if (dis1 != null) {
                try {
                    dis1.close();
                } catch (Exception ee) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception ee) {
                }
            }
        }
    }

    @Override
    public void updateProgress(String progress) {
        System.out.println(progress);
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    private static class MCOutputStream {
        private final DataOutputStream dos;
        private final byte[] buffer;
        private short skips = 0;

        public MCOutputStream(DataOutputStream dos, byte[] buffer) {
            this.dos = dos;
            this.buffer = buffer;
        }

        public int writeMC(MicroCache mc) throws Exception {
            if (mc.getSize() == 0) {
                skips++;
                return 0;
            }
            dos.writeShort(skips);
            skips = 0;
            int len = mc.encodeMicroCache(buffer);
            if (len == 0) {
                throw new IllegalArgumentException("encoded buffer of non-empty micro-cache cannot be empty");
            }
            dos.writeInt(len);
            dos.write(buffer, 0, len);
            return len;
        }

        public void finish() throws Exception {
            if (skips > 0) {
                dos.writeShort(skips);
                skips = 0;
            }
        }
    }

    private static class MCInputStream {
        private short skips = -1;
        private final DataInputStream dis;
        private final DataBuffers dataBuffers;
        private final MicroCache empty = MicroCache.emptyCache();

        public MCInputStream(DataInputStream dis, DataBuffers dataBuffers) {
            this.dis = dis;
            this.dataBuffers = dataBuffers;
        }

        public MicroCache readMC() throws Exception {
            if (skips < 0) {
                skips = dis.readShort();
            }
            MicroCache mc = empty;
            if (skips == 0) {
                int size = dis.readInt();
                byte[] ab = new byte[size];
                dis.readFully(ab);
                StatCoderContext bc = new StatCoderContext(ab);
                mc = new MicroCache2(bc, dataBuffers, 0, 0, 32, null, null);
            }
            skips--;
            return mc;
        }

        public void finish() throws Exception {
            skips = -1;
        }
    }

}
