package cgeo.geocaching.search;

import cgeo.geocaching.Intents;
import cgeo.geocaching.enumerations.CacheType;

import androidx.annotation.NonNull;

public class GeocacheSearchSuggestionCursor extends BaseSearchSuggestionCursor {

    public void addCache(@NonNull final String geocode, @NonNull final String name, final String type) {
        final int icon = CacheType.getById(type).markerId;
        addRow(new String[]{
                String.valueOf(rowId),
                name,
                geocode,
                Intents.ACTION_GEOCACHE,
                geocode,
                String.valueOf(icon)
        });
        rowId++;
    }

}
