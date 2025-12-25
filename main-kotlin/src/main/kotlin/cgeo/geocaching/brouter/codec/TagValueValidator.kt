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


interface TagValueValidator {
    /**
     * @param tagValueSet the way description to check
     * @return 0 = nothing, 1=no matching, 2=normal
     */
    Int accessType(Byte[] tagValueSet)

    Byte[] unify(Byte[] tagValueSet, Int offset, Int len)

    Boolean isLookupIdxUsed(Int idx)

    Unit setDecodeForbidden(Boolean decodeForbidden)
}
