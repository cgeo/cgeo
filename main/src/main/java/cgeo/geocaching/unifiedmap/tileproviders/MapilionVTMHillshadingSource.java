package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.net.Uri;

import androidx.core.util.Pair;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

public class MapilionVTMHillshadingSource extends AbstractMapsforgeVTMOnlineTileProvider {
    public MapilionVTMHillshadingSource() {
        super(CgeoApplication.getInstance().getString(R.string.map_source_mapilion_hillshading), Uri.parse("https://mapilion-vector-and-raster-map-tiles.p.rapidapi.com/rapid-api/hillshades/v2"), "/{Z}/{X}/{Y}?rapidapi-key=" + Settings.getString(R.string.pref_rapidapiKey, ""), MIN_ZOOM_LEVEL, 12, new Pair<>("© <a href=\"https://mapilion.com/attribution\">Mapilion</a>", false));
    }
}
