package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.Matrix;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.CircleDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;

class MapsforgePositionLayer extends AbstractPositionLayer {

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

    MapsforgePositionLayer(final Map map) {
        accuracyCircleLayer = new VectorLayer(map);
        map.layers().add(accuracyCircleLayer);
        arrowLayer = new ItemizedLayer(map, new MarkerSymbol(new AndroidBitmap(positionAndHeadingArrow), MarkerSymbol.HotspotPlace.CENTER));
        map.layers().add(arrowLayer);
        repaintArrow();
    }

    @Override
    protected void repaintArrow() {
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
    protected void repaintPosition() {
        // @todo
    };

    @Override
    protected void repaintHistory() {
        // @todo
    };

    @Override
    protected void repaintRouteAndTracks() {
        // @todo
    };

    protected void destroyLayer(final Map map) {
        map.layers().remove(arrowLayer);
    }
}
