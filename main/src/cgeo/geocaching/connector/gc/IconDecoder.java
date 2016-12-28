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
    private static final int CT_CITO = 7;
    private static final int CT_VIRTUAL = 8;
    private static final int CT_MEGAEVENT = 9;
    private static final int CT_WHERIGO = 10;
    private static final int CT_WEBCAM = 11;
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
            return false; //out of image position
        }

        int numberOfDetections = 9; //for level 12 and 13
        if (zoomlevel < 12) {
            numberOfDetections = 5;
        }
        if (zoomlevel > 13) {
            numberOfDetections = 9;
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

                final int type;
                if (zoomlevel < 12) {
                    type = getCacheTypeFromPixel11(r, g, b);
                } else if (zoomlevel > 13) {
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
     * A method that returns true if pixel color appears on more than one cache type and shall be excluded from parsing
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
        if ((r == g) && (g == b)) {
            return true;
        }
        if (zoomlevel < 12) {
            return false;
        }
        if (zoomlevel > 13) {

            if ((r == 252) && (b == 252) && (g == 251) || (r == 206) && (b == 219) && (g == 230) || (r == 178) && (b == 198) && (g == 215) || (r == 162) && (b == 186) && (g == 209) || (r == 187) && (b == 205) && (g == 222) || (r == 216) && (b == 226) && (g == 235) || (r == 253) && (b == 254) && (g == 254) || (r == 243) && (b == 247) && (g == 249) || (r == 136) && (b == 166) && (g == 195) || (r == 254) && (b == 255) && (g == 255) || (r == 222) && (b == 231) && (g == 239) || (r == 240) && (b == 244) && (g == 248) || (r == 194) && (b == 209) && (g == 221) || (r == 153) && (b == 178) && (g == 199) || (r == 228) && (b == 238) && (g == 242) || (r == 109) && (b == 165) && (g == 141) || (r == 124) && (b == 194) && (g == 165) || (r == 206) && (b == 229) && (g == 220) || (r == 199) && (b == 224) && (g == 210) || (r == 252) && (b == 253) && (g == 254) || (r == 244) && (b == 247) && (g == 243) || (r == 197) && (b == 212) && (g == 227) || (r == 171) && (b == 172) && (g == 172) || (r == 160) && (b == 161) && (g == 161) || (r == 232) && (b == 249) && (g == 250) || (r == 253) && (b == 241) && (g == 227) || (r == 254) && (b == 248) && (g == 240) || (r == 72) && (b == 112) && (g == 48) || (r == 251) && (b == 239) && (g == 217) || (r == 214) && (b == 239) && (g == 244) || (r == 202) && (b == 214) && (g == 194) || (r == 168) && (b == 186) && (g == 156) || (r == 254) && (b == 249) && (g == 247) || (r == 132) && (b == 166) && (g == 130) || (r == 238) && (b == 238) && (g == 233) || (r == 241) && (b == 252) && (g == 253) || (r == 101) && (b == 135) && (g == 80) || (r == 193) && (b == 208) && (g == 184) || (r == 251) && (b == 253) && (g == 252)) {
                return true;
            }
            return false;
        }
        //zoom level 12, 13
        if ((r == 247) && (b == 249) && (g == 251) || (r == 253) && (b == 254) && (g == 254) || (r == 252) && (b == 253) && (g == 254) || (r == 241) && (b == 249) && (g == 251) || (r == 249) && (b == 252) && (g == 253) || (r == 251) && (b == 253) && (g == 253) || (r == 254) && (b == 253) && (g == 253)) {
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
        if (b < 139) {
            if (r < 115) {
                if (b < 82) {
                    if (r < 63) {
                        if (r < 20) {
                            if (r < 4) {
                                return g < 137 ? CT_TRADITIONAL : CT_CITO;
                            }
                            return g < 150 ? CT_TRADITIONAL : CT_CITO;
                        }
                        return CT_EARTH;
                    }
                    if (g < 149) {
                        return r < 88 ? CT_EARTH : CT_FOUND;
                    }
                    return CT_EVENT;
                }
                if (g < 138) {
                    if (r < 37) {
                        return CT_CITO;
                    }
                    if (b < 126) {
                        if (r < 72) {
                            if (r < 44) {
                                return g < 90 ? CT_EVENT : CT_CITO;
                            }
                            return CT_EVENT;
                        }
                        return CT_EARTH;
                    }
                    if (g < 90) {
                        return CT_EVENT;
                    }
                    return g < 120 ? CT_CITO : CT_EVENT;
                }
                if (g < 166) {
                    return r < 75 ? CT_TRADITIONAL : CT_EARTH;
                }
                if (r < 62) {
                    if (g < 175) {
                        return CT_CITO;
                    }
                    return r < 48 ? CT_OWN : CT_CITO;
                }
                return g < 184 ? CT_TRADITIONAL : CT_CITO;
            }
            if (r < 201) {
                if (g < 118) {
                    return r < 141 ? CT_FOUND : CT_EVENT;
                }
                if (g < 170) {
                    if (r < 150) {
                        return g < 146 ? CT_FOUND : CT_EARTH;
                    }
                    return r < 192 ? CT_FOUND : CT_EVENT;
                }
                return CT_EVENT;
            }
            if (g < 183) {
                return CT_MULTI;
            }
            if (r < 239) {
                return CT_FOUND;
            }
            if (g < 210) {
                return r < 246 ? CT_MULTI : CT_EARTH;
            }
            return b < 66 ? CT_FOUND : CT_EARTH;
        }
        if (r < 234) {
            if (r < 86) {
                if (b < 180) {
                    if (b < 151) {
                        if (r < 55) {
                            return g < 75 ? CT_EVENT : CT_MYSTERY;
                        }
                        return g < 160 ? CT_CITO : CT_OWN;
                    }
                    return g < 92 ? CT_EVENT : CT_MYSTERY;
                }
                return CT_VIRTUAL;
            }
            if (b < 211) {
                if (r < 164) {
                    if (g < 179) {
                        if (b < 173) {
                            if (r < 98) {
                                return CT_CITO;
                            }
                            if (g < 160) {
                                return CT_EVENT;
                            }
                            return r < 133 ? CT_EARTH : CT_EVENT;
                        }
                        if (b < 185) {
                            if (r < 117) {
                                return CT_MYSTERY;
                            }
                            return g < 154 ? CT_EVENT : CT_CITO;
                        }
                        if (b < 196) {
                            return r < 141 ? CT_MYSTERY : CT_CITO;
                        }
                        return CT_MYSTERY;
                    }
                    if (g < 206) {
                        if (b < 181) {
                            if (g < 189) {
                                return r < 121 ? CT_TRADITIONAL : CT_EARTH;
                            }
                            return b < 155 ? CT_CITO : CT_TRADITIONAL;
                        }
                        if (g < 188) {
                            return b < 204 ? CT_CITO : CT_MYSTERY;
                        }
                        return CT_EARTH;
                    }
                    if (g < 210) {
                        return g < 208 ? CT_OWN : CT_CITO;
                    }
                    if (r < 155) {
                        if (g < 215) {
                            return r < 121 ? CT_OWN : CT_CITO;
                        }
                        return CT_OWN;
                    }
                    return CT_CITO;
                }
                if (r < 193) {
                    if (b < 192) {
                        if (g < 175) {
                            return b < 176 ? CT_FOUND : CT_EVENT;
                        }
                        if (b < 171) {
                            return CT_EVENT;
                        }
                        return r < 167 ? CT_EARTH : CT_EVENT;
                    }
                    if (g < 204) {
                        return b < 199 ? CT_EVENT : CT_CITO;
                    }
                    if (g < 226) {
                        if (r < 186) {
                            return CT_TRADITIONAL;
                        }
                        return g < 218 ? CT_EARTH : CT_TRADITIONAL;
                    }
                    return r < 179 ? CT_OWN : CT_CITO;
                }
                if (g < 208) {
                    if (g < 180) {
                        return CT_EVENT;
                    }
                    if (r < 198) {
                        return r < 196 ? CT_FOUND : CT_EVENT;
                    }
                    return CT_FOUND;
                }
                if (g < 215) {
                    return r < 207 ? CT_EVENT : CT_FOUND;
                }
                if (b < 199) {
                    return CT_EVENT;
                }
                return r < 218 ? CT_EARTH : CT_EVENT;
            }
            if (r < 174) {
                if (r < 156) {
                    return CT_VIRTUAL;
                }
                if (g < 201) {
                    return CT_MYSTERY;
                }
                if (b < 228) {
                    return CT_EARTH;
                }
                if (r < 173) {
                    return CT_VIRTUAL;
                }
                return g < 221 ? CT_EARTH : CT_VIRTUAL;
            }
            if (b < 227) {
                if (g < 222) {
                    if (r < 188) {
                        return b < 214 ? CT_CITO : CT_MYSTERY;
                    }
                    return b < 224 ? CT_CITO : CT_MYSTERY;
                }
                if (r < 210) {
                    if (g < 235) {
                        return g < 233 ? CT_TRADITIONAL : CT_CITO;
                    }
                    return CT_OWN;
                }
                return r < 216 ? CT_TRADITIONAL : CT_EARTH;
            }
            if (r < 208) {
                if (g < 219) {
                    return CT_MYSTERY;
                }
                if (b < 240) {
                    if (r < 190) {
                        return b < 233 ? CT_EARTH : CT_VIRTUAL;
                    }
                    return CT_EARTH;
                }
                return CT_VIRTUAL;
            }
            if (g < 233) {
                if (b < 233) {
                    return r < 210 ? CT_MYSTERY : CT_CITO;
                }
                if (r < 221) {
                    return CT_MYSTERY;
                }
                return b < 239 ? CT_CITO : CT_MYSTERY;
            }
            if (b < 243) {
                if (g < 241) {
                    if (r < 226) {
                        return CT_EARTH;
                    }
                    if (r < 228) {
                        return CT_CITO;
                    }
                    return r < 229 ? CT_MYSTERY : CT_EARTH;
                }
                if (r < 221) {
                    return CT_OWN;
                }
                return r < 230 ? CT_CITO : CT_TRADITIONAL;
            }
            if (r < 224) {
                if (b < 244) {
                    return r < 216 ? CT_VIRTUAL : CT_EARTH;
                }
                return CT_VIRTUAL;
            }
            return g < 241 ? CT_MYSTERY : CT_EARTH;
        }
        if (b < 230) {
            if (r < 246) {
                if (b < 206) {
                    if (r < 243) {
                        if (r < 240) {
                            return g < 226 ? CT_EARTH : CT_EVENT;
                        }
                        return CT_EARTH;
                    }
                    return g < 216 ? CT_MULTI : CT_EARTH;
                }
                if (b < 219) {
                    if (r < 243) {
                        if (r < 237) {
                            return g < 235 ? CT_EVENT : CT_EARTH;
                        }
                        return CT_EVENT;
                    }
                    return CT_EARTH;
                }
                return g < 236 ? CT_FOUND : CT_EVENT;
            }
            if (g < 220) {
                return CT_MULTI;
            }
            if (g < 243) {
                if (r < 249) {
                    return CT_EARTH;
                }
                if (r < 251) {
                    return CT_MULTI;
                }
                return b < 185 ? CT_EARTH : CT_MULTI;
            }
            return CT_EARTH;
        }
        if (r < 249) {
            if (b < 242) {
                if (r < 246) {
                    if (r < 240) {
                        return CT_TRADITIONAL;
                    }
                    return g < 237 ? CT_EVENT : CT_FOUND;
                }
                return CT_EVENT;
            }
            if (b < 249) {
                if (g < 245) {
                    if (b < 245) {
                        return CT_CITO;
                    }
                    if (g < 243) {
                        return CT_MYSTERY;
                    }
                    return b < 247 ? CT_CITO : CT_MYSTERY;
                }
                if (b < 248) {
                    if (g < 250) {
                        if (r < 236) {
                            return CT_OWN;
                        }
                        if (r < 246) {
                            if (g < 246) {
                                return CT_EARTH;
                            }
                            if (r < 243) {
                                return CT_TRADITIONAL;
                            }
                            return r < 244 ? CT_EARTH : CT_TRADITIONAL;
                        }
                        return CT_EARTH;
                    }
            return CT_OWN;
            }
                return CT_CITO;
            }
            if (g < 249) {
                return r < 241 ? CT_VIRTUAL : CT_MYSTERY;
            }
            return CT_VIRTUAL;
        }
        if (r < 253) {
            if (b < 252) {
                if (b < 248) {
                    return CT_EVENT;
                }
                return r < 251 ? CT_FOUND : CT_EVENT;
            }
            if (b < 254) {
                if (g < 253) {
                    return r < 252 ? CT_CITO : CT_MYSTERY;
                }
                return CT_OWN;
            }
            return CT_EARTH;
            }
        if (g < 250) {
            if (g < 249) {
                return CT_MULTI;
            }
            return b < 240 ? CT_EARTH : CT_MULTI;
            }
        if (b < 249) {
            if (b < 243) {
                return CT_EARTH;
            }
            return g < 253 ? CT_MULTI : CT_EARTH;
            }
        if (b < 253) {
            return CT_EVENT;
        }
        if (g < 254) {
            return CT_MYSTERY;
        }
        if (b < 255) {
            return r < 255 ? CT_MULTI : CT_EARTH;
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
        if (b < 150) {
            if (r < 118) {
                if (b < 93) {
                    if (r < 69) {
                        if (b < 74) {
                            return r < 68 ? CT_EARTH : CT_TRADITIONAL;
                        }
                        if (r < 3) {
                            if (b < 78) {
                                return g < 146 ? CT_TRADITIONAL : CT_CITO;
                            }
                            return CT_CITO;
                        }
                        return g < 151 ? CT_TRADITIONAL : CT_CITO;
                    }
                    if (g < 138) {
                        if (r < 100) {
                            return r < 85 ? CT_EARTH : CT_TRADITIONAL;
                        }
                        return CT_FOUND;
                    }
                    if (g < 145) {
                        return r < 94 ? CT_EVENT : CT_TRADITIONAL;
                    }
                    return CT_EVENT;
                }
                if (g < 152) {
                    if (b < 122) {
                        if (r < 50) {
                            if (r < 40) {
                                return CT_CITO;
                            }
                            return r < 46 ? CT_EVENT : CT_CITO;
                        }
                        if (g < 133) {
                            return CT_EVENT;
                        }
                        return r < 109 ? CT_EARTH : CT_TRADITIONAL;
                    }
                    if (r < 46) {
                        if (g < 79) {
                            return CT_EVENT;
                        }
                        return b < 133 ? CT_EVENT : CT_MYSTERY;
                    }
                    if (r < 79) {
                        return CT_CITO;
                    }
                    return r < 81 ? CT_EVENT : CT_CITO;
                }
                if (g < 175) {
                    if (g < 163) {
                        return r < 81 ? CT_TRADITIONAL : CT_EARTH;
                    }
                    return b < 110 ? CT_CITO : CT_TRADITIONAL;
                }
                if (r < 44) {
                    return CT_OWN;
                }
                if (g < 184) {
                    return b < 130 ? CT_CITO : CT_TRADITIONAL;
                }
                return g < 197 ? CT_CITO : CT_OWN;
            }
            if (g < 160) {
                if (g < 93) {
                    return CT_EVENT;
                }
                if (r < 182) {
                    if (r < 132) {
                        if (g < 130) {
                            return CT_FOUND;
                        }
                        return b < 60 ? CT_OWN : CT_TRADITIONAL;
                    }
                    return CT_FOUND;
                }
                return b < 80 ? CT_MULTI : CT_EVENT;
            }
            if (b < 58) {
                if (r < 238) {
                    return r < 162 ? CT_OWN : CT_FOUND;
                }
                return g < 209 ? CT_EARTH : CT_FOUND;
            }
            if (r < 233) {
                if (g < 191) {
                    if (r < 147) {
                        return b < 139 ? CT_EVENT : CT_EARTH;
                    }
                    return b < 116 ? CT_OWN : CT_TRADITIONAL;
                }
                if (r < 176) {
                    return CT_EVENT;
                }
                return g < 211 ? CT_OWN : CT_EVENT;
            }
            if (g < 204) {
                if (b < 105) {
                    return g < 192 ? CT_MULTI : CT_EARTH;
                }
                return CT_MULTI;
            }
            if (r < 246) {
                if (r < 245) {
                    return CT_EARTH;
                }
                return g < 213 ? CT_EARTH : CT_EVENT;
            }
            return CT_EARTH;
        }
        if (r < 192) {
            if (b < 197) {
                if (r < 80) {
                    if (b < 178) {
                        if (g < 95) {
                            return CT_EVENT;
                        }
                        if (r < 58) {
                            return CT_MYSTERY;
                        }
                        return b < 158 ? CT_EVENT : CT_MYSTERY;
                    }
                    return CT_VIRTUAL;
                }
                if (g < 171) {
                    if (b < 172) {
                        if (r < 149) {
                            if (g < 136) {
                                return r < 96 ? CT_CITO : CT_EVENT;
                            }
                            return CT_CITO;
                        }
                        return CT_FOUND;
                    }
                    if (b < 188) {
                        if (r < 118) {
                            return g < 128 ? CT_EVENT : CT_MYSTERY;
                        }
                        if (r < 142) {
                            return g < 151 ? CT_EVENT : CT_CITO;
                        }
                        return CT_EVENT;
                    }
                    if (b < 190) {
                        return r < 132 ? CT_MYSTERY : CT_CITO;
                    }
                    return CT_MYSTERY;
                }
                if (g < 194) {
                    if (r < 142) {
                        if (r < 123) {
                            return g < 179 ? CT_EARTH : CT_TRADITIONAL;
                        }
                        return CT_EARTH;
                    }
                    if (b < 176) {
                        return CT_EARTH;
                    }
                    return r < 165 ? CT_CITO : CT_EVENT;
                }
                if (r < 172) {
                    if (g < 206) {
                        if (b < 161) {
                            return r < 138 ? CT_CITO : CT_EVENT;
                        }
                        return CT_TRADITIONAL;
                    }
                    if (r < 149) {
                        if (r < 126) {
                            return CT_OWN;
                        }
                        return r < 140 ? CT_CITO : CT_OWN;
                    }
                    if (g < 216) {
                        return b < 183 ? CT_CITO : CT_TRADITIONAL;
                    }
                    return CT_CITO;
                }
                if (b < 179) {
                    return g < 199 ? CT_TRADITIONAL : CT_EVENT;
                }
                return CT_EARTH;
            }
            if (b < 216) {
                if (r < 125) {
                    return CT_VIRTUAL;
                }
                if (g < 212) {
                    if (r < 158) {
                        return g < 191 ? CT_MYSTERY : CT_EARTH;
                    }
                    if (b < 210) {
                        return CT_CITO;
                    }
                    return r < 178 ? CT_MYSTERY : CT_CITO;
                }
                if (g < 224) {
                    return CT_TRADITIONAL;
                }
                return g < 227 ? CT_CITO : CT_OWN;
            }
            if (r < 156) {
                return CT_VIRTUAL;
            }
            if (b < 229) {
                if (r < 175) {
                    if (r < 160) {
                        return g < 214 ? CT_EARTH : CT_VIRTUAL;
                    }
                    return CT_EARTH;
                }
                return CT_MYSTERY;
            }
            if (r < 174) {
                return CT_VIRTUAL;
            }
            if (b < 234) {
                if (r < 176) {
                    return g < 222 ? CT_EARTH : CT_VIRTUAL;
                }
                return CT_EARTH;
            }
            return CT_VIRTUAL;
        }
        if (b < 212) {
            if (r < 240) {
                if (g < 209) {
                    if (r < 215) {
                        if (g < 173) {
                            return CT_EVENT;
                        }
                        if (b < 197) {
                            return CT_FOUND;
                        }
                        return r < 211 ? CT_EVENT : CT_FOUND;
                    }
                    return CT_EVENT;
                }
                if (b < 199) {
                    if (b < 163) {
                        return CT_OWN;
                    }
                    if (b < 198) {
                        if (g < 227) {
                            return CT_EVENT;
                        }
                        return b < 189 ? CT_OWN : CT_EVENT;
                    }
                    return CT_OWN;
                }
                if (r < 221) {
                    if (r < 201) {
                        return CT_EARTH;
                    }
                    if (r < 216) {
                        return r < 210 ? CT_EVENT : CT_TRADITIONAL;
                    }
                    return CT_FOUND;
                }
                return CT_EVENT;
            }
            if (g < 224) {
                if (r < 246) {
                    return b < 171 ? CT_EARTH : CT_EVENT;
                }
                return CT_MULTI;
            }
            if (g < 235) {
                return b < 194 ? CT_EARTH : CT_MULTI;
            }
            return CT_EARTH;
        }
        if (r < 239) {
            if (b < 235) {
                if (r < 222) {
                    if (g < 226) {
                        if (b < 225) {
                            return g < 220 ? CT_CITO : CT_EARTH;
                        }
                        if (r < 203) {
                            return CT_MYSTERY;
                        }
                        return b < 233 ? CT_CITO : CT_MYSTERY;
                    }
                    if (g < 236) {
                        if (b < 231) {
                            if (r < 216) {
                                return CT_TRADITIONAL;
                            }
                            return r < 219 ? CT_EARTH : CT_TRADITIONAL;
                        }
                        return CT_CITO;
                    }
                    return g < 241 ? CT_CITO : CT_OWN;
                }
                if (g < 234) {
                    if (g < 227) {
                        return CT_FOUND;
                    }
                    return r < 231 ? CT_TRADITIONAL : CT_FOUND;
                }
                if (r < 232) {
                    return CT_TRADITIONAL;
                }
                return b < 221 ? CT_EVENT : CT_EARTH;
            }
            if (r < 215) {
                if (b < 243) {
                    if (r < 198) {
                        return b < 238 ? CT_EARTH : CT_VIRTUAL;
                    }
                    if (r < 201) {
                        return b < 240 ? CT_EARTH : CT_VIRTUAL;
                    }
                    return CT_EARTH;
                }
                return CT_VIRTUAL;
            }
            if (b < 247) {
                if (r < 229) {
                    if (g < 236) {
                        return g < 231 ? CT_MYSTERY : CT_CITO;
            }
                    if (b < 244) {
                        return CT_EARTH;
                    }
                    return r < 225 ? CT_VIRTUAL : CT_EARTH;
            }
                if (g < 244) {
                    if (b < 239) {
                        return CT_EARTH;
                    }
                    if (b < 246) {
                        if (g < 239) {
                            return r < 231 ? CT_CITO : CT_MYSTERY;
            }
                        return CT_CITO;
            }
                    return CT_MYSTERY;
            }
                return r < 233 ? CT_CITO : CT_TRADITIONAL;
            }
            if (b < 248) {
                return r < 230 ? CT_VIRTUAL : CT_EARTH;
            }
            return CT_VIRTUAL;
            }
        if (b < 245) {
            if (r < 251) {
                if (b < 223) {
                    return g < 238 ? CT_EVENT : CT_EARTH;
                }
                if (g < 241) {
                    return r < 242 ? CT_FOUND : CT_EVENT;
                }
                if (b < 240) {
                    if (r < 244) {
                        return r < 242 ? CT_OWN : CT_EARTH;
                    }
                    return b < 236 ? CT_EVENT : CT_OWN;
                }
                if (r < 248) {
                    return CT_FOUND;
                }
                return b < 243 ? CT_EVENT : CT_OWN;
            }
            if (g < 246) {
                if (b < 239) {
                    return r < 254 ? CT_MULTI : CT_EARTH;
                }
                return CT_EVENT;
            }
            if (b < 243) {
                return CT_EARTH;
            }
            return g < 251 ? CT_MULTI : CT_EARTH;
        }
        if (b < 251) {
            if (r < 250) {
                if (g < 248) {
                    return b < 247 ? CT_FOUND : CT_MYSTERY;
                }
                if (r < 242) {
                    return CT_OWN;
                }
                if (g < 250) {
                    return r < 245 ? CT_TRADITIONAL : CT_MYSTERY;
                }
                return r < 248 ? CT_CITO : CT_TRADITIONAL;
            }
            if (r < 254) {
                if (g < 249) {
                    return CT_EVENT;
                }
                return r < 252 ? CT_FOUND : CT_EVENT;
            }
            return CT_MULTI;
        }
        if (r < 252) {
            if (b < 253) {
                if (r < 249) {
                    return CT_VIRTUAL;
                }
                return g < 253 ? CT_CITO : CT_OWN;
            }
            if (r < 251) {
                return CT_VIRTUAL;
            }
            return g < 253 ? CT_MYSTERY : CT_VIRTUAL;
        }
        if (g < 255) {
            if (r < 253) {
                return g < 254 ? CT_MYSTERY : CT_OWN;
            }
            if (b < 255) {
                if (b < 253) {
                    return CT_EVENT;
                }
                return r < 255 ? CT_MYSTERY : CT_EVENT;
            }
            return CT_MYSTERY;
        }
        if (r < 255) {
            return b < 255 ? CT_OWN : CT_VIRTUAL;
        }
        return CT_MULTI;
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
        if (r < 182) {
            if (b < 158) {
                if (g < 103) {
                    return r < 94 ? CT_MYSTERY : CT_EVENT;
                }
                return CT_TRADITIONAL;
            }
            if (g < 149) {
                return CT_MYSTERY;
            }
            if (r < 143) {
                return CT_EARTH;
            }
            return g < 218 ? CT_MYSTERY : CT_EARTH;
        }
        if (r < 240) {
            if (b < 174) {
                return b < 27 ? CT_MULTI : CT_EVENT;
            }
            if (b < 221) {
                return CT_TRADITIONAL;
            }
            if (g < 239) {
                return CT_MYSTERY;
            }
            if (b < 241) {
                return CT_TRADITIONAL;
            }
            return r < 238 ? CT_EARTH : CT_MYSTERY;
        }
        if (b < 181) {
            return CT_MULTI;
        }
        if (r < 253) {
            if (b < 235) {
                return r < 250 ? CT_EVENT : CT_MULTI;
            }
            if (b < 247) {
                return r < 249 ? CT_TRADITIONAL : CT_EVENT;
            }
            if (g < 255) {
                return r < 242 ? CT_EARTH : CT_MYSTERY;
            }
            return CT_EARTH;
        }
        if (b < 242) {
            return CT_MULTI;
        }
        if (r < 255) {
            return g < 248 ? CT_EVENT : CT_TRADITIONAL;
            }
        if (b < 248) {
            return CT_MULTI;
        }
        return g < 254 ? CT_EVENT : CT_MULTI;
    }

}