package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;

import com.google.android.gms.maps.GoogleMap;

class GoogleTerrainSource extends AbstractGoogleTileProvider {

    GoogleTerrainSource() {
        super(GoogleMap.MAP_TYPE_TERRAIN, R.string.map_source_google_terrain);
    }

}
