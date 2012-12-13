package cgeo.geocaching.connector.gc;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

import android.graphics.Bitmap;

/**
 * icon decoder for cache icons
 *
 */
public abstract class IconDecoder {

    public static boolean parseMapPNG(final cgCache cache, Bitmap bitmap, UTFGridPosition xy, int zoomlevel) {
        if (zoomlevel >= 14) {
            return parseMapPNG14(cache, bitmap, xy);
        }
        if (zoomlevel <= 11) {
            return parseMapPNG11(cache, bitmap, xy);
        }
        return parseMapPNG13(cache, bitmap, xy);
    }

    public static int CT_TRADITIONAL = 0;
    public static int CT_MULTI = 1;
    public static int CT_MYSTERY = 2;
    public static int CT_EVENT = 3;
    public static int CT_VIRTUAL = 4;
    public static int CT_FOUND = 5;
    public static int CT_OWN = 6;
    public static int CT_MEGAEVENT = 7;
    public static int CT_CITO = 8;
    public static int CT_WEBCAM = 9;
    public static int CT_WHEREIGO = 10;
    public static int CT_EARTH = 11;
    public static int CT_LETTERBOX = 12;

    /**
     * The icon decoder over all 16 pixels of image . It should not be invoked on any part of image that might overlay
     * with other caches.
     * Is uses decision tree to determine right type.
     *
     * @param cache
     * @param bitmap
     * @param xy
     * @return true if parsing was successful
     */
    private static boolean parseMapPNG13(final cgCache cache, Bitmap bitmap, UTFGridPosition xy) {
        final int topX = xy.getX() * 4;
        final int topY = xy.getY() * 4;
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        int[] pngType = new int[7];

        if ((topX < 0) || (topY < 0) || (topX + 4 > bitmapWidth) || (topY + 4 > bitmapHeight)) {
            return false; //out of image position
        }

        for (int x = topX; x < topX + 4; x++) {
            for (int y = topY; y < topY + 4; y++) {
                int color = bitmap.getPixel(x, y);

                if ((color & 0xFFFFFF) == 0x5f5f5f) {
                    continue; //Border in every icon is the same and therefore no use to us
                }
                if ((color >>> 24) != 255) {
                    continue; //transparent pixels (or semi_transparent) are only shadows of border
                }

                int red = (color & 0xFF0000) >> 16;
                int green = (color & 0xFF00) >> 8;
                int blue = color & 0xFF;

                int type = getCacheTypeFromPixel13(red, green, blue);
                pngType[type]++;
            }
        }

        int type = -1;
        int count = 0;

        for (int x = 0; x < 7; x++)
        {
            if (pngType[x] > count) {
                count = pngType[x];
                type = x;
            }
        }

        if (count > 1) { // 2 pixels need to detect same type and we say good to go
            switch (type) {
                case 0:
                    cache.setType(CacheType.TRADITIONAL);
                    return true;
                case 1:
                    cache.setType(CacheType.MULTI);
                    return true;
                case 2:
                    cache.setType(CacheType.MYSTERY); //mystery, whereigo, groundspeak HQ and mystery is most common
                    return true;
                case 3:
                    cache.setType(CacheType.EVENT); //event, cito, mega-event and event is most common
                    return true;
                case 4:
                    cache.setType(CacheType.EARTH); //It's an image of ghost (webcam, earth, virtual) and earth in most common
                    return true;
                case 5:
                    cache.setFound(true);
                    return true;
                case 6:
                    cache.setOwn(true);
                    return true;
            }
        }
        return false;
    }

