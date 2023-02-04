package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class CruiserNavigationApp extends AbstractPointNavigationApp {

    static final String ACTION = "com.devemux86.NAVIGATION";

    protected CruiserNavigationApp() {
        super(getString(R.string.cache_menu_cruiser), null, getString(R.string.package_cruiser));
    }

    @Override
    public void navigate(final @NonNull Context context, final @NonNull Geopoint coords) {
        final Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.setPackage(getString(R.string.package_cruiser));
        intent.putExtra("LATITUDE", new double[]{coords.getLatitude()});
        intent.putExtra("LONGITUDE", new double[]{coords.getLongitude()});
        context.startActivity(intent);
    }

}
