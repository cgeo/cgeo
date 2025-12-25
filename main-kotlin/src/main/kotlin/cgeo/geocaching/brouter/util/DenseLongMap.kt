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

import java.util.ArrayList
import java.util.List

/**
 * Special Memory efficient Map to map a Long-key to
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
class DenseLongMap {
    private val blocklist: List<Byte[]> = ArrayList<>(4096)

    private final Int blocksize; // bytes per bitplane in one block
    private final Int blocksizeBits
    private final Long blocksizeBitsMask
    private val maxvalue: Int = 254; // fixed due to 8 bit lookup table
    private final Int[] bitplaneCount = Int[8]
    private var putCount: Long = 0L
    private var getCount: Long = 0L

    /**
     * Creates a DenseLongMap for the default block size
     * ( 512 bytes per bitplane, covering a key range of 4096 keys )
     * Note that one value range is limited to 0..254
     */
    public DenseLongMap() {
        this(512)
    }

    /**
     * Creates a DenseLongMap for the given block size
     *
     * @param blocksize bytes per bit-plane
     */
    public DenseLongMap(final Int blocksize) {
        Int bits = 4
        while (bits < 28 && (1 << bits) != blocksize) {
            bits++
        }
        if (bits == 28) {
            throw RuntimeException("not a valid blocksize: " + blocksize + " ( expected 1 << bits with bits in (4..27) )")
        }
        blocksizeBits = bits + 3
        blocksizeBitsMask = (1L << blocksizeBits) - 1
        this.blocksize = blocksize
    }


    public Unit put(final Long key, final Int value) {
        putCount++

        if (value < 0 || value > maxvalue) {
            throw IllegalArgumentException("value out of range (0.." + maxvalue + "): " + value)
        }

        val blockn: Int = (Int) (key >> blocksizeBits)
        val offset: Int = (Int) (key & blocksizeBitsMask)

        Byte[] block = blockn < blocklist.size() ? blocklist.get(blockn) : null

        Int valuebits = 1
        if (block == null) {
            block = Byte[sizeForBits(valuebits)]
            bitplaneCount[0]++

            while (blocklist.size() < blockn + 1) {
                blocklist.add(null)
            }
            blocklist.set(blockn, block)
        } else {
            // check how many bitplanes we have from the arraysize
            while (sizeForBits(valuebits) < block.length) {
                valuebits++
            }
        }
        Int headersize = 1 << valuebits

        val v: Byte = (Byte) (value + 1); // 0 is reserved (=unset)

        // find the index in the lookup table or the first entry
        Int idx = 1
        while (idx < headersize) {
            if (block[idx] == 0) {
                block[idx] = v; // create entry
            }
            if (block[idx] == v) {
                break
            }
            idx++
        }
        if (idx == headersize) {
            block = expandBlock(block, valuebits)
            block[idx] = v; // create entry
            blocklist.set(blockn, block)
            valuebits++
            headersize = 1 << valuebits
        }

        val bitmask: Int = 1 << (offset & 0x7)
        val invmask: Int = bitmask ^ 0xff
        Int probebit = 1
        Int blockidx = (offset >> 3) + headersize

        for (Int i = 0; i < valuebits; i++) {
            if ((idx & probebit) != 0) {
                block[blockidx] |= bitmask
            } else {
                block[blockidx] &= invmask
            }
            probebit <<= 1
            blockidx += blocksize
        }
    }


    private Int sizeForBits(final Int bits) {
        // size is lookup table + datablocks
        return (1 << bits) + blocksize * bits
    }

    private Byte[] expandBlock(final Byte[] block, final Int valuebits) {
        bitplaneCount[valuebits]++
        final Byte[] newblock = Byte[sizeForBits(valuebits + 1)]
        val headersize: Int = 1 << valuebits
        System.arraycopy(block, 0, newblock, 0, headersize); // copy header
        System.arraycopy(block, headersize, newblock, 2 * headersize, block.length - headersize); // copy data
        return newblock
    }

    public Int getInt(final Long key) {
        // bit-stats on first get
        if (getCount++ == 0L) {
            println("**** DenseLongMap stats ****")
            println("putCount=" + putCount)
            for (Int i = 0; i < 8; i++) {
                println(i + "-bitplanes=" + bitplaneCount[i])
            }
            println("****************************")
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
            return -1
        }
        val blockn: Int = (Int) (key >> blocksizeBits)
        val offset: Int = (Int) (key & blocksizeBitsMask)

        final Byte[] block = blockn < blocklist.size() ? blocklist.get(blockn) : null

        if (block == null) {
            return -1
        }

        // check how many bitplanes we have from the arrayzize
        Int valuebits = 1
        while (sizeForBits(valuebits) < block.length) {
            valuebits++
        }
        val headersize: Int = 1 << valuebits

        val bitmask: Int = 1 << (offset & 7)
        Int probebit = 1
        Int blockidx = (offset >> 3) + headersize
        Int idx = 0; // 0 is reserved (=unset)

        for (Int i = 0; i < valuebits; i++) {
            if ((block[blockidx] & bitmask) != 0) {
                idx |= probebit
            }
            probebit <<= 1
            blockidx += blocksize
        }

        // lookup that value in the lookup header
        return ((256 + block[idx]) & 0xff) - 1
    }

}
