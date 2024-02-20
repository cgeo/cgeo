package cgeo.geocaching.unifiedmap.tileproviders;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

import android.net.Uri;

import androidx.core.util.Pair;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

public class MapToolkitHillshadingSource extends AbstractMapsforgeVTMOnlineTileProvider {
    public MapToolkitHillshadingSource() {
        super("MapToolkit Hillshading", Uri.parse("https://maptoolkit.p.rapidapi.com/tiles"), "/{Z}/{X}/{Y}/hillshading.png?rapidapi-key="+ Settings.getString(R.string.pref_rapidapiKeyMaptoolkit, ""), MIN_ZOOM_LEVEL, 14, new Pair<>("© <a href='https://www.maptoolkit.com' target='_blank'>Maptoolkit</a>", false));
    }
}
