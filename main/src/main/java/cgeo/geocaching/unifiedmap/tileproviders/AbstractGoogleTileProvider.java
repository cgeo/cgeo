package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.googlemaps.GoogleMapsFragment;

import androidx.annotation.StringRes;

import com.google.android.gms.maps.GoogleMap;

public class AbstractGoogleTileProvider extends AbstractTileProvider {

    final int mapType;

    AbstractGoogleTileProvider(final int mapType, final @StringRes int nameRes) {
        super(2, 21);
        this.mapType = mapType;
        this.tileProviderName = CgeoApplication.getInstance().getString(nameRes);
    }

    public void setMapType(final GoogleMap googleMap) {
        if (googleMap != null) {
            googleMap.setMapType(mapType);
        }
    }

    @Override
    public AbstractMapFragment createMapFragment() {
        return new GoogleMapsFragment();
    }

}
