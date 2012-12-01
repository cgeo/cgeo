package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.ILogable;
import cgeo.geocaching.R;
import cgeo.geocaching.StaticMapsActivity;
import cgeo.geocaching.StaticMapsProvider;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.AbstractApp;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;

abstract class AbstractStaticMapsApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp {
    public AbstractStaticMapsApp(String name) {
        super(name, null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public boolean isDefaultNavigationApp() {
        return false;
    }

    protected static boolean hasStaticMap(cgWaypoint waypoint) {
        if (waypoint==null) {
            return false;
        }
        String geocode = waypoint.getGeocode();
        int id = waypoint.getId();
        if (StringUtils.isNotEmpty(geocode) && cgData.isOffline(geocode, null)) {
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, id);
        }
        return false;
    }

    protected static boolean invokeStaticMaps(final Activity activity, final cgCache cache, final cgWaypoint waypoint, final boolean download) {
        final ILogable logable = cache != null && cache.getListId() != 0 ? cache : waypoint;
        // If the cache is not stored for offline, cache seems to be null and waypoint may be null too
        if (logable==null || logable.getGeocode()==null ) {
            ActivityMixin.showToast(activity, getString(R.string.err_detail_no_map_static));
            return true;
        }
        final String geocode = StringUtils.upperCase(logable.getGeocode());

        StaticMapsActivity.startActivity(activity, geocode, download, waypoint);
        return true;
    }
}
