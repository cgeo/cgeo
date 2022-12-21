package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class CacheListAppUtils {

    private CacheListAppUtils() {
        // utility class
    }

    @NonNull
    public static List<Geocache> filterCoords(@NonNull final List<Geocache> caches) {
        final List<Geocache> cachesWithCoords = new ArrayList<>(caches.size());
        for (final Geocache geocache : caches) {
            if (geocache.getCoords() != null) {
                cachesWithCoords.add(geocache);
            }
        }
        return cachesWithCoords;
    }

}
