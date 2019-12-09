package cgeo.geocaching.connector.capability;

import androidx.annotation.NonNull;

public class Smiley {
    @NonNull public final String text;
    @NonNull public final String meaning;

    public Smiley(@NonNull final String text, @NonNull final String meaning) {
        this.text = text;
        this.meaning = meaning;
    }
}
