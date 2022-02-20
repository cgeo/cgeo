package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;

import com.google.android.gms.maps.GoogleMap;

class GoogleSatelliteSource extends AbstractGoogleTileProvider {

    GoogleSatelliteSource() {
        super(GoogleMap.MAP_TYPE_SATELLITE, R.string.map_source_google_satellite);
    }
}
