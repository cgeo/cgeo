package cgeo.geocaching.maps.mapsforge.v6.layers;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

/**
 * A {@code Polygon} draws a closed connected series of line segments specified by a list of {@link LatLong LatLongs}.
 * If the first and the last {@code LatLong} are not equal, the {@code Polygon} will be closed automatically.
 * <p/>
 * A {@code Polygon} holds two {@link Paint} objects to allow for different outline and filling. These paints define
 * drawing parameters such as color, stroke width, pattern and transparency.
 *
 * This class is a copy of {@link org.mapsforge.map.layer.overlay.Polygon} with added capability to display holes.
 */
public class Polygon extends Layer {

    private BoundingBox boundingBox;
    private final GraphicFactory graphicFactory;
    private final boolean keepAligned;
    private final List<LatLong> latLongs = new CopyOnWriteArrayList<>();

    private final List<List<LatLong>> holes = new CopyOnWriteArrayList<>();
    private Paint paintFill;
    private Paint paintStroke;

    /**
     * @param paintFill      the initial {@code Paint} used to fill this polygon (may be null).
     * @param paintStroke    the initial {@code Paint} used to stroke this polygon (may be null).
     * @param graphicFactory the GraphicFactory
     */
    public Polygon(final Paint paintFill, final Paint paintStroke, final GraphicFactory graphicFactory) {
        this(paintFill, paintStroke, graphicFactory, false);
    }

    /**
     * @param paintFill      the initial {@code Paint} used to fill this polygon (may be null).
     * @param paintStroke    the initial {@code Paint} used to stroke this polygon (may be null).
     * @param graphicFactory the GraphicFactory
     * @param keepAligned    if set to true it will keep the bitmap aligned with the map,
     *                       to avoid a moving effect of a bitmap shader.
     */
    public Polygon(final Paint paintFill, final Paint paintStroke, final GraphicFactory graphicFactory, final boolean keepAligned) {
        super();
        this.keepAligned = keepAligned;
        this.paintFill = paintFill;
        this.paintStroke = paintStroke;
        this.graphicFactory = graphicFactory;
    }

    public synchronized void addPoint(final LatLong point) {
        this.latLongs.add(point);
        updatePoints();
    }

    public synchronized void addPoints(final List<LatLong> points) {
        this.latLongs.addAll(points);
        updatePoints();
    }

    public synchronized  void addHole(final List<LatLong> hole) {
        this.holes.add(hole);
        updatePoints();
    }

    public synchronized void clear() {
        this.latLongs.clear();
        this.holes.clear();
        updatePoints();
    }

    public synchronized boolean contains(final LatLong tapLatLong) {
        return LatLongUtils.contains(latLongs, tapLatLong);
    }

    @SuppressWarnings({"PMD.NPathComplexity"}) // methods readability won't profit from further splitting and deviate too much from mapsforge source
    @Override
    public synchronized void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        if (this.latLongs.size() < 2 || (this.paintStroke == null && this.paintFill == null)) {
            return;
        }

        if (this.boundingBox != null && !this.boundingBox.intersects(boundingBox)) {
            return;
        }

        final Path path = this.graphicFactory.createPath();
        final long mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());

        addToPath(path, this.latLongs, mapSize, topLeftPoint);

        for (List<LatLong> hole : holes) {
            addToPath(path, hole, mapSize, topLeftPoint);
        }

        if (this.paintStroke != null) {
            if (this.keepAligned) {
                this.paintStroke.setBitmapShaderShift(topLeftPoint);
            }
            canvas.drawPath(path, this.paintStroke);
        }
        if (this.paintFill != null) {
            if (this.keepAligned) {
                this.paintFill.setBitmapShaderShift(topLeftPoint);
            }

            canvas.drawPath(path, this.paintFill);
        }
    }

    private static void addToPath(final Path path, final List<LatLong> points, final long mapSize, final Point topLeftPoint) {
        final Iterator<LatLong> iterator = points.iterator();
        LatLong latLong = iterator.next();
        float x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
        float y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);
        path.moveTo(x, y);

        while (iterator.hasNext()) {
            latLong = iterator.next();
            x = (float) (MercatorProjection.longitudeToPixelX(latLong.longitude, mapSize) - topLeftPoint.x);
            y = (float) (MercatorProjection.latitudeToPixelY(latLong.latitude, mapSize) - topLeftPoint.y);
            path.lineTo(x, y);
        }
        path.close();
    }

    /**
     * @return a thread-safe list of LatLongs in this polygon.
     */
    public List<LatLong> getLatLongs() {
        return this.latLongs;
    }

    /**
     * @return the {@code Paint} used to fill this polygon (may be null).
     */
    public synchronized Paint getPaintFill() {
        return this.paintFill;
    }

    /**
     * @return the {@code Paint} used to stroke this polygon (may be null).
     */
    public synchronized Paint getPaintStroke() {
        return this.paintStroke;
    }

    /**
     * @return true if it keeps the bitmap aligned with the map, to avoid a
     * moving effect of a bitmap shader, false otherwise.
     */
    public boolean isKeepAligned() {
        return keepAligned;
    }

    /**
     * @param paintFill the new {@code Paint} used to fill this polygon (may be null).
     */
    public synchronized void setPaintFill(final Paint paintFill) {
        this.paintFill = paintFill;
    }

    /**
     * @param paintStroke the new {@code Paint} used to stroke this polygon (may be null).
     */
    public synchronized void setPaintStroke(final Paint paintStroke) {
        this.paintStroke = paintStroke;
    }

    public synchronized void setPoints(final List<LatLong> points) {
        this.latLongs.clear();
        this.latLongs.addAll(points);
        updatePoints();
    }

    private void updatePoints() {
        this.boundingBox = this.latLongs.isEmpty() ? null : new BoundingBox(this.latLongs);
    }
}
