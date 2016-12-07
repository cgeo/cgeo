package cgeo.geocaching.connector.gc;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;

import android.graphics.Bitmap;

/**
 * icon decoder for cache icons
 *
 */
final class IconDecoder {
    private static final int CT_TRADITIONAL = 0;
    private static final int CT_MULTI = 1;
    private static final int CT_MYSTERY = 2;
    private static final int CT_EVENT = 3;
    private static final int CT_EARTH = 4;
    private static final int CT_FOUND = 5;
    private static final int CT_OWN = 6;
    private static final int CT_MEGAEVENT = 7;
    private static final int CT_CITO = 8;
    private static final int CT_WEBCAM = 9;
    private static final int CT_WHERIGO = 10;
    private static final int CT_VIRTUAL = 11;
    private static final int CT_LETTERBOX = 12;

    private IconDecoder() {
        throw new IllegalStateException("utility class");
    }

    static boolean parseMapPNG(final Geocache cache, final Bitmap bitmap, final UTFGridPosition xy, final int zoomlevel) {
        final int topX = xy.getX() * 4;
        final int topY = xy.getY() * 4;
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if ((topX < 0) || (topY < 0) || (topX + 4 > bitmapWidth) || (topY + 4 > bitmapHeight)) {
            return false; // out of image position
        }

        int numberOfDetections = 7; // for level 13 and less
        if (zoomlevel > 13) {
            numberOfDetections = 9;
        }

        final int[] pngType = new int[numberOfDetections];
        for (int x = topX; x < topX + 4; x++) {
            for (int y = topY; y < topY + 4; y++) {
                final int color = bitmap.getPixel(x, y);

                if ((color >>> 24) != 255) {
                    continue; // transparent pixels (or semi_transparent) are
                              // only shadows of border
                }

                final int r = (color & 0xFF0000) >> 16;
                final int g = (color & 0xFF00) >> 8;
                final int b = color & 0xFF;

                if (isPixelDuplicated(r, g, b, zoomlevel)) {
                    continue;
                }

                final int type;
                if (zoomlevel > 13) {
                    type = getCacheTypeFromPixel14(r, g, b);
                } else {
                    type = getCacheTypeFromPixel13(r, g, b);
                }
                pngType[type]++;
            }
        }

        int type = -1;
        int count = 0;

        for (int x = 0; x < pngType.length; x++) {
            if (pngType[x] > count) {
                count = pngType[x];
                type = x;
            }
        }

        if (count > 1) { // 2 pixels need to detect same type and we say good to
                         // go
            switch (type) {
                case CT_TRADITIONAL:
                    cache.setType(CacheType.TRADITIONAL, zoomlevel);
                    return true;
                case CT_MULTI:
                    cache.setType(CacheType.MULTI, zoomlevel);
                    return true;
                case CT_MYSTERY:
                    cache.setType(CacheType.MYSTERY, zoomlevel);
                    return true;
                case CT_EVENT:
                    cache.setType(CacheType.EVENT, zoomlevel);
                    return true;
                case CT_EARTH:
                    cache.setType(CacheType.EARTH, zoomlevel);
                    return true;
                case CT_FOUND:
                    cache.setFound(true);
                    return true;
                case CT_OWN:
                    cache.setOwnerUserId(Settings.getUserName());
                    return true;
                case CT_MEGAEVENT:
                    cache.setType(CacheType.MEGA_EVENT, zoomlevel);
                    return true;
                case CT_CITO:
                    cache.setType(CacheType.CITO, zoomlevel);
                    return true;
                case CT_WEBCAM:
                    cache.setType(CacheType.WEBCAM, zoomlevel);
                    return true;
                case CT_WHERIGO:
                    cache.setType(CacheType.WHERIGO, zoomlevel);
                    return true;
                case CT_VIRTUAL:
                    cache.setType(CacheType.VIRTUAL, zoomlevel);
                    return true;
                case CT_LETTERBOX:
                    cache.setType(CacheType.LETTERBOX, zoomlevel);
                    return true;
            }
        }
        return false;
    }