    /**
     * The icon decoder over all 16 pixels of image . It should not be invoked on any part of image that might overlay
     * with other caches.
     * Is uses decision tree to determine right type.
     *
     * @param cache
     * @param bitmap
     * @param xy
     * @return true if parsing was successful
     */
    private static boolean parseMapPNG11(final cgCache cache, Bitmap bitmap, UTFGridPosition xy) {
        final int topX = xy.getX() * 4;
        final int topY = xy.getY() * 4;
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        int[] pngType = new int[5];

        if ((topX < 0) || (topY < 0) || (topX + 4 > bitmapWidth) || (topY + 4 > bitmapHeight)) {
            return false; //out of image position
        }

        for (int x = topX; x < topX + 4; x++) {
            for (int y = topY; y < topY + 4; y++) {
                int color = bitmap.getPixel(x, y);


                if ((color >>> 24) != 255) {
                    continue; //transparent pixels (or semi_transparent) are only shadows of border
                }

                int r = (color & 0xFF0000) >> 16;
                int g = (color & 0xFF00) >> 8;
                int b = color & 0xFF;

                //Duplicate colors does not add any value
                if (((r == 52) && (g == 52) && (b == 52)) ||
                        ((r == 69) && (g == 69) && (b == 69)) ||
                        ((r == 90) && (g == 90) && (b == 90)) ||
                        ((r == 233) && (g == 233) && (b == 234)) ||
                        ((r == 255) && (g == 255) && (b == 255))) {
                    continue;
                }

                int type = getCacheTypeFromPixel11(r, g, b);
                pngType[type]++;
            }
        }

        int type = -1;
        int count = 0;

        for (int x = 0; x < 5; x++)
        {
            if (pngType[x] > count) {
                count = pngType[x];
                type = x;
            }
        }

        if (count > 1) { // 2 pixels need to detect same type and we say good to go
            switch (type) {
                case 0:
                    cache.setType(CacheType.TRADITIONAL);
                    return true;
                case 1:
                    cache.setType(CacheType.MULTI);
                    return true;
                case 2:
                    cache.setType(CacheType.MYSTERY); //mystery, whereigo, groundspeak HQ and mystery is most common
                    return true;
                case 3:
                    cache.setType(CacheType.EVENT); //event, cito, mega-event and event is most common
                    return true;
                case 4:
                    cache.setType(CacheType.EARTH); //webcam, earth, virtual and earth in most common
                    return true;
            }
        }
        return false;
    }


    /**
     * For level 14 find the borders of the icons and then use a single pixel and color to match.
     *
     * @param cache
     * @param bitmap
     * @param xy
     */
    private static boolean parseMapPNG14(cgCache cache, Bitmap bitmap, UTFGridPosition xy) {
        final int topX = xy.getX() * 4;
        final int topY = xy.getY() * 4;
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        int[] pngType = new int[13];

        if ((topX < 0) || (topY < 0) || (topX + 4 > bitmapWidth) || (topY + 4 > bitmapHeight)) {
            return false; //out of image position
        }

        for (int x = topX; x < topX + 4; x++) {
            for (int y = topY; y < topY + 4; y++) {
                int color = bitmap.getPixel(x, y);

                if ((color & 0xFFFFFF) == 0x5f5f5f) {
                    continue; //Border in every icon is the same and therefore no use to us
                }
                if ((color >>> 24) != 255) {
                    continue; //transparent pixels (or semi_transparent) are only shadows of border
                }

                int r = (color & 0xFF0000) >> 16;
                int g = (color & 0xFF00) >> 8;
                int b = color & 0xFF;

                //Duplicate colors does not add any value
                if (((r == 216) && (g == 216) && (b == 216)) ||
                        ((r == 23) && (g == 23) && (b == 23)) ||
                        ((r == 240) && (g == 240) && (b == 240)) ||
                        ((r == 44) && (g == 44) && (b == 44)) ||
                        ((r == 228) && (g == 228) && (b == 228)) ||
                        ((r == 225) && (g == 225) && (b == 225)) ||
                        ((r == 199) && (g == 199) && (b == 199)) ||
                        ((r == 161) && (g == 161) && (b == 161)) ||
                        ((r == 8) && (g == 8) && (b == 8)) ||
                        ((r == 200) && (g == 200) && (b == 200)) ||
                        ((r == 255) && (g == 255) && (b == 255)) ||
                        ((r == 250) && (g == 250) && (b == 250)) ||
                        ((r == 95) && (g == 95) && (b == 95)) ||
                        ((r == 236) && (g == 236) && (b == 236)) ||
                        ((r == 215) && (g == 215) && (b == 215)) ||
                        ((r == 232) && (g == 232) && (b == 232)) ||
                        ((r == 217) && (g == 217) && (b == 217)) ||
                        ((r == 0) && (g == 0) && (b == 0)) ||
                        ((r == 167) && (g == 167) && (b == 167)) ||
                        ((r == 247) && (g == 247) && (b == 247)) ||
                        ((r == 144) && (g == 144) && (b == 144)) ||
                        ((r == 231) && (g == 231) && (b == 231)) ||
                        ((r == 248) && (g == 248) && (b == 248))) {
                    continue;
                }

                int type = getCacheTypeFromPixel14(r, g, b);
                pngType[type]++;
            }
        }

        int type = -1;
        int count = 0;

        for (int x = 0; x < 7; x++)
        {
            if (pngType[x] > count) {
                count = pngType[x];
                type = x;
            }
        }
        /*
         * public static int CT_MEGAEVENT = 7;
         * public static int CT_CITO = 8;
         * public static int CT_WEBCAM = 9;
         * public static int CT_WHEREIGO = 10;
         * public static int CT_EARTH = 11;
         * public static int CT_LETTERBOX = 12;
         */
        if (count > 1) { // 2 pixels need to detect same type and we say good to go
            switch (type) {
                case 0:
                    cache.setType(CacheType.TRADITIONAL);
                    return true;
                case 1:
                    cache.setType(CacheType.MULTI);
                    return true;
                case 2:
                    cache.setType(CacheType.MYSTERY);
                    return true;
                case 3:
                    cache.setType(CacheType.EVENT);
                    return true;
                case 4:
                    cache.setType(CacheType.VIRTUAL);
                    return true;
                case 5:
                    cache.setFound(true);
                    return true;
                case 6:
                    cache.setOwn(true);
                    return true;
                case 7:
                    cache.setType(CacheType.MEGA_EVENT);
                    return true;
                case 8:
                    cache.setType(CacheType.CITO);
                    return true;
                case 9:
                    cache.setType(CacheType.WEBCAM);
                    return true;
                case 10:
                    cache.setType(CacheType.WHERIGO);
                    return true;
                case 11:
                    cache.setType(CacheType.EARTH);
                    return true;
                case 12:
                    cache.setType(CacheType.LETTERBOX);
                    return true;
            }
        }
        return false;

    }

