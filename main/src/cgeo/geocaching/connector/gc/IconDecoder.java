package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.settings.Settings;

import android.graphics.Bitmap;

/**
 * icon decoder for cache icons
 *
 */
abstract class IconDecoder {
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

    static boolean parseMapPNG(final Geocache cache, final Bitmap bitmap, final UTFGridPosition xy, final int zoomlevel) {
        final int topX = xy.getX() * 4;
        final int topY = xy.getY() * 4;
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if ((topX < 0) || (topY < 0) || (topX + 4 > bitmapWidth) || (topY + 4 > bitmapHeight)) {
            return false; //out of image position
        }

        int numberOfDetections = 7; //for level 12 and 13
        if (zoomlevel < 12) {
            numberOfDetections = 5;
        }
        if (zoomlevel > 13) {
            numberOfDetections = 13;
        }

        final int[] pngType = new int[numberOfDetections];
        for (int x = topX; x < topX + 4; x++) {
            for (int y = topY; y < topY + 4; y++) {
                final int color = bitmap.getPixel(x, y);

                if ((color >>> 24) != 255) {
                    continue; //transparent pixels (or semi_transparent) are only shadows of border
                }

                final int r = (color & 0xFF0000) >> 16;
                final int g = (color & 0xFF00) >> 8;
                final int b = color & 0xFF;

                if (isPixelDuplicated(r, g, b, zoomlevel)) {
                    continue;
                }

                int type;
                if (zoomlevel < 12) {
                    type = getCacheTypeFromPixel11(r, g, b);
                } else {
                    if (zoomlevel > 13) {
                        type = getCacheTypeFromPixel14(r, g, b);
                    } else {
                        type = getCacheTypeFromPixel13(r, g, b);
                    }
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

        if (count > 1) { // 2 pixels need to detect same type and we say good to go
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
                    cache.setOwnerUserId(Settings.getUsername());
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
     * A method that returns true if pixel color appears on more then one cache type and shall be excluded from parsing
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
        if (zoomlevel < 12) {
            if (((r == g) && (g == b)) || ((r == 233) && (g == 233) && (b == 234))) {
                return true;
            }
            return false;
        }
        if (zoomlevel > 13) {
            if ((r == g) && (g == b)) {
                if ((r == 119) || (r == 231) || (r == 5) || (r == 230) || (r == 244) || (r == 93) || (r == 238) || (r == 73) || (r == 9) || (r == 225) || (r == 162) || (r == 153) || (r == 32) ||
                        (r == 50) || (r == 20) || (r == 232) || (r == 224) || (r == 192) || (r == 248) || (r == 152) || (r == 128) || (r == 176) || (r == 184) || (r == 200)) {
                    return false;
                }
                return true;
            }
            if ((r == 44) && (b == 44) && (g == 17) ||
                    (r == 228) && (b == 228) && (g == 255) ||
                    (r == 236) && (b == 236) && (g == 255) ||
                    (r == 252) && (b == 225) && (g == 83) ||
                    (r == 252) && (b == 221) && (g == 81) ||
                    (r == 252) && (b == 216) && (g == 79) ||
                    (r == 252) && (b == 211) && (g == 77) ||
                    (r == 251) && (b == 206) && (g == 75) ||
                    (r == 251) && (b == 201) && (g == 73) ||
                    (r == 251) && (b == 196) && (g == 71) ||
                    (r == 251) && (b == 191) && (g == 69) ||
                    (r == 243) && (b == 153) && (g == 36)) {
                return true;
            }
            return false;
        }
        //zoom level 12, 13
        if ((r == 95) && (g == 95) && (b == 95)) {
            return true;
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
    private static int getCacheTypeFromPixel13(final int r, final int g, final int b) {
        if (b < 130) {
            if (r < 41) {
                return CT_MYSTERY;
            }
            if (g < 74) {
                return CT_EVENT;
            }
            if (r < 130) {
                return CT_TRADITIONAL;
            }
            if (b < 31) {
                return CT_MULTI;
            }
            if (b < 101) {
                if (g < 99) {
                    return r < 178 ? CT_FOUND : CT_EVENT;
                }
                if (b < 58) {
                    if (g < 174) {
                        return CT_FOUND;
                    }
                    if (r < 224) {
                        return CT_OWN;
                    }
                    if (b < 49) {
                        return g < 210 ? CT_FOUND : CT_OWN;
                    }
                    if (g < 205) {
                        return g < 202 ? CT_FOUND : CT_OWN;
                    }
                    return CT_FOUND;
                }
                if (r < 255) {
                    return CT_FOUND;
                }
                return g < 236 ? CT_MULTI : CT_FOUND;
            }
            return g < 182 ? CT_EVENT : CT_MULTI;
        }
        if (r < 136) {
            return CT_MYSTERY;
        }
        if (b < 168) {
            return g < 174 ? CT_EARTH : CT_TRADITIONAL;
        }
        return CT_EARTH;
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
    private static int getCacheTypeFromPixel14(final int r, final int g, final int b) {
        if (b < 128) {
            if (r < 214) {
                if (b < 37) {
                    if (g < 50) {
                        if (b < 19) {
                            if (g < 16) {
                                if (b < 4) {
                                    return CT_FOUND;
                                }
                                return r < 8 ? CT_VIRTUAL : CT_WEBCAM;
                            }
                            return CT_FOUND;
                        }
                        return CT_WEBCAM;
                    }
                    if (b < 24) {
                        if (b < 18) {
                            return CT_EARTH;
                        }
                        return r < 127 ? CT_TRADITIONAL : CT_EARTH;
                    }
                    return CT_FOUND;
                }
                if (r < 142) {
                    if (r < 63) {
                        if (r < 26) {
                            return CT_CITO;
                        }
                        return r < 51 ? CT_WEBCAM : CT_CITO;
                    }
                    return g < 107 ? CT_WEBCAM : CT_MULTI;
                }
                if (g < 138) {
                    return r < 178 ? CT_MEGAEVENT : CT_EVENT;
                }
                return b < 71 ? CT_FOUND : CT_EARTH;
            }
            if (b < 77) {
                if (g < 166) {
                    if (r < 238) {
                        return g < 120 ? CT_MULTI : CT_OWN;
                    }
                    if (b < 57) {
                        if (r < 254) {
                            if (b < 39) {
                                if (r < 239) {
                                    return CT_OWN;
                                }
                                if (b < 36) {
                                    if (g < 150) {
                                        if (b < 24) {
                                            return b < 22 ? CT_FOUND : CT_OWN;
                                        }
                                        if (g < 138) {
                                            return b < 25 ? CT_FOUND : CT_OWN;
                                        }
                                        return CT_FOUND;
                                    }
                                    return CT_OWN;
                                }
                                if (b < 38) {
                                    if (b < 37) {
                                        if (g < 153) {
                                            return r < 242 ? CT_OWN : CT_FOUND;
                                        }
                                        return CT_OWN;
                                    }
                                    return CT_FOUND;
                                }
                                return CT_OWN;
                            }
                            if (g < 148) {
                                return CT_OWN;
                            }
                            if (r < 244) {
                                return CT_FOUND;
                            }
                            if (b < 45) {
                                if (b < 42) {
                                    return CT_FOUND;
                                }
                                if (g < 162) {
                                    return r < 245 ? CT_OWN : CT_FOUND;
                                }
                                return CT_OWN;
                            }
                            return CT_FOUND;
                        }
                        return g < 3 ? CT_FOUND : CT_VIRTUAL;
                    }
                    return CT_OWN;
                }
                if (b < 51) {
                    if (r < 251) {
                        return CT_OWN;
                    }
                    return g < 208 ? CT_EARTH : CT_MULTI;
                }
                if (b < 63) {
                    if (r < 247) {
                        return CT_FOUND;
                    }
                    if (r < 250) {
                        if (g < 169) {
                            return CT_FOUND;
                        }
                        if (g < 192) {
                            if (b < 54) {
                                return CT_OWN;
                            }
                            if (r < 248) {
                                return g < 180 ? CT_FOUND : CT_OWN;
                            }
                            return CT_OWN;
                        }
                        return g < 193 ? CT_FOUND : CT_OWN;
                    }
                    return CT_FOUND;
                }
                return CT_FOUND;
            }
            if (g < 177) {
                return CT_OWN;
            }
            if (r < 239) {
                return CT_FOUND;
            }
            if (g < 207) {
                return CT_OWN;
            }
            return r < 254 ? CT_FOUND : CT_EARTH;
        }
        if (r < 203) {
            if (b < 218) {
                if (g < 158) {
                    if (g < 71) {
                        return CT_MYSTERY;
                    }
                    return r < 153 ? CT_WHERIGO : CT_WEBCAM;
                }
                if (b < 167) {
                    return r < 157 ? CT_TRADITIONAL : CT_WEBCAM;
                }
                return CT_WHERIGO;
            }
            if (g < 199) {
                if (r < 142) {
                    return CT_LETTERBOX;
                }
                return r < 175 ? CT_CITO : CT_LETTERBOX;
            }
            if (g < 207) {
                return r < 167 ? CT_MEGAEVENT : CT_CITO;
            }
            return CT_EARTH;
        }
        if (b < 224) {
            if (g < 235) {
                if (b < 163) {
                    if (r < 249) {
                        return b < 133 ? CT_FOUND : CT_OWN;
                    }
                    return CT_FOUND;
                }
                if (r < 235) {
                    if (r < 213) {
                        if (r < 207) {
                            return CT_FOUND;
                        }
                        if (g < 206) {
                            return CT_OWN;
                        }
                        return g < 207 ? CT_FOUND : CT_OWN;
                    }
                    return g < 194 ? CT_OWN : CT_FOUND;
                }
                if (g < 230) {
                    return CT_OWN;
                }
                return b < 205 ? CT_FOUND : CT_OWN;
            }
            if (r < 238) {
                return CT_CITO;
            }
            return b < 170 ? CT_EVENT : CT_FOUND;
        }
        if (r < 251) {
            if (r < 210) {
                return CT_MYSTERY;
            }
            if (b < 252) {
                if (r < 243) {
                    if (r < 225) {
                        return CT_WHERIGO;
                    }
                    if (b < 232) {
                        if (g < 228) {
                            return CT_WEBCAM;
                        }
                        return r < 231 ? CT_VIRTUAL : CT_TRADITIONAL;
                    }
                    if (r < 236) {
                        return CT_WHERIGO;
                    }
                    return r < 240 ? CT_WEBCAM : CT_WHERIGO;
                }
                if (g < 247) {
                    return r < 245 ? CT_WEBCAM : CT_FOUND;
                }
                return CT_WHERIGO;
            }
            return CT_LETTERBOX;
        }
        if (r < 255) {
            return CT_OWN;
        }
        return g < 254 ? CT_FOUND : CT_OWN;
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
    private static int getCacheTypeFromPixel11(final int r, final int g, final int b) {
        if (g < 136) {
            if (r < 90) {
                return g < 111 ? CT_MYSTERY : CT_TRADITIONAL;
            }
            return b < 176 ? CT_EVENT : CT_MYSTERY;
        }
        if (r < 197) {
            return CT_TRADITIONAL;
        }
        return b < 155 ? CT_MULTI : CT_EARTH;
    }

}
