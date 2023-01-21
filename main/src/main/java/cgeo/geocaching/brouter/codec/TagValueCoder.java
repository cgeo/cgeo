package cgeo.geocaching.brouter.codec;

import cgeo.geocaching.brouter.util.BitCoderContext;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

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
    private HashMap<TagValueSet, TagValueSet> identityMap;
    private Object tree;
    private BitCoderContext bc;
    private int pass;
    private int nextTagValueSetId;

    public TagValueCoder(final BitCoderContext bc, final DataBuffers buffers, final TagValueValidator validator) {
        tree = decodeTree(bc, buffers, validator);
        this.bc = bc;
    }

    public TagValueCoder() {
        identityMap = new HashMap<>();
    }

    public void encodeTagValueSet(final byte[] data) {
        if (pass == 1) {
            return;
        }
        final TagValueSet tvsProbe = new TagValueSet(nextTagValueSetId);
        tvsProbe.data = data;
        TagValueSet tvs = identityMap.get(tvsProbe);
        if (pass == 3) {
            bc.encodeBounded(tvs.range - 1, tvs.code);
        } else if (pass == 2) {
            if (tvs == null) {
                tvs = tvsProbe;
                nextTagValueSetId++;
                identityMap.put(tvs, tvs);
            }
            tvs.frequency++;
        }
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

    public void encodeDictionary(final BitCoderContext bc) {
        if (++pass == 3) {
            if (identityMap.size() == 0) {
                final TagValueSet dummy = new TagValueSet(nextTagValueSetId++);
                identityMap.put(dummy, dummy);
            }
            final PriorityQueue<TagValueSet> queue = new PriorityQueue<>(2 * identityMap.size(), new TagValueSet.FrequencyComparator());
            queue.addAll(identityMap.values());
            while (queue.size() > 1) {
                final TagValueSet node = new TagValueSet(nextTagValueSetId++);
                node.child1 = queue.poll();
                node.child2 = queue.poll();
                node.frequency = node.child1.frequency + node.child2.frequency;
                queue.add(node);
            }
            final TagValueSet root = queue.poll();
            root.encode(bc, 1, 0);
        }
        this.bc = bc;
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

    public static final class TagValueSet {
        public byte[] data;
        public int frequency;
        public int code;
        public int range;
        public TagValueSet child1;
        public TagValueSet child2;
        private final int id; // serial number to make the comparator well defined in case of equal frequencies

        public TagValueSet(final int id) {
            this.id = id;
        }

        public void encode(final BitCoderContext bc, final int range, final int code) {
            this.range = range;
            this.code = code;
            final boolean isNode = child1 != null;
            bc.encodeBit(isNode);
            if (isNode) {
                child1.encode(bc, range << 1, code);
                child2.encode(bc, range << 1, code + range);
            } else {
                if (data == null) {
                    bc.encodeVarBits(0);
                    return;
                }
                final BitCoderContext src = new BitCoderContext(data);
                for (; ; ) {
                    final int delta = src.decodeVarBits();
                    bc.encodeVarBits(delta);
                    if (delta == 0) {
                        break;
                    }
                    final int data = src.decodeVarBits();
                    bc.encodeVarBits(data);
                }
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (o instanceof TagValueSet) {
                final TagValueSet tvs = (TagValueSet) o;
                if (data == null) {
                    return tvs.data == null;
                }
                if (tvs.data == null) {
                    return data == null;
                }
                if (data.length != tvs.data.length) {
                    return false;
                }
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != tvs.data[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (data == null) {
                return 0;
            }
            int h = 17;
            for (int i = 0; i < data.length; i++) {
                h = (h << 8) + data[i];
            }
            return h;
        }

        public static class FrequencyComparator implements Comparator<TagValueSet> {

            @Override
            public int compare(final TagValueSet tvs1, final TagValueSet tvs2) {
                if (tvs1.frequency < tvs2.frequency) {
                    return -1;
                }
                if (tvs1.frequency > tvs2.frequency) {
                    return 1;
                }

                // to avoid ordering instability, decide on the id if frequency is equal
                if (tvs1.id < tvs2.id) {
                    return -1;
                }
                if (tvs1.id > tvs2.id) {
                    return 1;
                }

                if (tvs1 != tvs2) {
                    throw new RuntimeException("identity corruption!");
                }
                return 0;
            }
        }

    }
}
