package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;

import android.content.Context;

import androidx.annotation.NonNull;

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
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        DefaultMap.startActivityCoords(context, cls != null ? cls : Settings.getMapProvider().getMapClass(), coords, WaypointType.WAYPOINT);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        DefaultMap.startActivityCoords(context, cls != null ? cls : Settings.getMapProvider().getMapClass(), waypoint);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        DefaultMap.startActivityGeoCode(context, cls != null ? cls : Settings.getMapProvider().getMapClass(), cache.getGeocode());
    }

}