    /**
     * A method that returns true if pixel color appears on more than one cache
     * type and shall be excluded from parsing
     *
     * @param r
     *            red value
     * @param g
     *            green value
     * @param b
     *            blue value
     * @param zoomlevel
     *            zoom level of map
     * @return true if parsing should not be performed
     */
    private static boolean isPixelDuplicated(final int r, final int g, final int b, final int zoomlevel) {
        if (zoomlevel > 13) {
            if ((r == g) && (g == b)) {
                return true;
            }
            if ((r == 252) && (b == 252) && (g == 251) || (r == 206) && (b == 219) && (g == 230) || (r == 178) && (b == 198) && (g == 215) || (r == 162) && (b == 186) && (g == 209) || (r == 187) && (b == 205) && (g == 222) || (r == 216) && (b == 226) && (g == 235) || (r == 243) && (b == 247) && (g == 249) || (r == 136) && (b == 166) && (g == 195) || (r == 222) && (b == 231) && (g == 239) || (r == 240) && (b == 244) && (g == 248) || (r == 194) && (b == 209) && (g == 221) || (r == 153) && (b == 178) && (g == 199) || (r == 228) && (b == 238) && (g == 242) || (r == 109) && (b == 165) && (g == 141) || (r == 124) && (b == 194) && (g == 165) || (r == 206) && (b == 229) && (g == 220) || (r == 199) && (b == 224) && (g == 210) || (r == 244) && (b == 247) && (g == 243) || (r == 197) && (b == 212) && (g == 227) || (r == 171) && (b == 172) && (g == 172) || (r == 160) && (b == 161) && (g == 161) || (r == 232) && (b == 249) && (g == 250) || (r == 253) && (b == 241) && (g == 227) || (r == 254) && (b == 248) && (g == 240) || (r == 72) && (b == 112) && (g == 48) || (r == 251) && (b == 239) && (g == 217) || (r == 214) && (b == 239) && (g == 244) || (r == 202) && (b == 214) && (g == 194) || (r == 168) && (b == 186) && (g == 156) || (r == 254) && (b == 249) && (g == 247) || (r == 132) && (b == 166) && (g == 130) || (r == 238) && (b == 238) && (g == 233) || (r == 241) && (b == 252) && (g == 253) || (r == 101) && (b == 135) && (g == 80) || (r == 193) && (b == 208) && (g == 184)) {
                return true;
            }
            return false;
        }
        // zoom 13 or less
        if ((r == 255) && (g == 255) && (b == 255)) {
            return true;
        }
        return false;
    }

    /**
     * This method returns detected type from specific pixel from geocaching.com
     * live map. It was constructed based on classification tree made by Orange
     * (http://orange.biolab.si/) Input file was made from every non-transparent
     * pixel of every possible "middle" cache icon from GC map
     *
     * @param r
     *            Red component of pixel (from 0 - 255)
     * @param g
     *            Green component of pixel (from 0 - 255)
     * @param b
     *            Blue component of pixel (from 0 - 255)
     * @return Value from 0 to 6 representing detected type or state of the
     *         cache.
     */
    private static int getCacheTypeFromPixel13(final int r, final int g, final int b) {
        if (b < 24) {
            return CT_OWN;
        }
        if (b < 49) {
            if (r < 88) {
                return CT_TRADITIONAL;
            }
            return g < 50 ? CT_EVENT : CT_FOUND;
        }
        if (r < 215) {
            if (b < 214) {
                if (r < 79) {
                    return CT_MYSTERY;
                }
                if (b < 200) {
                    if (r < 106) {
                        return g < 133 ? CT_TRADITIONAL : CT_MYSTERY;
                    }
                    if (b < 90) {
                        return CT_OWN;
                    }
                    if (g < 219) {
                        if (r < 205) {
                            if (b < 114) {
                                return g < 172 ? CT_TRADITIONAL : CT_OWN;
                            }
                            return g < 158 ? CT_FOUND : CT_TRADITIONAL;
                        }
                        return CT_FOUND;
                    }
                    return CT_OWN;
                }
                return CT_MYSTERY;
            }
            if (r < 211) {
                if (g < 200) {
                    return CT_MYSTERY;
                }
                if (r < 188) {
                    return CT_EARTH;
                }
                return r < 200 ? CT_MYSTERY : CT_EARTH;
            }
            return CT_MYSTERY;
        }
        if (b < 253) {
            if (g < 227) {
                if (r < 241) {
                    return CT_EVENT;
                }
                if (b < 206) {
                    if (r < 243) {
                        return g < 166 ? CT_MULTI : CT_EVENT;
                    }
                    return CT_MULTI;
                }
                return CT_EVENT;
            }
            if (r < 252) {
                if (b < 252) {
                    if (b < 246) {
                        if (b < 207) {
                            return CT_OWN;
                        }
                        if (g < 248) {
                            if (g < 233) {
                                return CT_TRADITIONAL;
                            }
                            return r < 241 ? CT_FOUND : CT_TRADITIONAL;
                        }
                        return CT_OWN;
                    }
                    return g < 249 ? CT_MYSTERY : CT_FOUND;
                }
                return CT_EARTH;
            }
            if (g < 253) {
                if (g < 237) {
                    return CT_MULTI;
                }
                if (g < 245) {
                    return CT_EVENT;
                }
                return b < 248 ? CT_MULTI : CT_EVENT;
            }
            return r < 255 ? CT_TRADITIONAL : CT_MULTI;
        }
        if (g < 255) {
            return r < 247 ? CT_EARTH : CT_MYSTERY;
        }
        return CT_EARTH;
    }

