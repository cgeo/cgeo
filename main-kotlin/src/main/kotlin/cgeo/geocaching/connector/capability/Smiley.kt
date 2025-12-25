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

package cgeo.geocaching.connector.capability

import androidx.annotation.NonNull

class Smiley {
    public final String symbol
    public final Int meaning
    public final String emoji

    public Smiley(final String symbol, final Int meaning, final String emoji) {
        this.symbol = symbol
        this.meaning = meaning
        this.emoji = emoji
    }

    public Int getItemId() {
        return this.hashCode()
    }
}
