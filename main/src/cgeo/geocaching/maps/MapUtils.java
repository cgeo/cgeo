package cgeo.geocaching.maps;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MapUtils {

    private MapUtils() {
        // should not be instantiated
    }

    // filter waypoints from owned caches if required.
    public static void filter(final Set<Waypoint> waypoints) {
        final boolean excludeMine = Settings.isExcludeMyCaches();

        // filtering required?
        if (!excludeMine) {
            return;
        }
        final List<Waypoint> removeList = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            final Geocache cache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            if (excludeMine && cache.isOwner()) {
                removeList.add(wp);
            }
        }
        waypoints.removeAll(removeList);
    }

    // filter own/found/disabled caches if required
    public static void filter(final Collection<Geocache> caches) {
        final boolean excludeMine = Settings.isExcludeMyCaches();
        final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
        final boolean excludeArchived = Settings.isExcludeArchivedCaches();

        // filtering required?
        if (!excludeMine && !excludeDisabled && !excludeArchived) {
            return;
        }
        final List<Geocache> removeList = new ArrayList<>();
        for (final Geocache cache : caches) {
            if ((excludeMine && cache.isFound()) || (excludeMine && cache.isOwner()) || (excludeDisabled && cache.isDisabled()) || (excludeDisabled && cache.isArchived()) || (excludeArchived && cache.isArchived())) {
                removeList.add(cache);
            }
        }
        caches.removeAll(removeList);
    }
}
