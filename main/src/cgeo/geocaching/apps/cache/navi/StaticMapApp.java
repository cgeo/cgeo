package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.ILogable;
import cgeo.geocaching.R;
import cgeo.geocaching.StaticMapsActivity;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

class StaticMapApp extends AbstractNavigationApp {

    StaticMapApp() {
        super(getString(R.string.cache_menu_map_static), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, cgCache cache, cgWaypoint waypoint, final Geopoint coords) {
        final ILogable logable = cache != null && cache.getListId() != 0 ? cache : waypoint;
        final String geocode = logable.getGeocode().toUpperCase();
        if (geocode == null) {
            ActivityMixin.showToast(activity, getString(R.string.err_detail_no_map_static));
            return true;
        }

        final Intent intent = new Intent(activity, StaticMapsActivity.class);
        intent.putExtra("geocode", geocode);
        if (waypoint != null) {
            intent.putExtra("waypoint", waypoint.getId());
        }
        activity.startActivity(intent);

        return true;
    }
}
