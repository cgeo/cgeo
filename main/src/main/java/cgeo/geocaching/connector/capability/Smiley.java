package cgeo.geocaching.connector.capability;

import androidx.annotation.NonNull;

public class Smiley {
    @NonNull public final String symbol;
    public final int meaning;
    @NonNull public final String emoji;

    public Smiley(@NonNull final String symbol, final int meaning, @NonNull final String emoji) {
        this.symbol = symbol;
        this.meaning = meaning;
        this.emoji = emoji;
    }

    public int getItemId() {
        return this.hashCode();
    }
}
