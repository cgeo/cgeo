package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.LocationDataProvider;

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
        final GeoData geo = LocationDataProvider.getInstance().currentGeo();
        final Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.setPackage(getString(R.string.package_cruiser));
        intent.putExtra("LATITUDE", new double[]{geo.getLatitude(), coords.getLatitude()});
        intent.putExtra("LONGITUDE", new double[]{geo.getLongitude(), coords.getLongitude()});
        context.startActivity(intent);
    }

}
