package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Waypoint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

public final class DefaultMap {

    private DefaultMap() {
        // utility class
    }

    public static Intent getLiveMapIntent(final Activity fromActivity) {
        return new UnifiedMapType().getLaunchMapIntent(fromActivity);
    }

    public static void startActivityLive(final Activity fromActivity) {
        fromActivity.startActivity(getLiveMapIntent(fromActivity));
    }

    public static void startActivityCoords(final Context fromActivity, final Waypoint waypoint) {
        new UnifiedMapType(waypoint).launchMap(fromActivity);
    }

    public static void startActivityCoords(final Activity fromActivity, final Geopoint coords) {
        new UnifiedMapType(coords).launchMap(fromActivity);
    }

    public static void startActivityCoords(final Context fromActivity, final Geopoint coords, final WaypointType type) {
        new UnifiedMapType(coords).launchMap(fromActivity);
    }

    public static void startActivityInitialCoords(final Context fromActivity, final Geopoint coords) {
        new UnifiedMapType(coords).launchMap(fromActivity);
    }

    public static void startActivityGeoCode(final Context fromActivity, final String geocode) {
        final UnifiedMapType mapType = new UnifiedMapType(geocode);
        mapType.filterContext = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
        mapType.launchMap(fromActivity);
    }

    public static void startActivityGeoCode(final Activity fromActivity, final String geocode) {
        new UnifiedMapType(geocode).launchMap(fromActivity);
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title) {
        new UnifiedMapType(search, title).launchMap(fromActivity);
    }

    public static void startActivitySearch(final Activity fromActivity, final SearchResult search, final String title, final int fromList) {
        if (fromList == 0) {
            final Geopoint referencePoint = fromActivity instanceof CacheListActivity ? ((CacheListActivity) fromActivity).getReferencePoint() : null;
            new UnifiedMapType(search, title, referencePoint).launchMap(fromActivity); // same as above
        } else {
            // no longer allowed / CacheListActivity directly launches into startActivityList in this case
            startActivityList(fromActivity, fromList, null);
        }
    }

    public static void startActivityList(final Activity fromActivity, final int fromList, final @Nullable GeocacheFilterContext filterContext) {
        if (fromList != 0) {
            final UnifiedMapType mapType = new UnifiedMapType(fromList, filterContext);
            mapType.launchMap(fromActivity);
        }
    }

    public static void startActivityViewport(final Activity fromActivity, final Viewport viewport) {
        final UnifiedMapType mapType = viewport == null ? new UnifiedMapType() : new UnifiedMapType(viewport);
        mapType.launchMap(fromActivity);
    }

    public static void startActivityWherigoMap(final Activity fromActivity, final Viewport viewport, final String mapTitle, final Geopoint coords) {
        final UnifiedMapType mapType = viewport == null ? new UnifiedMapType() : new UnifiedMapType(viewport, mapTitle);
        mapType.coords = coords;
        mapType.launchMap(fromActivity);
    }
}
