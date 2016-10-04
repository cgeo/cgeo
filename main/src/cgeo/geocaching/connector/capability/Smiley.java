package cgeo.geocaching.connector.capability;

import android.support.annotation.NonNull;

public class Smiley {
    public @NonNull final String text;
    public @NonNull final String meaning;

    public Smiley(@NonNull final String text, @NonNull final String meaning) {
        this.text = text;
        this.meaning = meaning;
    }
}