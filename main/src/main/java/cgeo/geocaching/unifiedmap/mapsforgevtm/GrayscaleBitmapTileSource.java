package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

/**
 * A raster tile source that hands out grey tiles.
 *
 * <p>Raster tiles arrive already rendered, so unlike a vector map they cannot be greyed through
 * the render theme - the pixels have to be converted after decoding. Markers and routes are drawn
 * by other layers and keep their colours.</p>
 */
public class GrayscaleBitmapTileSource extends BitmapTileSource {

    public GrayscaleBitmapTileSource(final String url, final String tilePath, final int zoomMin, final int zoomMax) {
        super(url, tilePath, zoomMin, zoomMax);
    }

    @Override
    public ITileDataSource getDataSource() {
        return new UrlTileDataSource(this, new GrayscaleTileDecoder(), getHttpEngine());
    }

    private static class GrayscaleTileDecoder implements ITileDecoder {

        @Override
        public boolean decode(final Tile tile, final ITileDataSink sink, final InputStream is) throws IOException {
            final Bitmap decoded = BitmapFactory.decodeStream(is);
            if (decoded == null) {
                Log.w("Could not decode tile " + tile);
                return false;
            }
            sink.setTileImage(new AndroidBitmap(toGrayscale(decoded)));
            // deliberately no sink.completed() here - UrlTileDataSource.query() completes the
            // tile itself based on what this returns, and completing twice releases the tile
            // and then acts on it again
            return true;
        }
    }

    /** draws the tile through a saturation-zero colour filter */
    private static Bitmap toGrayscale(final Bitmap source) {
        final Bitmap gray = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        final ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        final Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        new Canvas(gray).drawBitmap(source, 0, 0, paint);
        source.recycle();
        return gray;
    }
}
