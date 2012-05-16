package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.ILogable;
import cgeo.geocaching.R;
import cgeo.geocaching.StaticMapsActivity;
import cgeo.geocaching.StaticMapsProvider;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;

abstract class AbstractStaticMapsApp extends AbstractNavigationApp {
    public AbstractStaticMapsApp(String name) {
        super(name, null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    protected static boolean hasStaticMap(cgCache cache) {
        if (cache != null) {
            String geocode = cache.getGeocode();
            if (StringUtils.isNotEmpty(geocode) && cgeoapplication.getInstance().isOffline(geocode, null)) {
                return StaticMapsProvider.doesExistStaticMapForCache(geocode);
            }
        }
        return false;
    }

    protected static boolean hasStaticMap(cgWaypoint waypoint) {
        if (waypoint != null) {
            String geocode = waypoint.getGeocode();
            int id = waypoint.getId();
            if (StringUtils.isNotEmpty(geocode) && cgeoapplication.getInstance().isOffline(geocode, null)) {
                return StaticMapsProvider.doesExistStaticMapForWaypoint(geocode, id);
            }
        }
        return false;
    }

    protected static boolean invoke(final Activity activity, final cgCache cache, final cgWaypoint waypoint, final boolean download) {
        final ILogable logable = cache != null && cache.getListId() != 0 ? cache : waypoint;
        final String geocode = logable.getGeocode().toUpperCase();
        if (geocode == null) {
            ActivityMixin.showToast(activity, getString(R.string.err_detail_no_map_static));
            return true;
        }

        final Intent intent = new Intent(activity, StaticMapsActivity.class);
        intent.putExtra("geocode", geocode);
        if (download) {
            intent.putExtra("download", true);
        }
        if (waypoint != null) {
            intent.putExtra("waypoint", waypoint.getId());
        }
        activity.startActivity(intent);

        return true;
    }


    @Override
    public boolean isEnabled(Geopoint geopoint) {
        return geopoint == null;
    }

}
