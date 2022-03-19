package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.models.Route;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Matrix;

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
    private final Style accuracyCircleStyle = Style.builder()
        .strokeWidth(1.0f)
        .strokeColor(MapLineUtils.getAccuracyCircleColor())
        .fillColor(MapLineUtils.getAccuracyCircleFillColor())
        .build();

    // position history
    private final List<PathLayer> historyLayers = new ArrayList<>();

    // individual route, routes & tracks
    private final List<PathLayer> trackLayers = new ArrayList<>();

    MapsforgePositionLayer(final Map map) {
        this.map = map;

        // position and heading arrow
        accuracyCircleLayer = new VectorLayer(map);
        map.layers().add(accuracyCircleLayer);
        arrowLayer = new ItemizedLayer(map, new MarkerSymbol(new AndroidBitmap(positionAndHeadingArrow), MarkerSymbol.HotspotPlace.CENTER));
        map.layers().add(arrowLayer);
        repaintPosition();
    }

    protected void destroyLayer(final Map map) {
        map.layers().remove(accuracyCircleLayer);
        map.layers().remove(arrowLayer);
        clearLayers(historyLayers);
        clearLayers(trackLayers);
    }

    private void clearLayers(final List<PathLayer> layers) {
        map.layers().removeAll(layers);
        layers.clear();
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
    };

    // ========================================================================
    // repaint methods

    @Override
    protected void repaintPosition() {
        if (currentLocation != null) {
            // accuracy circle
            final float accuracy = currentLocation.getAccuracy() / 1000.0f;
            accuracyCircleLayer.add(new CircleDrawable(new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()), accuracy, accuracyCircleStyle));
            accuracyCircleLayer.update();

            // position and heading arrow
            final Matrix matrix = new Matrix();
            matrix.setRotate(currentHeading, arrowWidthHalf, arrowHeightHalf);
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
        repaintHistoryHelper(GeoPoint::new, (points) -> {
            final PathLayer historyLayer = new PathLayer(map, MapLineUtils.getTrailColor(), MapLineUtils.getHistoryLineWidth());
            historyLayers.add(historyLayer);
            map.layers().add(historyLayer);
            historyLayer.setPoints(points);
        });
    };

    @Override
    protected void repaintRouteAndTracks() {
        clearLayers(trackLayers);
        repaintRouteAndTracksHelper((segment, isTrack) -> {
            final PathLayer trackLayer = new PathLayer(map, isTrack ? MapLineUtils.getTrackColor() : MapLineUtils.getRouteColor() , isTrack ? MapLineUtils.getTrackLineWidth() : MapLineUtils.getRouteLineWidth());
            trackLayers.add(trackLayer);
            map.layers().add(trackLayer);
            trackLayer.setPoints(segment);
        });
    };

}
