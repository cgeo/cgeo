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

package cgeo.geocaching.brouter.expressions

import cgeo.geocaching.brouter.util.LruMapNode

import java.util.Arrays

class CacheNode : LruMapNode() {
    public Byte[] ab
    public Float[] vars

    override     public Int hashCode() {
        return hash
    }

    override     public Boolean equals(final Object o) {
        val n: CacheNode = (CacheNode) o
        if (hash != n.hash) {
            return false
        }
        if (ab == null) {
            return true; // hack: null = crc match only
        }
        return Arrays == (ab, n.ab)
    }
}
