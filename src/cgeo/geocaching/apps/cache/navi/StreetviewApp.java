package cgeo.geocaching.apps.cache.navi;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.activity.ActivityMixin;

class StreetviewApp extends AbstractNavigationApp implements NavigationApp {

    StreetviewApp(final Resources res) {
        super(res.getString(R.string.cache_menu_streetview), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    public boolean invoke(cgGeo geo, Activity activity, Resources res,
            cgCache cache,
            Long searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
        if (cache == null && waypoint == null && latitude == null && longitude == null) {
            return false;
        }

        try {
            if (cache != null && cache.latitude != null && cache.longitude != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.streetview:cbll=" + cache.latitude + "," + cache.longitude)));
            } else if (waypoint != null && waypoint.latitude != null && waypoint.longitude != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.streetview:cbll=" + waypoint.latitude + "," + waypoint.longitude)));
            }

            return true;
        } catch (ActivityNotFoundException e) {
            if (res != null) {
                ActivityMixin.showToast(activity, res.getString(R.string.err_application_no));
            }
        }
        
        return false;
    }
}
