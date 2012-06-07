package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.ILogable;
import cgeo.geocaching.R;
import cgeo.geocaching.StaticMapsActivity;
import cgeo.geocaching.StaticMapsProvider;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
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

    protected static boolean hasStaticMap(cgCache cache) {
        String geocode = cache.getGeocode();
        if (StringUtils.isNotEmpty(geocode) && cgeoapplication.getInstance().isOffline(geocode, null)) {
            return StaticMapsProvider.hasStaticMapForCache(geocode);
        }
        return false;
    }

    protected static boolean hasStaticMap(cgWaypoint waypoint) {
        String geocode = waypoint.getGeocode();
        int id = waypoint.getId();
        if (StringUtils.isNotEmpty(geocode) && cgeoapplication.getInstance().isOffline(geocode, null)) {
            return StaticMapsProvider.hasStaticMapForWaypoint(geocode, id);
        }
        return false;
    }

    protected static boolean invokeStaticMaps(final Activity activity, final cgCache cache, final cgWaypoint waypoint, final boolean download) {
        final ILogable logable = cache != null && cache.getListId() != 0 ? cache : waypoint;
        final String geocode = StringUtils.upperCase(logable.getGeocode());
        if (geocode == null) {
            ActivityMixin.showToast(activity, getString(R.string.err_detail_no_map_static));
            return true;
        }

        StaticMapsActivity.startActivity(activity, geocode, download, waypoint);
        return true;
    }
}
