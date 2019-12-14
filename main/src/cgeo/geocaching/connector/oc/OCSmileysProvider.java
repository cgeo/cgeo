package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.capability.Smiley;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OCSmileysProvider {

    private OCSmileysProvider() {
        // utility class
    }

    private static final List<Smiley> SMILIES = Collections.unmodifiableList(Arrays.asList(
            new Smiley("8)", "cool"),
            new Smiley(":,-(", "cry"),
            new Smiley("::|", "embarassed"),
            new Smiley(":(", "frown"),
            new Smiley("O:)", "innocent"),
            new Smiley(":-*", "kiss"),
            new Smiley(":D", "laughing"),
            new Smiley(":-($)", "money"),
            new Smiley(":x", "sealed"),
            new Smiley(":)", "smile"),
            new Smiley(":-o", "surprised"),
            new Smiley(":P", "tongue out"),
            new Smiley(":-/", "undecided"),
            new Smiley(";)", "wink"),
            new Smiley("XO", "yell")
           ));

    @NonNull
    public static List<Smiley> getSmileys() {
        return SMILIES;
    }

}
