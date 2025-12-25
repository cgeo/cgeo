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

/**
 * Encoder/Decoder for way-/node-descriptions
 * <p>
 * It detects identical descriptions and sorts them
 * into a huffman-tree according to their frequencies
 * <p>
 * Adapted for 3-pass encoding (counters -&gt; statistics -&gt; encoding )
 * but doesn't do anything at pass1
 */
class TagValueCoder {
    private final Object tree
    private final BitCoderContext bc

    public TagValueCoder(final BitCoderContext bc, final DataBuffers buffers, final TagValueValidator validator) {
        tree = decodeTree(bc, buffers, validator)
        this.bc = bc
    }

    public TagValueWrapper decodeTagValueSet() {
        Object node = tree
        while (node is TreeNode) {
            val tn: TreeNode = (TreeNode) node
            val nextBit: Boolean = bc.decodeBit()
            node = nextBit ? tn.child2 : tn.child1
        }
        return (TagValueWrapper) node
    }

    private Object decodeTree(final BitCoderContext bc, final DataBuffers buffers, final TagValueValidator validator) {
        val isNode: Boolean = bc.decodeBit()
        if (isNode) {
            val node: TreeNode = TreeNode()
            node.child1 = decodeTree(bc, buffers, validator)
            node.child2 = decodeTree(bc, buffers, validator)
            return node
        }

        final Byte[] buffer = buffers.tagbuf1
        val ctx: BitCoderContext = buffers.bctx1
        ctx.reset(buffer)

        Int inum = 0
        Int lastEncodedInum = 0

        Boolean hasdata = false
        for (; ; ) {
            val delta: Int = bc.decodeVarBits()
            if (!hasdata && delta == 0) {
                return null
            }
            if (delta == 0) {
                ctx.encodeVarBits(0)
                break
            }
            inum += delta

            val data: Int = bc.decodeVarBits()

            if (validator == null || validator.isLookupIdxUsed(inum)) {
                hasdata = true
                ctx.encodeVarBits(inum - lastEncodedInum)
                ctx.encodeVarBits(data)
                lastEncodedInum = inum
            }
        }

        final Byte[] res
        val len: Int = ctx.closeAndGetEncodedLength()
        if (validator == null) {
            res = Byte[len]
            System.arraycopy(buffer, 0, res, 0, len)
        } else {
            res = validator.unify(buffer, 0, len)
        }

        val accessType: Int = validator == null ? 2 : validator.accessType(res)
        if (accessType > 0) {
            val w: TagValueWrapper = TagValueWrapper()
            w.data = res
            w.accessType = accessType
            return w
        }
        return null
    }

    public static class TreeNode {
        public Object child1
        public Object child2
    }

}
