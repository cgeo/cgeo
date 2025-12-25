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

package cgeo.geocaching.connector.oc

import cgeo.geocaching.connector.capability.Smiley

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.List

class OCSmileysProvider {

    /**
     * deactivated until it is realized at OC(.de)
     * private static val SMILEYS: List<Smiley> = Collections.unmodifiableList(Arrays.asList(
     * Smiley("8)", R.string.smiley_cool, ""),
     * Smiley(":,-(", R.string.smiley_cry, ""),
     * Smiley("::|", R.string.smiley_embarassed, ""),
     * Smiley(":-!", R.string.smiley_footinmouth, ""),
     * Smiley(":(", R.string.smiley_frown, ""),
     * Smiley("O:)", R.string.smiley_innocent, ""),
     * Smiley(":-*", R.string.smiley_kiss, ""),
     * Smiley(":D", R.string.smiley_laughing, ""),
     * Smiley(":-($)", R.string.smiley_money, ""),
     * Smiley(":x", R.string.smiley_sealed, ""),
     * Smiley(":)", R.string.smiley_smile, ""),
     * Smiley(":o", R.string.smiley_surprised, ""),
     * Smiley(":P", R.string.smiley_tongueout, ""),
     * Smiley(":/", R.string.smiley_undecided, ""),
     * Smiley(";)", R.string.smiley_wink, ""),
     * Smiley("XO", R.string.smiley_yell, "")
     * ))
     */
    private static val SMILEYS: List<Smiley> = Collections.unmodifiableList(Collections.emptyList())

    private OCSmileysProvider() {
        // utility class
    }

    public static List<Smiley> getSmileys() {
        return SMILEYS
    }

    public static Smiley getSmiley(final Int id) {
        for (final Smiley smiley : SMILEYS) {
            if (smiley.getItemId() == id) {
                return smiley
            }
        }
        return null
    }
}