    /**
     * This method returns detected type from specific pixel from geocaching.com live map.
     * It was constructed based on classification tree made by Orange (http://orange.biolab.si/)
     * Input file was made from every non-transparent pixel of every possible "middle" cache icon from GC map
     *
     * @param r
     *            Red component of pixel (from 0 - 255)
     * @param g
     *            Green component of pixel (from 0 - 255)
     * @param b
     *            Blue component of pixel (from 0 - 255)
     * @return Value from 0 to 6 representing detected type or state of the cache.
     */
    private static int getCacheTypeFromPixel13(int r, int g, int b) {
        if (g < 110) {
            if (r > 87) {
                return ((g > 73) && (b < 63)) ? CT_FOUND : CT_EVENT;
            }
            return CT_MYSTERY;
        }
        if (b > 137) {
            if ((r < 184) && (g > 190)) {
                return CT_TRADITIONAL;
            }
            if ((r < 184) && (g < 191) && (r < 136)) {
                return CT_MYSTERY;
            }
            return CT_VIRTUAL;
        }
        if (r < 158) {
            return ((r > 129) && (r < 153)) ? CT_FOUND : CT_TRADITIONAL;
        }
        if (b > 33) {
            if (b > 57) {
                if (b > 100) {
                    return (r > 229) ? CT_MULTI : CT_EVENT;
                }
                return ((r > 254) && (g < 236)) ? CT_MULTI : CT_FOUND;
            }
            if ((g > 173) && ((g < 224))) {
                return ((r < 243) && (r > 223)) ? CT_FOUND : CT_OWN;
            }
            return CT_FOUND;
        }
        return CT_MULTI;
    }

