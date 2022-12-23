package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.capability.Smiley;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GCSmileysProvider {

    private static final List<Smiley> SMILEYS = Collections.unmodifiableList(Arrays.asList(
            new Smiley(":)", R.string.smiley_smile, "\uD83D\uDE03"),
            new Smiley(":D", R.string.smiley_big_smile, "\uD83D\uDE01"),
            new Smiley("8D", R.string.smiley_cool, "\uD83D\uDE0E"),
            new Smiley(":I", R.string.smiley_blush, "\uD83D\uDE33"),
            new Smiley(":P", R.string.smiley_tongue, "\uD83D\uDE1D"),
            new Smiley("}:)", R.string.smiley_evil, "\uD83D\uDE08"),
            new Smiley(";)", R.string.smiley_wink, "\uD83D\uDE09"),
            new Smiley(":o)", R.string.smiley_clown, "\uD83E\uDD21"),
            new Smiley("B)", R.string.smiley_blackeye, "\uD83E\uDD74"),
            new Smiley("8", R.string.smiley_8ball, "\uD83C\uDFB1"),
            new Smiley(":(", R.string.smiley_frown, "☹️️"),
            new Smiley("8)", R.string.smiley_shy, "\uD83E\uDD7A"),
            new Smiley(":O", R.string.smiley_shocked, "\uD83D\uDE28"),
            new Smiley(":(!", R.string.smiley_angry, "\uD83D\uDE21"),
            new Smiley("xx(", R.string.smiley_dead, "\uD83D\uDC80"),
            new Smiley("|)", R.string.smiley_sleepy, "\uD83D\uDE2A"),
            new Smiley(":X", R.string.smiley_kisses, "\uD83D\uDE18"),
            new Smiley("^", R.string.smiley_approve, "\uD83D\uDC4C"),
            new Smiley("V", R.string.smiley_disapprove, "\uD83D\uDE12"),
            new Smiley("?", R.string.smiley_question, "❓")
    ));

    private GCSmileysProvider() {
        // utility class
    }

    @NonNull
    public static List<Smiley> getSmileys() {
        return SMILEYS;
    }

    @Nullable
    public static Smiley getSmiley(final int id) {
        for (final Smiley smiley : SMILEYS) {
            if (smiley.getItemId() == id) {
                return smiley;
            }
        }
        return null;
    }
}
