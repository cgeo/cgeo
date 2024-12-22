package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.map.layer.hills.AClasyHillShading;
import org.mapsforge.map.layer.hills.AdaptiveClasyHillShading;
import org.mapsforge.map.layer.hills.HgtFileInfo;
import org.mapsforge.map.layer.hills.HiResClasyHillShading;

public class CgeoAdaptiveClasyHillShading extends AdaptiveClasyHillShading {

    public CgeoAdaptiveClasyHillShading() {
        super(false);
    }

    @Override
    public int getZoomMax(HgtFileInfo hgtFileInfo) {
        return 17;
    }
}
