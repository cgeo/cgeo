package cgeo.geocaching.maps;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.UnifiedMapType;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public final class DefaultMap {

    private DefaultMap() {
        // utility class
    }

    private static Class<?> getDefaultMapClass() {
        return Settings.getMapProvider().getMapClass();
    }

    public static Intent getLiveMapIntent(final Activity fromActivity, final Class<?> cls) {
        return new MapOptions().newIntent(fromActivity, cls);
    }

    public static Intent getLiveMapIntent(final Activity fromActivity) {
        return getLiveMapIntent(fromActivity, getDefaultMapClass());
    }

    public static Intent getLiveMapIntent(final Activity fromActivity, final Geopoint coords) {
        return new MapOptions(coords).newIntent(fromActivity, getDefaultMapClass());
    }

    public static void startActivityCoords(final Context fromActivity, final Class<?> cls, final Waypoint waypoint) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in waypoint mode (1)");
            new UnifiedMapType(waypoint.getGeocode()).launchMap(fromActivity);
        } else {
            new MapOptions(waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName(), waypoint.getGeocode()).startIntent(fromActivity, cls);
        }
    }

    public static void startActivityCoords(final Context fromActivity, final Waypoint waypoint) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in waypoint mode (2)");
            new UnifiedMapType(waypoint.getGeocode()).launchMap(fromActivity);
        } else {
            startActivityCoords(fromActivity, getDefaultMapClass(), waypoint);
        }
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in coords mode");
            new UnifiedMapType(coords).launchMap(fromActivity);
        } else {
            startActivityCoords(fromActivity, getDefaultMapClass(), coords, null);
        }
    }

    public static void startActivityCoords(final Context fromActivity, final Class<?> cls, final Geopoint coords, final WaypointType type) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in coords with WaypointType mode");
            new UnifiedMapType(coords).launchMap(fromActivity);
        } else {
            new MapOptions(coords, type).startIntent(fromActivity, cls);
        }
    }

    public static void startActivityInitialCoords(final Context fromActivity, final Geopoint coords) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in coords mode");
            new UnifiedMapType(coords).launchMap(fromActivity);
        } else {
            new MapOptions(coords).startIntent(fromActivity, getDefaultMapClass());
        }
    }

    public static void startActivityGeoCode(final Context fromActivity, final Class<?> cls, final String geocode) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in geocode mode");
            new UnifiedMapType(geocode).launchMap(fromActivity);
        } else {
            final MapOptions mo = new MapOptions(geocode);
            mo.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
            mo.startIntent(fromActivity, cls);
        }
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in geocode mode");
            new UnifiedMapType(geocode).launchMap(fromActivity);
        } else {
            startActivityGeoCode(fromActivity, getDefaultMapClass(), geocode);
        }
    }

    public static void startActivitySearch(final Activity fromActivity, final Class<?> cls, final SearchResult search, final String title, final int fromList) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in searchResult mode (item count: " + search.getGeocodes().size() + ", title='" + title + "', fromList=" + fromList + ")");
            new UnifiedMapType(search, title, fromList).launchMap(fromActivity);
        } else {
            new MapOptions(search, title, fromList).startIntent(fromActivity, cls);
        }
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title, final int fromList) {
        if (Settings.useUnifiedMap()) {
            Log.e("Launching UnifiedMap in searchResult mode (item count: " + search.getGeocodes().size() + ", title='" + title + "', fromList=" + fromList + ")");
            // @todo: filter
            new UnifiedMapType(search, title, fromList).launchMap(fromActivity);
        } else {
            final MapOptions mo = new MapOptions(search, title, fromList);
            mo.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
            mo.startIntent(fromActivity, getDefaultMapClass());
        }
    }


}
