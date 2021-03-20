package cgeo.geocaching.brouter.expressions;

import cgeo.geocaching.brouter.util.LruMapNode;

import java.util.Arrays;

public final class VarWrapper extends LruMapNode {
    public float[] vars;

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object o) {
        final VarWrapper n = (VarWrapper) o;
        if (hash != n.hash) {
            return false;
        }
        return Arrays.equals(vars, n.vars);
    }
}