    /**
     * This method returns detected type from specific pixel from geocaching.com live map level 14 or higher.
     * It was constructed based on classification tree made by Orange (http://orange.biolab.si/)
     * Input file was made from every non-transparent pixel of every possible "full" cache icon from GC map
     *
     * @param r
     *            Red component of pixel (from 0 - 255)
     * @param g
     *            Green component of pixel (from 0 - 255)
     * @param b
     *            Blue component of pixel (from 0 - 255)
     * @return Value from 0 to 6 representing detected type or state of the cache.
     */
    private static int getCacheTypeFromPixel14(int r, int g, int b) {
        if (b < 140) {
            if (r > 155) {
                if (g < 159) {
                    if (r < 173) {
                        return (r > 161) ? CT_MEGAEVENT : CT_OWN;
                    }
                    if (r < 206) {
                        if (b > 49) {
                            return (b > 83) ? CT_EVENT : CT_FOUND;
                        }
                        return (b < 31) ? CT_EARTH : CT_FOUND;
                    }
                    return (r < 221) ? CT_FOUND : CT_MULTI;
                }
                if (r > 210) {
                    if (g < 188) {
                        return CT_FOUND;
                    }
                    if (r < 246) {
                        return CT_OWN;
                    }
                    if (r < 254) {
                        return CT_FOUND;
                    }
                    if (r < 255) {
                        return CT_EVENT;
                    }
                    if (g < 208) {
                        return CT_EARTH;
                    }
                    if (g > 225) {
                        return CT_EARTH;
                    }
                    return (b < 36) ? CT_MULTI : CT_OWN;
                }
                return (b < 66) ? CT_OWN : CT_EARTH;
            }
            if (r < 63) {
                if (b > 26) {
                    if (b < 29) {
                        return CT_WEBCAM;
                    }
                    if (g > 102) {
                        return CT_CITO;
                    }
                    return (r < 26) ? CT_CITO : CT_WEBCAM;
                }
                if (g < 38) {
                    return CT_WEBCAM;
                }
                return (r < 41) ? CT_EARTH : CT_TRADITIONAL;
            }
            if (b < 119) {
                if (g < 81) {
                    return CT_WEBCAM;
                }
                if (b < 90) {
                    return CT_OWN;
                }
                return (r < 104) ? CT_WEBCAM : CT_OWN;
            }
            if (r < 132) {
                return (b < 124) ? CT_MULTI : CT_WHEREIGO;
            }
            if (g > 164) {
                return CT_TRADITIONAL;
            }
            if (b < 134) {
                return CT_OWN;
            }
            return (b > 137) ? CT_OWN : CT_WHEREIGO;
        }
        if (b < 245) {
            if (r < 180) {
                if (b < 218) {
                    if (g < 71) {
                        return CT_MYSTERY;
                    }
                    if (r < 96) {
                        return CT_WHEREIGO;
                    }
                    if (b > 165) {
                        return CT_WHEREIGO;
                    }
                    if (r < 153) {
                        return CT_WHEREIGO;
                    }
                    if (r < 160) {
                        return CT_WEBCAM;
                    }
                    return (r < 162) ? CT_WHEREIGO : CT_WEBCAM;
                }
                return (r < 158) ? CT_MEGAEVENT : CT_EARTH;
            }
            if (g > 232) {
                if (g > 247) {
                    return CT_CITO;
                }
                if (r < 237) {
                    return CT_OWN;
                }
                if (g < 238) {
                    return CT_OWN;
                }
                if (r > 243) {
                    return CT_WEBCAM;
                }
                return (g > 238) ? CT_OWN : CT_WEBCAM;
            }
            if (r < 228) {
                if (b > 238) {
                    return CT_MYSTERY;
                }
                if (r < 193) {
                    if (r < 184) {
                        return CT_OWN;
                    }
                    if (g < 186) {
                        return CT_WHEREIGO;
                    }
                    return (r > 189) ? CT_WHEREIGO : CT_OWN;
                }
                if (g < 223) {
                    if (r > 216) {
                        return CT_OWN;
                    }
                    if (g > 217) {
                        return CT_WHEREIGO;
                    }
                    if (b > 211) {
                        return CT_WEBCAM;
                    }
                    if (b < 196) {
                        return CT_WEBCAM;
                    }
                    if (r > 210) {
                        return CT_OWN;
                    }
                    return (g > 206) ? CT_WHEREIGO : CT_OWN;
                }
                if (g < 224) {
                    return CT_OWN;
                }
                return (r < 226) ? CT_WHEREIGO : CT_OWN;
            }
            return (b < 216) ? CT_FOUND : CT_OWN;
        }
        if (r < 238) {
            if (r > 141) {
                return (r > 185) ? CT_LETTERBOX : CT_CITO;
            }
            return (r < 41) ? CT_EARTH : CT_LETTERBOX;
        }
        return (r < 243) ? CT_WHEREIGO : CT_OWN;
    }

    /**
     * This method returns detected type from specific pixel from geocaching.com live map level 11 or lower.
     * It was constructed based on classification tree made by Orange (http://orange.biolab.si/)
     * Input file was made from every non-transparent pixel of every possible "full" cache icon from GC map
     *
     * @param r
     *            Red component of pixel (from 0 - 255)
     * @param g
     *            Green component of pixel (from 0 - 255)
     * @param b
     *            Blue component of pixel (from 0 - 255)
     * @return Value from 0 to 4 representing detected type or state of the cache.
     */
    private static int getCacheTypeFromPixel11(int r, int g, int b) {
        if (b < 139) {
            if (g > 104) {
                if (r < 173) {
                    return CT_TRADITIONAL;
                }
                return (r > 225) ? CT_MULTI : CT_EVENT;
            }
            if (b < 25) {
                return CT_EVENT;
            }
            return (r < 87) ? CT_MYSTERY : CT_EVENT;
        }
        if (r > 140) {
            return (r < 197) ? CT_TRADITIONAL : CT_VIRTUAL;
        }
        return CT_MYSTERY;
    }

}
