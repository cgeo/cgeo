package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.utils.GroupedList;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_MAPSFORGE;

import android.view.View;

import org.oscim.core.GeoPoint;
import org.oscim.layers.Layer;
import org.oscim.layers.vector.PathLayer;
import org.oscim.map.Map;

class MapsforgePositionLayer extends AbstractPositionLayer<GeoPoint> {

    final Map map;

    // individual route, routes & tracks
    private final PathLayer navigationLayer;

    MapsforgePositionLayer(final Map map, final View root) {
        super(root, GeoPoint::new);
        this.map = map;

        repaintPosition();

        // history (group)
        // group already created in MapsforgeVtmView.init, layers will be added later
        // MAP_MAPSFORGE.addGroup(LayerHelper.ZINDEX_HISTORY);

        // direction line & navigation
        navigationLayer = new PathLayer(map, MapLineUtils.getDirectionColor(), MapLineUtils.getDirectionLineWidth(true));
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_DIRECTION_LINE, navigationLayer);

        // tracks & routes (group)
        // group already created in MapsforgeVtmView.init, layers will be added later
        // MAP_MAPSFORGE.addGroup(LayerHelper.ZINDEX_TRACK_ROUTE);
    }

    protected void destroyLayer(final GroupedList<Layer> mapLayers) {
        mapLayers.remove(navigationLayer);
        MAP_MAPSFORGE.clearGroup(LayerHelper.ZINDEX_TRACK_ROUTE);
    }

}
