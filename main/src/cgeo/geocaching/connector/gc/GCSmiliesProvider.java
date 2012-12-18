package cgeo.geocaching.connector.gc;


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

        public final String text;

        Smiley(final String text) {
            this.text = text;
        }

        public int getItemId() {
            return text.hashCode();
        }
    }

    public static Smiley[] getSmilies() {
        return Smiley.values();
    }

    public static Smiley getSmiley(int itemId) {
        for (Smiley smiley : getSmilies()) {
            if (smiley.getItemId() == itemId) {
                return smiley;
            }
        }
        return null;
    }
}
