package cgeo.geocaching.unifiedmap.mapsforgevtm;

import org.oscim.layers.vector.VectorLayer;
import org.oscim.map.Map;

class ClearableVectorLayer extends VectorLayer {

    ClearableVectorLayer(final Map map) {
        super(map);
    }

    public void clear() {
        mDrawables.clear();
    }
}
