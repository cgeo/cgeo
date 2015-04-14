package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.utils.Log;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleMapsDirectionApp extends AbstractPointNavigationApp {

    protected GoogleMapsDirectionApp() {
        super(getString(R.string.cache_menu_maps_directions), null);
    }

    @Override
    public boolean isInstalled() {
        return MapProviderFactory.isGoogleMapsInstalled();
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geopoint coords) {
        try {
            final GeoData geo = Sensors.getInstance().currentGeo();
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://maps.google.com/maps?f=d&saddr="
                            + geo.getCoords().getLatitude() + "," + geo.getCoords().getLongitude() + "&daddr="
                            + coords.getLatitude() + "," + coords.getLongitude())));

        } catch (final Exception e) {
            Log.i("GoogleMapsDirectionApp: application not available.", e);
        }

    }

}
