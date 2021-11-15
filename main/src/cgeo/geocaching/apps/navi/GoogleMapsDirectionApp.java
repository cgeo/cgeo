package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

class GoogleMapsDirectionApp extends AbstractPointNavigationApp {

    protected GoogleMapsDirectionApp() {
        super(getString(R.string.cache_menu_maps_directions), null);
    }

    @Override
    public boolean isInstalled() {
        return MapProviderFactory.isGoogleMapsInstalled();
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        try {
            final GeoData geo = Sensors.getInstance().currentGeo();
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("https://maps.google.com/maps?f=d&saddr="
                            + geo.getCoords().getLatitude() + "," + geo.getCoords().getLongitude() + "&daddr="
                            + coords.getLatitude() + "," + coords.getLongitude())));

        } catch (final Exception e) {
            Log.i("GoogleMapsDirectionApp: application not available.", e);
        }

    }

}
