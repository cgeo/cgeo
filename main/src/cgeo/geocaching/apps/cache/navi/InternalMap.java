package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;

import java.util.UUID;

class InternalMap extends AbstractInternalMap implements
        NavigationApp {

    InternalMap(Resources res) {
        super(res.getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            final UUID searchId, cgWaypoint waypoint, final Geopoint coords) {
        cgSettings settings = getSettings(activity);
        Intent mapIntent = new Intent(activity, settings.getMapFactory().getMapClass());
        if (cache != null) {
            mapIntent.putExtra("detail", false);
            mapIntent.putExtra("geocode", cache.geocode);
        }
        if (searchId != null) {
            mapIntent.putExtra("detail", true);
            mapIntent.putExtra("searchid", searchId.toString());
        }
        if (waypoint != null) {
            mapIntent.putExtra("latitude", waypoint.coords.getLatitude());
            mapIntent.putExtra("longitude", waypoint.coords.getLongitude());
            mapIntent.putExtra("wpttype", waypoint.type);
        } else if (coords != null) {
            mapIntent.putExtra("latitude", coords.getLatitude());
            mapIntent.putExtra("longitude", coords.getLongitude());
            mapIntent.putExtra("wpttype", WaypointType.WAYPOINT.id);
        }

        activity.startActivity(mapIntent);
        return true;
    }

}
