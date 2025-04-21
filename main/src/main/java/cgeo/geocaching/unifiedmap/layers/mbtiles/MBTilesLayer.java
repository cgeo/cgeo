package cgeo.geocaching.unifiedmap.layers.mbtiles;

/* based upon work of (c) 2023 Christian Pesch, https://github.com/cpesch */

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.model.common.Observer;

public class MBTilesLayer extends TileLayer<MBTilesRendererJob> implements Observer {
    private final MBTilesRenderer renderer;
    private MBTilesMapWorkerPool mapWorkerPool;

    public MBTilesLayer(final TileCache tileCache, final MapViewPosition mapViewPosition, final boolean isTransparent,
                        final MBTilesFile file, final GraphicFactory graphicFactory) {
        super(tileCache, mapViewPosition, graphicFactory.createMatrix(), isTransparent);
        this.renderer = new MBTilesRenderer(file, graphicFactory);
    }

    public synchronized void setDisplayModel(final DisplayModel displayModel) {
        super.setDisplayModel(displayModel);
        if (displayModel != null) {
            if (this.mapWorkerPool == null) {
                this.mapWorkerPool = new MBTilesMapWorkerPool(this.tileCache, this.jobQueue, this.renderer, this);
            }
            this.mapWorkerPool.start();
        } else if (this.mapWorkerPool != null) {
            // if we do not have a displayModel any more we can stop rendering.
            this.mapWorkerPool.stop();
        }
    }

    protected MBTilesRendererJob createJob(final Tile tile) {
        return new MBTilesRendererJob(tile, renderer, this.isTransparent);
    }

    /**
     * Whether the tile is stale and should be refreshed.
     * <br />
     * This method is called from {@link #draw(org.mapsforge.core.model.BoundingBox, byte, org.mapsforge.core.graphics.Canvas, org.mapsforge.core.model.Point)} to determine whether the tile needs to
     * be refreshed.
     * <br />
     * A tile is considered stale if the timestamp of the layer's {@link #renderer} is more recent than the
     * {@code bitmap}'s {@link org.mapsforge.core.graphics.TileBitmap#getTimestamp()}.
     * <br />
     * When a tile has become stale, the layer will first display the tile referenced by {@code bitmap} and attempt to
     * obtain a fresh copy in the background. When a fresh copy becomes available, the layer will replace is and update
     * the cache. If a fresh copy cannot be obtained for whatever reason, the stale tile will continue to be used until
     * another {@code #draw(BoundingBox, byte, Canvas, Point)} operation requests it again.
     *
     * @param tile   A tile.
     * @param bitmap The bitmap for {@code tile} currently held in the layer's cache.
     */
    @Override
    protected boolean isTileStale(final Tile tile, final TileBitmap bitmap) {
        return this.renderer.getDataTimestamp(tile) > bitmap.getTimestamp();
    }

    @Override
    protected void onAdd() {
        this.mapWorkerPool.start();
        if (tileCache != null) {
            tileCache.addObserver(this);
        }
        super.onAdd();
    }

    @Override
    protected void onRemove() {
        this.mapWorkerPool.stop();
        if (tileCache != null) {
            tileCache.removeObserver(this);
        }
        super.onRemove();
    }

    @Override
    public void onChange() {
        this.requestRedraw();
    }
}
