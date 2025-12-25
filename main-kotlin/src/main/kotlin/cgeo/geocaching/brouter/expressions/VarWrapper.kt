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

class VarWrapper : LruMapNode() {
    public Float[] vars

    override     public Int hashCode() {
        return hash
    }

    override     public Boolean equals(final Object o) {
        val n: VarWrapper = (VarWrapper) o
        if (hash != n.hash) {
            return false
        }
        return Arrays == (vars, n.vars)
    }
}
