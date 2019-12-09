package cgeo.geocaching.connector.gc;

import cgeo.geocaching.connector.capability.Smiley;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GCSmileysProvider {

    private GCSmileysProvider() {
        // utility class
    }

    private static final List<Smiley> SMILIES = Collections.unmodifiableList(Arrays.asList(
            new Smiley(":)", "smile"),
            new Smiley(":D", "big smile"),
            new Smiley("8D", "cool"),
            new Smiley(":I", "blush"),
            new Smiley(":P", "tongue"),
            new Smiley("}:", "evil"),
            new Smiley(";)", "wink"),
            new Smiley(":o", "clown"),
            new Smiley("B", "black eye"),
            new Smiley("8", "eight ball"),
            new Smiley(":(", "frown"),
            new Smiley("8)", "shy"),
            new Smiley(":O", "shocked"),
            new Smiley(":(!", "angry"),
            new Smiley("xx(", "dead"),
            new Smiley("|)", "sleepy"),
            new Smiley(":X", "kisses"),
            new Smiley("^", "approve"),
            new Smiley("V", "disapprove"),
            new Smiley("?", "question")
           ));

    @NonNull
    public static List<Smiley> getSmileys() {
        return SMILIES;
    }

}
