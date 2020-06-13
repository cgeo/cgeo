package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.DisplayUtils;

import androidx.core.util.Pair;

import java.util.ArrayList;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;

abstract class AbstractLineLayer extends Layer {
    protected float width;
    private Paint line = null;
    protected int lineColor = 0xD00000A0;
    protected boolean isHidden = false;

    // used for caching
    private ArrayList<Geopoint> track = null;
    private ArrayList<Pair<Integer, Integer>> pixelTrack = null;
    private long mapSize = 0;

    protected AbstractLineLayer() {
        width = DisplayUtils.getDefaultThinLineWidth();
    }

    public void updateTrack(final ArrayList<Geopoint> track) {
        this.track = new ArrayList<Geopoint>(track);
        this.pixelTrack = null;
        this.mapSize = 0;
    }

    public void setHidden(final boolean isHidden) {
        this.isHidden = isHidden;
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (isHidden) {
            return;
        }

        // no route or route too short?
        if (this.track == null || this.track.size() < 2) {
            return;
        }

        // still building cache?
        if (this.pixelTrack == null && this.mapSize > 0) {
            return;
        }

        final long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        if (this.pixelTrack == null || this.mapSize != mapSize) {
            translateRouteToPixels(mapSize);
        }

        prepareLine();
        for (int i = 1; i < pixelTrack.size(); i++) {
            final Pair<Integer, Integer> source = pixelTrack.get(i - 1);
            final Pair<Integer, Integer> destination = pixelTrack.get(i);
            canvas.drawLine(source.first - (int) topLeftPoint.x, source.second - (int) topLeftPoint.y, destination.first - (int) topLeftPoint.x, destination.second - (int) topLeftPoint.y, line);
        }
    }

    private void translateRouteToPixels(final long mapSize) {
        this.mapSize = mapSize;
        this.pixelTrack = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < track.size(); i++) {
            pixelTrack.add(translateToPixels(mapSize, this.track.get(i)));
        }
    }

    private void prepareLine() {
        if (line == null) {
            line = AndroidGraphicFactory.INSTANCE.createPaint();
            line.setStrokeWidth(width);
            line.setStyle(Style.STROKE);
            line.setColor(lineColor);
            line.setTextSize(20);
        }
    }

    private static Pair<Integer, Integer> translateToPixels(final long mapSize, final Geopoint coords) {
        final int posX = (int) (MercatorProjection.longitudeToPixelX(coords.getLongitude(), mapSize));
        final int posY = (int) (MercatorProjection.latitudeToPixelY(coords.getLatitude(), mapSize));
        return new Pair<>(posX, posY);
    }
}
