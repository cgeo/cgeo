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

        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 97, 136, 14).getType());
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 226, 104, 14).getType());
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 54, 97, 14).getType());
        assertTrue(parseMapPNG(bitmap, 119, 108, 14).isFound());
    }

    private Bitmap getBitmap(int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeStream(getInstrumentation().getContext().getResources().openRawResource(resourceId));
        assert bitmap.getWidth() == Tile.TILE_SIZE : "Wrong size";
        return bitmap;
    }

    private static cgCache parseMapPNG(Bitmap bitmap, int x, int y, int zoomlevel) {
        final cgCache cache = new cgCache();
        IconDecoder.parseMapPNG(cache, bitmap, new UTFGridPosition(x / 4, y / 4), zoomlevel);
        return cache;
    }

    public void testParseMap13() {
        final Bitmap bitmap = getBitmap(R.raw.tile13);

        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 146, 225, 13).getType());
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 181, 116, 13).getType());
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 118, 230, 13).getType());
    }

    public void testParseMap12() {
        final Bitmap bitmap = getBitmap(R.raw.tile12);

        int multi = 0;
        multi = parseMapPNG(bitmap, 130, 92, 12).getType() == CacheType.MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 93, 222, 12).getType() == CacheType.MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 129, 227, 12).getType() == CacheType.MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 234, 170, 12).getType() == CacheType.MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 195, 113, 12).getType() == CacheType.MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 195, 124, 12).getType() == CacheType.MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 111, 74, 12).getType() == CacheType.MULTI ? multi + 1 : multi;

        int mystery = 0;
        mystery = parseMapPNG(bitmap, 37, 25, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 49, 183, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 183, 181, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 176, 94, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 161, 124, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 168, 118, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 231, 114, 12).getType() == CacheType.MYSTERY ? mystery + 1 : mystery;

        int tradi = 0;
        tradi = parseMapPNG(bitmap, 179, 27, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 106, 93, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 145, 147, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 204, 163, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 9, 146, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 117, 225, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 90, 107, 12).getType() == CacheType.TRADITIONAL ? tradi + 1 : tradi;

        assertEquals(7, multi);
        assertEquals(7, mystery);
        assertEquals(7, tradi);

    }
}
