package cgeo.geocaching.maps;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MapUtils {

    private MapUtils() {
        // should not be instantiated
    }

    // filter waypoints from owned caches or certain wp types if requested.
    public static void filter(final Set<Waypoint> waypoints, final boolean checkOwnership) {
        final boolean excludeMine = checkOwnership && Settings.isExcludeMyCaches();
        final boolean excludeWpOriginal = Settings.isExcludeWpOriginal();
        final boolean excludeWpParking = Settings.isExcludeWpParking();
        final boolean excludeWpVisited = Settings.isExcludeWpVisited();

        // filtering required?
        if (!excludeMine && !excludeWpOriginal && !excludeWpParking && !excludeWpVisited) {
            return;
        }
        final List<Waypoint> removeList = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            final Geocache cache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            final WaypointType wpt = wp.getWaypointType();
            if ((excludeMine && cache.isOwner()) || (excludeWpOriginal && wpt == WaypointType.ORIGINAL) || (excludeWpParking && wpt == WaypointType.PARKING) || (excludeWpVisited && wp.isVisited())) {
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

        final CacheType filterCacheType = Settings.getCacheType() == null ? CacheType.ALL : Settings.getCacheType();

        // filtering required?
        if (!excludeMine && !excludeDisabled && !excludeArchived && filterCacheType.equals(CacheType.ALL)) {
            return;
        }
        final List<Geocache> removeList = new ArrayList<>();
        for (final Geocache cache : caches) {
            if ((excludeMine && cache.isFound()) || (excludeMine && cache.isOwner()) || (excludeDisabled && cache.isDisabled()) || (excludeArchived && cache.isArchived()) ||
                (!filterCacheType.equals(CacheType.ALL) && !filterCacheType.equals(cache.getType()))) {
                removeList.add(cache);
            }
        }
        caches.removeAll(removeList);
    }

    // one-time messages to be shown for maps
    public static void showMapOneTimeMessages(final Activity activity) {
        Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_QUICK_SETTINGS);
        Dialogs.basicOneTimeMessage(activity, Settings.isLongTapOnMapActivated() ? OneTimeDialogs.DialogType.MAP_LONG_TAP_ENABLED : OneTimeDialogs.DialogType.MAP_LONG_TAP_DISABLED);
    }

}
