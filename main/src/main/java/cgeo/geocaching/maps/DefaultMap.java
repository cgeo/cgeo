package cgeo.geocaching.maps;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.UnifiedMapType;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

public final class DefaultMap {

    private DefaultMap() {
        // utility class
    }

    private static Class<?> getDefaultMapClass() {
        return Settings.getMapProvider().getMapClass();
    }

    public static Intent getLiveMapIntent(final Activity fromActivity, final Class<?> cls) {
        if (Settings.useLegacyMaps()) {
            return new MapOptions().newIntent(fromActivity, cls);
        } else {
            Log.d("Launching UnifiedMap in live mode");
            return new UnifiedMapType().getLaunchMapIntent(fromActivity);
        }
    }

    public static Intent getLiveMapIntent(final Activity fromActivity) {
        return getLiveMapIntent(fromActivity, getDefaultMapClass());
    }

    public static void startActivityCoords(final Context fromActivity, final Class<?> cls, final Waypoint waypoint) {
        if (Settings.useLegacyMaps()) {
            new MapOptions(waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getPrefix(), waypoint.getName(), waypoint.getGeocode()).startIntent(fromActivity, cls);
        } else {
            Log.d("Launching UnifiedMap in waypoint mode (1)");
            new UnifiedMapType(waypoint).launchMap(fromActivity);
        }
    }

    public static void startActivityCoords(final Context fromActivity, final Waypoint waypoint) {
        if (Settings.useLegacyMaps()) {
            startActivityCoords(fromActivity, getDefaultMapClass(), waypoint);
        } else {
            Log.d("Launching UnifiedMap in waypoint mode (2)");
            new UnifiedMapType(waypoint).launchMap(fromActivity);
        }
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords) {
        if (Settings.useLegacyMaps()) {
            startActivityCoords(fromActivity, getDefaultMapClass(), coords, null);
        } else {
            Log.d("Launching UnifiedMap in coords mode (1)");
            new UnifiedMapType(coords).launchMap(fromActivity);
        }
    }

    public static void startActivityCoords(final Context fromActivity, final Class<?> cls, final Geopoint coords, final WaypointType type) {
        if (Settings.useLegacyMaps()) {
            new MapOptions(coords, type).startIntent(fromActivity, cls);
        } else {
            Log.d("Launching UnifiedMap in coords with WaypointType mode");
            new UnifiedMapType(coords).launchMap(fromActivity);
        }
    }

    public static void startActivityInitialCoords(final Context fromActivity, final Geopoint coords) {
        if (Settings.useLegacyMaps()) {
            new MapOptions(coords).startIntent(fromActivity, getDefaultMapClass());
        } else {
            Log.d("Launching UnifiedMap in coords mode (2)");
            new UnifiedMapType(coords).launchMap(fromActivity);
        }
    }

    public static void startActivityGeoCode(final Context fromActivity, final Class<?> cls, final String geocode) {
        if (Settings.useLegacyMaps()) {
            final MapOptions mo = new MapOptions(geocode);
            mo.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
            mo.startIntent(fromActivity, cls);
        } else {
            Log.d("Launching UnifiedMap in geocode mode (1)");
            final UnifiedMapType mapType = new UnifiedMapType(geocode);
            mapType.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
            mapType.launchMap(fromActivity);
        }
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        if (Settings.useLegacyMaps()) {
            startActivityGeoCode(fromActivity, getDefaultMapClass(), geocode);
        } else {
            Log.d("Launching UnifiedMap in geocode mode (2)");
            new UnifiedMapType(geocode).launchMap(fromActivity);
        }
    }

    public static void startActivitySearch(final Activity fromActivity, final Class<?> cls, final SearchResult search, final String title) {
        if (Settings.useLegacyMaps()) {
            new MapOptions(search, title, StoredList.TEMPORARY_LIST.id).startIntent(fromActivity, cls);
        } else {
            Log.d("Launching UnifiedMap in searchResult mode (item count: " + search.getGeocodes().size() + ", title='" + title + "')");
            new UnifiedMapType(search, title).launchMap(fromActivity);
        }
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title, final int fromList) {
        if (Settings.useLegacyMaps()) {
            final MapOptions mo = new MapOptions(search, title, fromList);
            mo.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
            mo.startIntent(fromActivity, getDefaultMapClass());
        } else {
            if (fromList == 0) {
                final Geopoint referencePoint = fromActivity instanceof CacheListActivity ? ((CacheListActivity) fromActivity).getReferencePoint() : null;
                new UnifiedMapType(search, title, referencePoint).launchMap(fromActivity); // same as above
            } else {
                // no longer allowed / CacheListActivity directly launches into startActivityList in this case
                startActivityList(fromActivity, fromList, null);
            }
        }
    }

    public static void startActivityList(final Activity fromActivity, final int fromList, final @Nullable GeocacheFilterContext filterContext) {
        if (!Settings.useLegacyMaps() && fromList != 0) { // only supported for UnifiedMap
            Log.d("Launching UnifiedMap in list mode, fromList=" + fromList + ")");
            final UnifiedMapType mapType = new UnifiedMapType(fromList, filterContext);
            mapType.launchMap(fromActivity);
        }
    }

    public static void startActivityViewport(final Activity fromActivity, final Viewport viewport) {
        if (!Settings.useLegacyMaps()) { // only supported for UnifiedMap
            Log.d("Launching UnifiedMap in viewport mode, viewport=" + viewport + ")");
            final UnifiedMapType mapType = viewport == null ? new UnifiedMapType() : new UnifiedMapType(viewport);
            mapType.launchMap(fromActivity);
        }
    }

    public static void startActivityWherigoMap(final Activity fromActivity, final Viewport viewport, final String mapTitle, final Geopoint coords) {
        if (Settings.useLegacyMaps()) {
            final String unifiedMapCategory = LocalizationUtils.getString(R.string.category_unifiedMap);
            SimpleDialog.of(fromActivity)
                    .setTitle(TextParam.id(R.string.wherigo_player))
                    .setMessage(TextParam.id(R.string.wherigo_map_supported_for_unified_map_only, unifiedMapCategory))
                    .show();
        } else {
            Log.d("Launching UnifiedMap in viewport mode, viewport=" + viewport + ")");
            final UnifiedMapType mapType = viewport == null ? new UnifiedMapType() : new UnifiedMapType(viewport, mapTitle);
            mapType.coords = coords;
            mapType.launchMap(fromActivity);
        }
    }
}
