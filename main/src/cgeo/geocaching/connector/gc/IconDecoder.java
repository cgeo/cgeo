package cgeo.geocaching.connector.gc;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

import android.graphics.Bitmap;

/**
 * icon decoder for cache icons
 * 
 */
public abstract class IconDecoder {

    private static final int[] OFFSET_X = new int[] { 0, -1, -1, 0, 1, 1, 1, 0, -1, -2, -2, -2, -2, -1, 0, 1, 2, 2, 2, 2, 2, 1, 0, -1, -2 };
    private static final int[] OFFSET_Y = new int[] { 0, 0, 1, 1, 1, 0, -1, -1, -1, -1, 0, 1, 2, 2, 2, 2, 2, 1, 0, -1, -2, -2, -2, -2, -2 };

    /**
     * The icon decoder walks a spiral around the center pixel position of the cache
     * and searches for characteristic colors.
     *
     * @param cache
     * @param bitmap
     * @param xy
     */
    public static void parseMapPNG13(final cgCache cache, Bitmap bitmap, UTFGridPosition xy) {
        final int xCenter = xy.getX() * 4 + 2;
        final int yCenter = xy.getY() * 4 + 2;
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        int countMulti = 0;
        int countFound = 0;

        for (int i = 0; i < OFFSET_X.length; i++) {

            // assert that we are still in the tile
            final int x = xCenter + OFFSET_X[i];
            if (x < 0 || x >= bitmapWidth) {
                continue;
            }

            final int y = yCenter + OFFSET_Y[i];
            if (y < 0 || y >= bitmapHeight) {
                continue;
            }

            int color = bitmap.getPixel(x, y) & 0x00FFFFFF;

            // transparent pixels are not interesting
            if (color == 0) {
                continue;
            }

            int red = (color & 0xFF0000) >> 16;
            int green = (color & 0xFF00) >> 8;
            int blue = color & 0xFF;

            // these are quite sure, so one pixel is enough for matching
            if (green > 0x80 && green > red && green > blue) {
                cache.setType(CacheType.TRADITIONAL);
                return;
            }
            if (blue > 0x80 && blue > red && blue > green) {
                cache.setType(CacheType.MYSTERY);
                return;
            }
            if (red > 0x90 && blue < 0x10 && green < 0x10) {
                cache.setType(CacheType.EVENT);
                return;
            }

            // next two are hard to distinguish, therefore we sample all pixels of the spiral
            if (red > 0xFA && green > 0xD0) {
                countMulti++;
            }
            if (red < 0xF3 && red > 0xa0 && green > 0x20 && blue < 0x80) {
                countFound++;
            }
        }

        // now check whether we are sure about found/multi
        if (countFound > countMulti && countFound >= 2) {
            cache.setFound(true);
        }
        if (countMulti > countFound && countMulti >= 5) {
            cache.setType(CacheType.MULTI);
        }
    }

    // Pixel colors in tile
    private final static int COLOR_BORDER_GRAY = 0x5F5F5F;
    private final static int COLOR_TRADITIONAL = 0x316013;
    private final static int COLOR_MYSTERY = 0x243C97;
    private final static int COLOR_MULTI = 0xFFDE19;
    private final static int COLOR_FOUND = 0xFBEA5D;

    // Offset inside cache icon
    private final static int POSX_TRADI = 7;
    private final static int POSY_TRADI = -12;
    private final static int POSX_MULTI = 5; // for orange 8
    private final static int POSY_MULTI = -9; // for orange 10
    private final static int POSX_MYSTERY = 5;
    private final static int POSY_MYSTERY = -13;
    private final static int POSX_FOUND = 10;
    private final static int POSY_FOUND = -8;

    /**
     * For level 14 find the borders of the icons and then use a single pixel and color to match.
     *
     * @param cache
     * @param bitmap
     * @param xy
     */
    public static void parseMapPNG14(cgCache cache, Bitmap bitmap, UTFGridPosition xy) {
        int x = xy.getX() * 4 + 2;
        int y = xy.getY() * 4 + 2;

        // search for left border
        int countX = 0;
        while ((bitmap.getPixel(x, y) & 0x00FFFFFF) != COLOR_BORDER_GRAY) {
            if (--x < 0 || ++countX > 20) {
                return;
            }
        }
        // search for bottom border
        int countY = 0;
        while ((bitmap.getPixel(x, y) & 0x00FFFFFF) != 0x000000) {
            if (++y >= Tile.TILE_SIZE || ++countY > 20) {
                return;
            }
        }

        try {
            if ((bitmap.getPixel(x + POSX_TRADI, y + POSY_TRADI) & 0x00FFFFFF) == COLOR_TRADITIONAL) {
                cache.setType(CacheType.TRADITIONAL);
                return;
            }
            if ((bitmap.getPixel(x + POSX_MYSTERY, y + POSY_MYSTERY) & 0x00FFFFFF) == COLOR_MYSTERY) {
                cache.setType(CacheType.MYSTERY);
                return;
            }
            if ((bitmap.getPixel(x + POSX_MULTI, y + POSY_MULTI) & 0x00FFFFFF) == COLOR_MULTI) {
                cache.setType(CacheType.MULTI);
                return;
            }
            if ((bitmap.getPixel(x + POSX_FOUND, y + POSY_FOUND) & 0x00FFFFFF) == COLOR_FOUND) {
                cache.setFound(true);
                return;
            }
        } catch (IllegalArgumentException e) {
            // intentionally left blank
        }

        return;
    }
}
