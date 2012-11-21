package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class GoogleMapsDirectionApp extends AbstractPointNavigationApp {

    protected GoogleMapsDirectionApp() {
        super(getString(R.string.cache_menu_maps_directions), null);
    }

    @Override
    public boolean isInstalled() {
        return MapProviderFactory.isGoogleMapsInstalled();
    }

    @Override
    public void navigate(Activity activity, Geopoint coords) {
        try {
            IGeoData geo = cgeoapplication.getInstance().currentGeo();
            final Geopoint coordsNow = geo == null ? null : geo.getCoords();

            if (coordsNow != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://maps.google.com/maps?f=d&saddr="
                                + coordsNow.getLatitude() + "," + coordsNow.getLongitude() + "&daddr="
                                + coords.getLatitude() + "," + coords.getLongitude())));
            } else {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://maps.google.com/maps?f=d&daddr="
                                + coords.getLatitude() + "," + coords.getLongitude())));
            }

        } catch (Exception e) {
            Log.i("GoogleMapsDirection: application not available.");
        }

    }

}
