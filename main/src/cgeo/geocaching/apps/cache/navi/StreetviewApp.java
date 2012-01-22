package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

class StreetviewApp extends AbstractPointNavigationApp {

    StreetviewApp() {
        super(getString(R.string.cache_menu_streetview), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    protected void navigate(Activity activity, Geopoint point) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("google.streetview:cbll=" + point.getLatitude() + "," + point.getLongitude())));
        } catch (ActivityNotFoundException e) {
            ActivityMixin.showToast(activity, cgeoapplication.getInstance().getString(R.string.err_application_no));
        }
    }
}