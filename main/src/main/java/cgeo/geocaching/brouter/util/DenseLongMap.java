package cgeo.geocaching.brouter.util;

import java.util.ArrayList;

/**
 * Special Memory efficient Map to map a long-key to
 * a "small" value (some bits only) where it is expected
 * that the keys are dense, so that we can use more or less
 * a simple array as the best-fit data model (except for
 * the 32-bit limit of arrays!)
 * <p>
 * Target application are osm-node ids which are in the
 * range 0...3 billion and basically dense (=only few
 * nodes deleted)
 *
 * @author ab
 */
public class DenseLongMap {
    private ArrayList<byte[]> blocklist = new ArrayList<>(4096);

    private int blocksize; // bytes per bitplane in one block
    private int blocksizeBits;
    private long blocksizeBitsMask;
    private int maxvalue = 254; // fixed due to 8 bit lookup table
    private int[] bitplaneCount = new int[8];
    private long putCount = 0L;
    private long getCount = 0L;

    /**
     * Creates a DenseLongMap for the default block size
     * ( 512 bytes per bitplane, covering a key range of 4096 keys )
     * Note that one value range is limited to 0..254
     */
    public DenseLongMap() {
        this(512);
    }

    /**
     * Creates a DenseLongMap for the given block size
     *
     * @param blocksize bytes per bit-plane
     */
    public DenseLongMap(final int blocksize) {
        int bits = 4;
        while (bits < 28 && (1 << bits) != blocksize) {
            bits++;
        }
        if (bits == 28) {
            throw new RuntimeException("not a valid blocksize: " + blocksize + " ( expected 1 << bits with bits in (4..27) )");
        }
        blocksizeBits = bits + 3;
        blocksizeBitsMask = (1L << blocksizeBits) - 1;
        this.blocksize = blocksize;
    }


    public void put(final long key, final int value) {
        putCount++;

        if (value < 0 || value > maxvalue) {
            throw new IllegalArgumentException("value out of range (0.." + maxvalue + "): " + value);
        }

        final int blockn = (int) (key >> blocksizeBits);
        final int offset = (int) (key & blocksizeBitsMask);

        byte[] block = blockn < blocklist.size() ? blocklist.get(blockn) : null;

        int valuebits = 1;
        if (block == null) {
            block = new byte[sizeForBits(valuebits)];
            bitplaneCount[0]++;

            while (blocklist.size() < blockn + 1) {
                blocklist.add(null);
            }
            blocklist.set(blockn, block);
        } else {
            // check how many bitplanes we have from the arraysize
            while (sizeForBits(valuebits) < block.length) {
                valuebits++;
            }
        }
        int headersize = 1 << valuebits;

        final byte v = (byte) (value + 1); // 0 is reserved (=unset)

        // find the index in the lookup table or the first entry
        int idx = 1;
        while (idx < headersize) {
            if (block[idx] == 0) {
                block[idx] = v; // create new entry
            }
            if (block[idx] == v) {
                break;
            }
            idx++;
        }
        if (idx == headersize) {
            block = expandBlock(block, valuebits);
            block[idx] = v; // create new entry
            blocklist.set(blockn, block);
            valuebits++;
            headersize = 1 << valuebits;
        }

        final int bitmask = 1 << (offset & 0x7);
        final int invmask = bitmask ^ 0xff;
        int probebit = 1;
        int blockidx = (offset >> 3) + headersize;

        for (int i = 0; i < valuebits; i++) {
            if ((idx & probebit) != 0) {
                block[blockidx] |= bitmask;
            } else {
                block[blockidx] &= invmask;
            }
            probebit <<= 1;
            blockidx += blocksize;
        }
    }


    private int sizeForBits(final int bits) {
        // size is lookup table + datablocks
        return (1 << bits) + blocksize * bits;
    }

    private byte[] expandBlock(final byte[] block, final int valuebits) {
        bitplaneCount[valuebits]++;
        final byte[] newblock = new byte[sizeForBits(valuebits + 1)];
        final int headersize = 1 << valuebits;
        System.arraycopy(block, 0, newblock, 0, headersize); // copy header
        System.arraycopy(block, headersize, newblock, 2 * headersize, block.length - headersize); // copy data
        return newblock;
    }

    public int getInt(final long key) {
        // bit-stats on first get
        if (getCount++ == 0L) {
            System.out.println("**** DenseLongMap stats ****");
            System.out.println("putCount=" + putCount);
            for (int i = 0; i < 8; i++) {
                System.out.println(i + "-bitplanes=" + bitplaneCount[i]);
            }
            System.out.println("****************************");
        }

    /* actual stats for the 30x45 raster and 512 blocksize with filtered nodes:
     *
     **** DenseLongMap stats ****
     putCount=858518399
     0-bitplanes=783337
     1-bitplanes=771490
     2-bitplanes=644578
     3-bitplanes=210767
     4-bitplanes=439
     5-bitplanes=0
     6-bitplanes=0
     7-bitplanes=0
     *
     * This is a total of 1,2 GB
     * (1.234.232.832+7.381.126+15.666.740 for body/header/object-overhead )
    */

        if (key < 0) {
            return -1;
        }
        final int blockn = (int) (key >> blocksizeBits);
        final int offset = (int) (key & blocksizeBitsMask);

        final byte[] block = blockn < blocklist.size() ? blocklist.get(blockn) : null;

        if (block == null) {
            return -1;
        }

        // check how many bitplanes we have from the arrayzize
        int valuebits = 1;
        while (sizeForBits(valuebits) < block.length) {
            valuebits++;
        }
        final int headersize = 1 << valuebits;

        final int bitmask = 1 << (offset & 7);
        int probebit = 1;
        int blockidx = (offset >> 3) + headersize;
        int idx = 0; // 0 is reserved (=unset)

        for (int i = 0; i < valuebits; i++) {
            if ((block[blockidx] & bitmask) != 0) {
                idx |= probebit;
            }
            probebit <<= 1;
            blockidx += blocksize;
        }

        // lookup that value in the lookup header
        return ((256 + block[idx]) & 0xff) - 1;
    }

}
