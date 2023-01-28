package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.location.IGeoDataProvider;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_MAPSFORGE;

import android.graphics.Matrix;
import android.location.Location;
import android.view.View;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.vector.PathLayer;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.CircleDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

class MapsforgePositionLayer extends AbstractPositionLayer<GeoPoint> {

    private static final GeopointConverter<GeoPoint> GP_CONVERTER = new GeopointConverter<>(
            gp -> new GeoPoint(gp.getLatitude(), gp.getLongitude()),
            gp -> new Geopoint(gp.getLatitude(), gp.getLongitude())
    );

    final Map map;

    // position and heading arrow
    private final ItemizedLayer arrowLayer;
    private final int arrowWidthHalf = arrowWidth / 2;
    private final int arrowHeightHalf = arrowHeight / 2;

    private final VectorLayer accuracyCircleLayer;
    private CircleDrawable accuracyCircle;
    private final Style accuracyCircleStyle = Style.builder()
            .strokeWidth(1.0f)
            .strokeColor(MapLineUtils.getAccuracyCircleColor())
            .fillColor(MapLineUtils.getAccuracyCircleFillColor())
            .build();

    // position history
    final Style historyStyle = Style.builder()
            .strokeWidth(MapLineUtils.getHistoryLineWidth(true))
            .strokeColor(MapLineUtils.getTrailColor())
            .build();

    // individual route, routes & tracks
    private final PathLayer navigationLayer;
    final Style routeStyle = Style.builder()
            .strokeWidth(MapLineUtils.getRouteLineWidth(true))
            .strokeColor(MapLineUtils.getRouteColor())
            .build();
    final Style trackStyle = Style.builder()
            .strokeWidth(MapLineUtils.getTrackLineWidth(true))
            .strokeColor(MapLineUtils.getTrackColor())
            .build();

    MapsforgePositionLayer(final Map map, final View root) {
        super(root, GeoPoint::new);
        this.map = map;

        // position and heading arrow
        accuracyCircleLayer = new VectorLayer(map);
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_POSITION_ACCURACY_CIRCLE, accuracyCircleLayer);
        arrowLayer = new ItemizedLayer(map, new MarkerSymbol(new AndroidBitmap(positionAndHeadingArrow), MarkerSymbol.HotspotPlace.CENTER));
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_POSITION, arrowLayer);
        repaintPosition();

        // history (group)
        MAP_MAPSFORGE.addGroup(LayerHelper.ZINDEX_HISTORY);

        // direction line & navigation
        navigationLayer = new PathLayer(map, MapLineUtils.getDirectionColor(), MapLineUtils.getDirectionLineWidth(true));
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_DIRECTION_LINE, navigationLayer);

        // tracks & routes (group)
        MAP_MAPSFORGE.addGroup(LayerHelper.ZINDEX_TRACK_ROUTE);
    }

    protected void destroyLayer(final Map map) {
        map.layers().remove(accuracyCircleLayer);
        map.layers().remove(arrowLayer);
        map.layers().remove(LayerHelper.ZINDEX_HISTORY);
        map.layers().remove(navigationLayer);
        map.layers().remove(LayerHelper.ZINDEX_TRACK_ROUTE);
    }

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        setCurrentPositionAndHeadingHelper(location, heading, navigationLayer::setPoints, MAP_MAPSFORGE);
    }

    // ========================================================================
    // route / track handling

    @Override
    public void updateIndividualRoute(final Route route) {
        super.updateIndividualRoute(route, GP_CONVERTER::toListList);
    }

    @Override
    public void updateTrack(final String key, final IGeoDataProvider track) {
        super.updateTrack(key, track, GP_CONVERTER::toListList);
    }

    // ========================================================================
    // repaint methods

    @Override
    protected void repaintPosition() {
        super.repaintPosition();
        if (currentLocation != null) {
            // accuracy circle
            final float accuracy = currentLocation.getAccuracy() / 1000.0f;
            if (accuracyCircle != null) {
                accuracyCircleLayer.remove(accuracyCircle);
            }
            accuracyCircle = new CircleDrawable(new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()), accuracy, accuracyCircleStyle);
            accuracyCircleLayer.add(accuracyCircle);
            accuracyCircleLayer.update();

            // position and heading arrow
            final Matrix matrix = new Matrix();
            matrix.setRotate(AngleUtils.normalize(currentHeading + map.getMapPosition().getBearing()), arrowWidthHalf, arrowHeightHalf);
            final Bitmap arrow = new AndroidBitmap(android.graphics.Bitmap.createBitmap(positionAndHeadingArrow, 0, 0, arrowWidth, arrowHeight, matrix, true));
            arrowLayer.removeAllItems();
            final MarkerItem item = new MarkerItem("current position", "", new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
            item.setMarker(new MarkerSymbol(arrow, MarkerSymbol.HotspotPlace.CENTER));
            arrowLayer.addItem(item);
        }
    }

    @Override
    protected void repaintHistory() {
        MAP_MAPSFORGE.clearGroup(LayerHelper.ZINDEX_HISTORY);
        repaintHistoryHelper((points) -> {
            if (points.size() < 2) {
                return; // no line can be drawn from a single point
            }
            final PathLayer historyLayer = new PathLayer(map, historyStyle);
            MAP_MAPSFORGE.addLayerToGroup(historyLayer, LayerHelper.ZINDEX_HISTORY);
            historyLayer.setPoints(points);
        });
    }

    @Override
    protected void repaintRouteAndTracks() {
        MAP_MAPSFORGE.clearGroup(LayerHelper.ZINDEX_TRACK_ROUTE);
        repaintRouteAndTracksHelper((segment, isTrack) -> {
            if (segment.size() < 2) {
                return; // no line can be drawn from a single point
            }
            final PathLayer segmentLayer = new PathLayer(map, isTrack ? trackStyle : routeStyle);
            MAP_MAPSFORGE.addLayerToGroup(segmentLayer, LayerHelper.ZINDEX_TRACK_ROUTE);
            segmentLayer.setPoints(segment);
        });
    }

}
