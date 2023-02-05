package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.LocationDataProvider;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class CruiserNavigationApp extends AbstractPointNavigationApp {

    static final String ACTION = "com.devemux86.NAVIGATION";

    protected CruiserNavigationApp() {
        super(getString(R.string.cache_menu_cruiser), null, getString(R.string.package_cruiser));
    }

    @Override
    public void navigate(final @NonNull Context context, final @NonNull Geopoint coords) {
        navigate(context, coords, null);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords, cache.getName());
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        final Geopoint coords = waypoint.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords, waypoint.getName());
    }

    private void navigate(@NonNull final Context context, @NonNull final Geopoint coords, @Nullable final String info) {
        final GeoData geo = LocationDataProvider.getInstance().currentGeo();
        final Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.setPackage(getString(R.string.package_cruiser));
        intent.putExtra("LATITUDE", new double[]{geo.getLatitude(), coords.getLatitude()});
        intent.putExtra("LONGITUDE", new double[]{geo.getLongitude(), coords.getLongitude()});
        if (StringUtils.isNotBlank(info)) {
            intent.putExtra("NAME", new String[] {info});
        }
        context.startActivity(intent);
    }
}
