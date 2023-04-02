package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_MAPSFORGE;

import android.view.View;

import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

class MapsforgePositionLayer extends AbstractPositionLayer<GeoPoint> {

    private static final GeopointConverter<GeoPoint> GP_CONVERTER = new GeopointConverter<>(
            gp -> new GeoPoint(gp.getLatitude(), gp.getLongitude()),
            gp -> new Geopoint(gp.getLatitude(), gp.getLongitude())
    );

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

    protected void destroyLayer(final Map map) {
        map.layers().remove(navigationLayer);
        MAP_MAPSFORGE.clearGroup(LayerHelper.ZINDEX_TRACK_ROUTE);
    }

    // ========================================================================
    // route / track handling

    @Override
    public void updateIndividualRoute(final Route route) {
        super.updateIndividualRoute(route, GP_CONVERTER::toListList);
    }

    @Override
    public void updateTrack(final String key, final IGeoItemSupplier track, final int color, final int width) {
        super.updateTrack(key, track, color, width, GP_CONVERTER::toListList);
    }

    // ========================================================================
    // repaint methods

    @Override
    protected void repaintRouteAndTracks() {
        MAP_MAPSFORGE.clearGroup(LayerHelper.ZINDEX_TRACK_ROUTE);
        repaintRouteAndTracksHelper((segment, color, width) -> {
            if (segment.size() < 2) {
                return; // no line can be drawn from a single point
            }
            final PathLayer segmentLayer = new PathLayer(map, Style.builder()
                    .strokeWidth(MapLineUtils.getWidthFromRaw(width, true))
                    .strokeColor(color)
                    .build());
            MAP_MAPSFORGE.addLayerToGroup(segmentLayer, LayerHelper.ZINDEX_TRACK_ROUTE);
            segmentLayer.setPoints(segment);
        });
    }

}
