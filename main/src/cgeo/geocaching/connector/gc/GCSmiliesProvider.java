package cgeo.geocaching.connector.gc;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class GCSmiliesProvider {
    public enum Smiley {
        SMILE(":)"),
        BIG_SMILE(":D"),
        COOL("8D"),
        BLUSH(":I"),
        TONGUE(":P"),
        EVIL("}:)"),
        WINK(";)"),
        CLOWN(":o)"),
        BLACK_EYE("B)"),
        EIGHTBALL("8"),
        FROWN(":("),
        SHY("8)"),
        SHOCKED(":O"),
        ANGRY(":(!"),
        DEAD("xx("),
        SLEEPY("|)"),
        KISSES(":X"),
        APPROVE("^"),
        DISAPPROVE("V"),
        QUESTION("?");

        @NonNull
        public final String text;

        Smiley(@NonNull final String text) {
            this.text = text;
        }

        public int getItemId() {
            return text.hashCode();
        }
    }

    @NonNull
    public static Smiley[] getSmilies() {
        return Smiley.values();
    }

    @Nullable
    public static Smiley getSmiley(final int itemId) {
        for (final Smiley smiley : getSmilies()) {
            if (smiley.getItemId() == itemId) {
                return smiley;
            }
        }
        return null;
    }
}
