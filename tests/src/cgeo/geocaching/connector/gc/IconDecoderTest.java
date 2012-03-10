package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class IconDecoderTest extends AbstractResourceInstrumentationTestCase {

    public void testparseMapPNG14() {
        final Bitmap bitmap = getBitmap(R.raw.tile14);
        Log.d(Settings.tag, "Bitmap=" + bitmap.getWidth() + "x" + bitmap.getHeight());

        assertEquals(CacheType.TRADITIONAL, parse14(bitmap, 97 / 4, 136 / 4).getType());
        assertEquals(CacheType.MYSTERY, parse14(bitmap, 226 / 4, 104 / 4).getType());
        assertEquals(CacheType.MULTI, parse14(bitmap, 54 / 4, 97 / 4).getType());
        assertTrue(parse14(bitmap, 119 / 4, 108 / 4).isFound());
    }

    private Bitmap getBitmap(int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeStream(getInstrumentation().getContext().getResources().openRawResource(resourceId));
        assert bitmap.getWidth() == Tile.TILE_SIZE : "Wrong size";
        return bitmap;
    }

    private static cgCache parse14(Bitmap bitmap, int x, int y) {
        final cgCache cache = new cgCache();
        IconDecoder.parseMapPNG14(cache, bitmap, new UTFGridPosition(x, y));
        return cache;
    }

    public void testParseMap13() {
        final Bitmap bitmap = getBitmap(R.raw.tile13);

        assertEquals(CacheType.TRADITIONAL, parse13(bitmap, 146 / 4, 225 / 4).getType());
        assertEquals(CacheType.MYSTERY, parse13(bitmap, 181 / 4, 116 / 4).getType());
        assertEquals(CacheType.MULTI, parse13(bitmap, 118 / 4, 230 / 4).getType());
    }

    private static cgCache parse13(Bitmap bitmap, int x, int y) {
        final cgCache cache = new cgCache();
        IconDecoder.parseMapPNG13(cache, bitmap, new UTFGridPosition(x, y));
        return cache;
    }
}
