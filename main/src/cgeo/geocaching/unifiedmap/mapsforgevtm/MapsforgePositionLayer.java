package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_MAPSFORGE;

import android.graphics.Matrix;
import android.location.Location;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

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
    private final List<PathLayer> historyLayers = new ArrayList<>();

    // individual route, routes & tracks
    private final PathLayer navigationLayer;
    private final List<PathLayer> trackLayers = new ArrayList<>();

    MapsforgePositionLayer(final Map map, final View root) {
        super(root, GeoPoint::new);
        this.map = map;

        // position and heading arrow
        accuracyCircleLayer = new VectorLayer(map);
        map.layers().add(accuracyCircleLayer);
        arrowLayer = new ItemizedLayer(map, new MarkerSymbol(new AndroidBitmap(positionAndHeadingArrow), MarkerSymbol.HotspotPlace.CENTER));
        map.layers().add(arrowLayer);
        repaintPosition();

        // navigation
        navigationLayer = new PathLayer(map, MapLineUtils.getDirectionColor(), MapLineUtils.getDirectionLineWidth(true));
        map.layers().add(navigationLayer);
    }

    protected void destroyLayer(final Map map) {
        map.layers().remove(accuracyCircleLayer);
        map.layers().remove(arrowLayer);
        clearLayers(historyLayers);
        clearLayers(trackLayers);
        map.layers().remove(navigationLayer);
    }

    private void clearLayers(final List<PathLayer> layers) {
        map.layers().removeAll(layers);
        layers.clear();
    }

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        setCurrentPositionAndHeadingHelper(location, heading, navigationLayer::setPoints, MAP_MAPSFORGE);
    }

    // ========================================================================
    // route / track handling

    @Override
    public void updateIndividualRoute(final Route route) {
        super.updateIndividualRoute(route, Route::getAllPointsGeoPoint);
    }

    @Override
    public void updateTrack(final String key, final Route track) {
        super.updateTrack(key, track, Route::getAllPointsGeoPoint);
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
        clearLayers(historyLayers);
        repaintHistoryHelper((points) -> {
            final PathLayer historyLayer = new PathLayer(map, MapLineUtils.getTrailColor(), MapLineUtils.getHistoryLineWidth(true));
            historyLayers.add(historyLayer);
            map.layers().add(historyLayer);
            historyLayer.setPoints(points);
        });
    }

    @Override
    protected void repaintRouteAndTracks() {
        clearLayers(trackLayers);
        repaintRouteAndTracksHelper((segment, isTrack) -> {
            final PathLayer trackLayer = new PathLayer(map, isTrack ? MapLineUtils.getTrackColor() : MapLineUtils.getRouteColor(), isTrack ? MapLineUtils.getTrackLineWidth(true) : MapLineUtils.getRouteLineWidth(true));
            trackLayers.add(trackLayer);
            map.layers().add(trackLayer);
            trackLayer.setPoints(segment);
        });
    }

}
