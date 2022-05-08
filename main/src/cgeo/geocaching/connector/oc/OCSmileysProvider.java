package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.capability.Smiley;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class OCSmileysProvider {

    /**
     * deactivated until it is realized at OC(.de)
     * private static final List<Smiley> SMILEYS = Collections.unmodifiableList(Arrays.asList(
     * new Smiley("8)", R.string.smiley_cool, ""),
     * new Smiley(":,-(", R.string.smiley_cry, ""),
     * new Smiley("::|", R.string.smiley_embarassed, ""),
     * new Smiley(":-!", R.string.smiley_footinmouth, ""),
     * new Smiley(":(", R.string.smiley_frown, ""),
     * new Smiley("O:)", R.string.smiley_innocent, ""),
     * new Smiley(":-*", R.string.smiley_kiss, ""),
     * new Smiley(":D", R.string.smiley_laughing, ""),
     * new Smiley(":-($)", R.string.smiley_money, ""),
     * new Smiley(":x", R.string.smiley_sealed, ""),
     * new Smiley(":)", R.string.smiley_smile, ""),
     * new Smiley(":o", R.string.smiley_surprised, ""),
     * new Smiley(":P", R.string.smiley_tongueout, ""),
     * new Smiley(":/", R.string.smiley_undecided, ""),
     * new Smiley(";)", R.string.smiley_wink, ""),
     * new Smiley("XO", R.string.smiley_yell, "")
     * ));
     */
    private static final List<Smiley> SMILEYS = Collections.unmodifiableList(Collections.emptyList());

    private OCSmileysProvider() {
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
