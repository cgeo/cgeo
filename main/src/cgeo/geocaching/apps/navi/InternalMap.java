package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.support.annotation.NonNull;

class InternalMap extends AbstractPointNavigationApp {

    private final Class<?> cls;

    InternalMap(final Class<?> cls, final int name) {
        super(getString(name), null);
        this.cls = cls;
    }

    InternalMap() {
        super(getString(R.string.cache_menu_map), null);
        cls = null;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geopoint coords) {
        DefaultMap.startActivityCoords(activity, cls != null ? cls : Settings.getMapProvider().getMapClass(), coords, WaypointType.WAYPOINT, null);
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint) {
        DefaultMap.startActivityCoords(activity, cls != null ? cls : Settings.getMapProvider().getMapClass(), waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getName());
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geocache cache) {
        DefaultMap.startActivityGeoCode(activity, cls != null ? cls : Settings.getMapProvider().getMapClass(), cache.getGeocode());
    }

}
