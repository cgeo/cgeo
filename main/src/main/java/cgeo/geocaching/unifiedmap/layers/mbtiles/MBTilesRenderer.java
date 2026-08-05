package cgeo.geocaching.unifiedmap.layers.mbtiles;

/* based upon work of (c) 2023 Christian Pesch, https://github.com/cpesch */

import cgeo.geocaching.utils.Log;

import java.io.InputStream;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;

public class MBTilesRenderer {

    private final MBTilesFile file;
    private final GraphicFactory graphicFactory;
    private final long timestamp;

    MBTilesRenderer(final MBTilesFile file, final GraphicFactory graphicFactory) {
        this.file = file;
        this.graphicFactory = graphicFactory;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Called when a job needs to be executed.
     *
     * @param rendererJob the job that should be executed.
     */
    public TileBitmap executeJob(final MBTilesRendererJob rendererJob) {

        try {
            final InputStream inputStream = file.getTileAsBytes(rendererJob.tile.tileX, rendererJob.tile.tileY, rendererJob.tile.zoomLevel);

            final TileBitmap bitmap;
            if (inputStream == null) {
                bitmap = graphicFactory.createTileBitmap(rendererJob.tile.tileSize, rendererJob.hasAlpha);
            } else {
                bitmap = graphicFactory.createTileBitmap(inputStream, rendererJob.tile.tileSize, rendererJob.hasAlpha);
                bitmap.scaleTo(rendererJob.tile.tileSize, rendererJob.tile.tileSize);
            }
            bitmap.setTimestamp(rendererJob.getDatabaseRenderer().getDataTimestamp(rendererJob.tile));
            return bitmap;
        } catch (Exception e) {
            Log.e("Error while rendering job " + rendererJob + ": " + e.getMessage());
            return null;
        }
    }

    public long getDataTimestamp(final Tile tile) {
        return timestamp;
    }

}
