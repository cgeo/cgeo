package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.util.LruMapNode;

import java.util.Arrays;

public final class CacheNode extends LruMapNode {
    public byte[] ab;
    public float[] vars;

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object o) {
        final CacheNode n = (CacheNode) o;
        if (hash != n.hash) {
            return false;
        }
        if (ab == null) {
            return true; // hack: null = crc match only
        }
        return Arrays.equals(ab, n.ab);
    }
}
