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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.R
import cgeo.geocaching.connector.capability.Smiley

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Arrays
import java.util.Collections
import java.util.List

class GCSmileysProvider {

    private static val SMILEYS: List<Smiley> = Collections.unmodifiableList(Arrays.asList(
            Smiley(":)", R.string.smiley_smile, "\uD83D\uDE03"),
            Smiley(":D", R.string.smiley_big_smile, "\uD83D\uDE01"),
            Smiley("8D", R.string.smiley_cool, "\uD83D\uDE0E"),
            Smiley(":I", R.string.smiley_blush, "\uD83D\uDE33"),
            Smiley(":P", R.string.smiley_tongue, "\uD83D\uDE1D"),
            Smiley("}:)", R.string.smiley_evil, "\uD83D\uDE08"),
            Smiley(";)", R.string.smiley_wink, "\uD83D\uDE09"),
            Smiley(":o)", R.string.smiley_clown, "\uD83E\uDD21"),
            Smiley("B)", R.string.smiley_blackeye, "\uD83E\uDD74"),
            Smiley("8", R.string.smiley_8ball, "\uD83C\uDFB1"),
            Smiley(":(", R.string.smiley_frown, "☹️️"),
            Smiley("8)", R.string.smiley_shy, "\uD83E\uDD7A"),
            Smiley(":O", R.string.smiley_shocked, "\uD83D\uDE28"),
            Smiley(":(!", R.string.smiley_angry, "\uD83D\uDE21"),
            Smiley("xx(", R.string.smiley_dead, "\uD83D\uDC80"),
            Smiley("|)", R.string.smiley_sleepy, "\uD83D\uDE2A"),
            Smiley(":X", R.string.smiley_kisses, "\uD83D\uDE18"),
            Smiley("^", R.string.smiley_approve, "\uD83D\uDC4C"),
            Smiley("V", R.string.smiley_disapprove, "\uD83D\uDE12"),
            Smiley("?", R.string.smiley_question, "❓")
    ))

    private GCSmileysProvider() {
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
