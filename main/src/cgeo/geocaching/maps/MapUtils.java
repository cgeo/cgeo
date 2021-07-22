package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.FilterUtils;

import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MapUtils {

    private MapUtils() {
        // should not be instantiated
    }

    // filter waypoints from owned caches or certain wp types if requested.
    public static void filter(final Set<Waypoint> waypoints, final GeocacheFilterContext filterContext, final boolean checkCacheFilters) {
        final boolean excludeMine = checkCacheFilters && Settings.isExcludeMyCaches();
        final boolean excludeFound = checkCacheFilters && Settings.isExcludeFound();
        final boolean excludeDisabled = checkCacheFilters && Settings.isExcludeDisabledCaches();
        final boolean excludeArchived = checkCacheFilters && Settings.isExcludeArchivedCaches();
        final boolean excludeOfflineLog = checkCacheFilters && Settings.isExcludeOfflineLog();
        final CacheType filterCacheType = checkCacheFilters ? null : (Settings.getCacheType() == null ? CacheType.ALL : Settings.getCacheType());

        final GeocacheFilter filter = filterContext.get();

        final boolean excludeWpOriginal = Settings.isExcludeWpOriginal();
        final boolean excludeWpParking = Settings.isExcludeWpParking();
        final boolean excludeWpVisited = Settings.isExcludeWpVisited();

        final List<Waypoint> removeList = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            final Geocache cache = DataStore.loadCache(wp.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            final WaypointType wpt = wp.getWaypointType();
            if (!filterCache(cache, excludeMine, excludeFound, excludeDisabled, excludeArchived, excludeOfflineLog, filterCacheType) ||
                !filter.filter(cache) ||
                (excludeWpOriginal && wpt == WaypointType.ORIGINAL) ||
                (excludeWpParking && wpt == WaypointType.PARKING) ||
                (excludeWpVisited && wp.isVisited())) {
                removeList.add(wp);
            }
        }
        waypoints.removeAll(removeList);
    }

    /** Applies given filter to cache list. Additionally, creates a second list additionally filtered by own/found/disabled caches if required */
    public static void filter(final Collection<Geocache> caches, final GeocacheFilterContext filterContext) {

        final GeocacheFilter filter = filterContext.get();
        filter.filterList(caches);


        final boolean excludeMine = Settings.isExcludeMyCaches();
        final boolean excludeFound = Settings.isExcludeFound();
        final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
        final boolean excludeArchived = Settings.isExcludeArchivedCaches();
        final boolean excludeOfflineLog = Settings.isExcludeOfflineLog();

        final CacheType filterCacheType = Settings.getCacheType() == null ? CacheType.ALL : Settings.getCacheType();

        // filtering required?
        if (!excludeMine && !excludeFound && !excludeDisabled && !excludeArchived && !excludeOfflineLog && filterCacheType.equals(CacheType.ALL)) {
            return;
        }
        final List<Geocache> removeList = new ArrayList<>();
        for (final Geocache cache : caches) {
            if (!filterCache(cache, excludeMine, excludeFound, excludeDisabled, excludeArchived, excludeOfflineLog, filterCacheType)) {
                removeList.add(cache);
            }
        }
        caches.removeAll(removeList);
    }

    private static boolean filterCache(final Geocache cache, final boolean excludeMine, final boolean excludeFound, final boolean excludeDisabled, final boolean excludeArchived, final boolean excludeOfflineLog, final CacheType globalType) {
        return cache != null && (!excludeFound || !cache.isFound()) && (!excludeMine || !cache.isOwner()) && (!excludeDisabled || !cache.isDisabled()) && (!excludeArchived || !cache.isArchived()) &&
            (!excludeOfflineLog || !cache.hasLogOffline()) &&
            (globalType == null || globalType.equals(CacheType.ALL) || globalType.equals(cache.getType()));

    }

    public static void updateFilterBar(final Activity activity, final GeocacheFilterContext filterContext) {
        FilterUtils.updateFilterBar(activity, getActiveMapFilterNames(filterContext));
    }

    @NonNull
    private static List<String> getActiveMapFilterNames(final GeocacheFilterContext filterContext) {
        final List<String> filters = new ArrayList<>();
        if (Settings.getCacheType() != CacheType.ALL) {
            filters.add(Settings.getCacheType().getL10n());
        }

        final GeocacheFilter filter = filterContext.get();
        if (filter.isFiltering()) {
            filters.add(filter.toUserDisplayableString());
        }

        return filters;
    }

    // one-time messages to be shown for maps
    public static void showMapOneTimeMessages(final Activity activity) {
        Dialogs.basicOneTimeMessage(activity, OneTimeDialogs.DialogType.MAP_QUICK_SETTINGS);
        Dialogs.basicOneTimeMessage(activity, Settings.isLongTapOnMapActivated() ? OneTimeDialogs.DialogType.MAP_LONG_TAP_ENABLED : OneTimeDialogs.DialogType.MAP_LONG_TAP_DISABLED);
    }

    // workaround for colored ActionBar titles/subtitles
    // @todo remove after switching map ActionBar to Toolbar
    public static Spanned getColoredValue(final String value) {
        return Html.fromHtml("<font color=\"" + String.format("#%06X", 0xFFFFFF & CgeoApplication.getInstance().getResources().getColor(R.color.colorTextActionBar)) + "\">" + value + "</font>");
    }
}
