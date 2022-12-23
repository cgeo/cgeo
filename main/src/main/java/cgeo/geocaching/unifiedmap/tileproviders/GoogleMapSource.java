package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;

import com.google.android.gms.maps.GoogleMap;

class GoogleMapSource extends AbstractGoogleTileProvider {

    GoogleMapSource() {
        super(GoogleMap.MAP_TYPE_NORMAL, R.string.map_source_google_map);
        supportsThemes = true;
    }
}
