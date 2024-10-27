package cgeo.geocaching.brouter.codec;

import cgeo.geocaching.brouter.util.BitCoderContext;

/**
 * Encoder/Decoder for way-/node-descriptions
 * <p>
 * It detects identical descriptions and sorts them
 * into a huffman-tree according to their frequencies
 * <p>
 * Adapted for 3-pass encoding (counters -&gt; statistics -&gt; encoding )
 * but doesn't do anything at pass1
 */
public final class TagValueCoder {
    private final Object tree;
    private final BitCoderContext bc;

    public TagValueCoder(final BitCoderContext bc, final DataBuffers buffers, final TagValueValidator validator) {
        tree = decodeTree(bc, buffers, validator);
        this.bc = bc;
    }

    public TagValueWrapper decodeTagValueSet() {
        Object node = tree;
        while (node instanceof TreeNode) {
            final TreeNode tn = (TreeNode) node;
            final boolean nextBit = bc.decodeBit();
            node = nextBit ? tn.child2 : tn.child1;
        }
        return (TagValueWrapper) node;
    }

    private Object decodeTree(final BitCoderContext bc, final DataBuffers buffers, final TagValueValidator validator) {
        final boolean isNode = bc.decodeBit();
        if (isNode) {
            final TreeNode node = new TreeNode();
            node.child1 = decodeTree(bc, buffers, validator);
            node.child2 = decodeTree(bc, buffers, validator);
            return node;
        }

        final byte[] buffer = buffers.tagbuf1;
        final BitCoderContext ctx = buffers.bctx1;
        ctx.reset(buffer);

        int inum = 0;
        int lastEncodedInum = 0;

        boolean hasdata = false;
        for (; ; ) {
            final int delta = bc.decodeVarBits();
            if (!hasdata && delta == 0) {
                return null;
            }
            if (delta == 0) {
                ctx.encodeVarBits(0);
                break;
            }
            inum += delta;

            final int data = bc.decodeVarBits();

            if (validator == null || validator.isLookupIdxUsed(inum)) {
                hasdata = true;
                ctx.encodeVarBits(inum - lastEncodedInum);
                ctx.encodeVarBits(data);
                lastEncodedInum = inum;
            }
        }

        final byte[] res;
        final int len = ctx.closeAndGetEncodedLength();
        if (validator == null) {
            res = new byte[len];
            System.arraycopy(buffer, 0, res, 0, len);
        } else {
            res = validator.unify(buffer, 0, len);
        }

        final int accessType = validator == null ? 2 : validator.accessType(res);
        if (accessType > 0) {
            final TagValueWrapper w = new TagValueWrapper();
            w.data = res;
            w.accessType = accessType;
            return w;
        }
        return null;
    }

    public static final class TreeNode {
        public Object child1;
        public Object child2;
    }

}