    /**
     * This method returns detected type from specific pixel from geocaching.com
     * live map level 14 or higher. It was constructed based on classification
     * tree made by Orange (http://orange.biolab.si/) Input file was made from
     * every non-transparent pixel of every possible "full" cache icon from GC
     * map
     *
     * @param r
     *            Red component of pixel (from 0 - 255)
     * @param g
     *            Green component of pixel (from 0 - 255)
     * @param b
     *            Blue component of pixel (from 0 - 255)
     * @return Value from 0 to 6 representing detected type or state of the
     *         cache.
     */
    private static int getCacheTypeFromPixel14(final int r, final int g, final int b) {
        if (r < 195) {
            if (b < 151) {
                if (r < 103) {
                    if (b < 80) {
                        if (r < 54) {
                            return g < 128 ? CT_EARTH : CT_CITO;
                        }
                        return b < 75 ? CT_TRADITIONAL : CT_EARTH;
                    }
                    if (r < 86) {
                        return b < 142 ? CT_CITO : CT_MYSTERY;
                    }
                    return CT_EARTH;
                }
                if (g < 141) {
                    return CT_FOUND;
                }
                if (r < 163) {
                    return b < 89 ? CT_OWN : CT_TRADITIONAL;
                }
                return g < 181 ? CT_FOUND : CT_OWN;
            }
            if (b < 208) {
                if (g < 185) {
                    if (b < 168) {
                        return g < 124 ? CT_MYSTERY : CT_CITO;
                    }
                    return CT_MYSTERY;
                }
                if (g < 207) {
                    return r < 164 ? CT_EARTH : CT_TRADITIONAL;
                }
                return CT_CITO;
            }
            if (r < 156) {
                return CT_WEBCAM;
            }
            return g < 233 ? CT_EARTH : CT_WEBCAM;
        }
        if (r < 235) {
            if (g < 161) {
                return CT_EVENT;
            }
            if (g < 216) {
                return CT_FOUND;
            }
            if (b < 201) {
                return CT_OWN;
            }
            if (b < 239) {
                return r < 233 ? CT_TRADITIONAL : CT_FOUND;
            }
            return CT_WEBCAM;
        }
        if (b < 145) {
            if (g < 199) {
                return b < 31 ? CT_EARTH : CT_MULTI;
            }
            return b < 49 ? CT_FOUND : CT_EARTH;
        }
        if (b < 220) {
            if (g < 210) {
                return r < 248 ? CT_EVENT : CT_MULTI;
            }
            return r < 250 ? CT_EARTH : CT_MULTI;
        }
        if (r < 252) {
            return g < 232 ? CT_EVENT : CT_OWN;
        }
        return CT_EVENT;
    }

}