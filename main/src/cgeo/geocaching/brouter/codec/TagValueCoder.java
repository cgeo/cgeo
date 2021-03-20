package cgeo.geocaching.brouter.codec;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

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
    private HashMap<TagValueSet, TagValueSet> identityMap;
    private Object tree;
    private BitCoderContext bc;
    private int pass;
    private int nextTagValueSetId;

    public TagValueCoder(BitCoderContext bc, DataBuffers buffers, TagValueValidator validator) {
        tree = decodeTree(bc, buffers, validator);
        this.bc = bc;
    }

    public TagValueCoder() {
        identityMap = new HashMap<TagValueSet, TagValueSet>();
    }

    public void encodeTagValueSet(byte[] data) {
        if (pass == 1) {
            return;
        }
        TagValueSet tvsProbe = new TagValueSet(nextTagValueSetId);
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
            TreeNode tn = (TreeNode) node;
            boolean nextBit = bc.decodeBit();
            node = nextBit ? tn.child2 : tn.child1;
        }
        return (TagValueWrapper) node;
    }

    public void encodeDictionary(BitCoderContext bc) {
        if (++pass == 3) {
            if (identityMap.size() == 0) {
                TagValueSet dummy = new TagValueSet(nextTagValueSetId++);
                identityMap.put(dummy, dummy);
            }
            PriorityQueue<TagValueSet> queue = new PriorityQueue<TagValueSet>(2 * identityMap.size(), new TagValueSet.FrequencyComparator());
            queue.addAll(identityMap.values());
            while (queue.size() > 1) {
                TagValueSet node = new TagValueSet(nextTagValueSetId++);
                node.child1 = queue.poll();
                node.child2 = queue.poll();
                node.frequency = node.child1.frequency + node.child2.frequency;
                queue.add(node);
            }
            TagValueSet root = queue.poll();
            root.encode(bc, 1, 0);
        }
        this.bc = bc;
    }

    private Object decodeTree(BitCoderContext bc, DataBuffers buffers, TagValueValidator validator) {
        boolean isNode = bc.decodeBit();
        if (isNode) {
            TreeNode node = new TreeNode();
            node.child1 = decodeTree(bc, buffers, validator);
            node.child2 = decodeTree(bc, buffers, validator);
            return node;
        }

        byte[] buffer = buffers.tagbuf1;
        BitCoderContext ctx = buffers.bctx1;
        ctx.reset(buffer);

        int inum = 0;
        int lastEncodedInum = 0;

        boolean hasdata = false;
        for (; ; ) {
            int delta = bc.decodeVarBits();
            if (!hasdata) {
                if (delta == 0) {
                    return null;
                }
            }
            if (delta == 0) {
                ctx.encodeVarBits(0);
                break;
            }
            inum += delta;

            int data = bc.decodeVarBits();

            if (validator == null || validator.isLookupIdxUsed(inum)) {
                hasdata = true;
                ctx.encodeVarBits(inum - lastEncodedInum);
                ctx.encodeVarBits(data);
                lastEncodedInum = inum;
            }
        }

        byte[] res;
        int len = ctx.closeAndGetEncodedLength();
        if (validator == null) {
            res = new byte[len];
            System.arraycopy(buffer, 0, res, 0, len);
        } else {
            res = validator.unify(buffer, 0, len);
        }

        int accessType = validator == null ? 2 : validator.accessType(res);
        if (accessType > 0) {
            TagValueWrapper w = new TagValueWrapper();
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

        public TagValueSet(int id) {
            this.id = id;
        }

        public void encode(BitCoderContext bc, int range, int code) {
            this.range = range;
            this.code = code;
            boolean isNode = child1 != null;
            bc.encodeBit(isNode);
            if (isNode) {
                child1.encode(bc, range << 1, code);
                child2.encode(bc, range << 1, code + range);
            } else {
                if (data == null) {
                    bc.encodeVarBits(0);
                    return;
                }
                BitCoderContext src = new BitCoderContext(data);
                for (; ; ) {
                    int delta = src.decodeVarBits();
                    bc.encodeVarBits(delta);
                    if (delta == 0) {
                        break;
                    }
                    int data = src.decodeVarBits();
                    bc.encodeVarBits(data);
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TagValueSet) {
                TagValueSet tvs = (TagValueSet) o;
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
            public int compare(TagValueSet tvs1, TagValueSet tvs2) {
                if (tvs1.frequency < tvs2.frequency)
                    return -1;
                if (tvs1.frequency > tvs2.frequency)
                    return 1;

                // to avoid ordering instability, decide on the id if frequency is equal
                if (tvs1.id < tvs2.id)
                    return -1;
                if (tvs1.id > tvs2.id)
                    return 1;

                if (tvs1 != tvs2) {
                    throw new RuntimeException("identity corruption!");
                }
                return 0;
            }
        }

    }
}
