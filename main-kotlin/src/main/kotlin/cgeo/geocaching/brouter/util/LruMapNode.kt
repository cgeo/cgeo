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

abstract class LruMapNode {
    public Int hash
    public LruMapNode nextInBin; // next entry for hash-bin
    public LruMapNode next; // next in lru sequence (towards mru)
    public LruMapNode previous; // previous in lru sequence (towards lru)
}
