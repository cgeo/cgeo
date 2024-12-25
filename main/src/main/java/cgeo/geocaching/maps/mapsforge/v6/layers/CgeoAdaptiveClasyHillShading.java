package cgeo.geocaching.maps.mapsforge.v6.layers;

import org.mapsforge.map.layer.hills.AdaptiveClasyHillShading;
import org.mapsforge.map.layer.hills.HgtFileInfo;

public class CgeoAdaptiveClasyHillShading extends AdaptiveClasyHillShading {

    public CgeoAdaptiveClasyHillShading(final boolean isHqEnabled) {
        super(isHqEnabled);
    }

    @Override
    public int getZoomMax(final HgtFileInfo hgtFileInfo) {
        return 17;
    }
}
