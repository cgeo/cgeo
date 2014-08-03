package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.ILogable;
import cgeo.geocaching.R;
import cgeo.geocaching.StaticMapsActivity_;
import cgeo.geocaching.StaticMapsProvider;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.AbstractApp;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;

abstract class AbstractStaticMapsApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp {
    protected AbstractStaticMapsApp(final String name, final int id) {
        super(name, id, null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public boolean isUsableAsDefaultNavigationApp() {
        return false;
    }

    protected static boolean hasStaticMap(Waypoint waypoint) {
        if (waypoint==null) {
            return false;
        }
        String geocode = waypoint.getGeocode();
        if (StringUtils.isNotEmpty(geocode) && DataStore.isOffline(geocode, null)) {
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, waypoint);
        }
        return false;
    }

    protected static boolean invokeStaticMaps(final Activity activity, final Geocache cache, final Waypoint waypoint, final boolean download) {
        final ILogable logable = cache != null && cache.getListId() != 0 ? cache : waypoint;
        // If the cache is not stored for offline, cache seems to be null and waypoint may be null too
        if (logable==null || logable.getGeocode()==null ) {
            ActivityMixin.showToast(activity, getString(R.string.err_detail_no_map_static));
            return true;
        }
        final String geocode = StringUtils.upperCase(logable.getGeocode());

        StaticMapsActivity_.IntentBuilder_ builder = StaticMapsActivity_.intent(activity).geocode(geocode).download(download);
        if (waypoint != null) {
            builder.waypointId(waypoint.getId());
        }
        builder.start();
        return true;
    }
}
