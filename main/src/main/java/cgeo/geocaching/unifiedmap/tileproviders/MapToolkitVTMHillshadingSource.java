package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.net.Uri;

import androidx.core.util.Pair;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

public class MapToolkitVTMHillshadingSource extends AbstractMapsforgeVTMOnlineTileProvider {
    public MapToolkitVTMHillshadingSource() {
        super("MapToolkit Hillshading", Uri.parse("https://maptoolkit.p.rapidapi.com/tiles"), "/{Z}/{X}/{Y}/hillshading.png?rapidapi-key=" + Settings.getString(R.string.pref_rapidapiKey, ""), MIN_ZOOM_LEVEL, 14, new Pair<>("© <a href='https://www.maptoolkit.com' target='_blank'>Maptoolkit</a>", false));
    }
}
