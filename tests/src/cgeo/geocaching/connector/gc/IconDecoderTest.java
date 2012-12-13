package cgeo.geocaching.connector.gc;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class IconDecoderTest extends AbstractResourceInstrumentationTestCase {

    public void testparseMapPNG14() {
        final Bitmap bitmap = getBitmap(R.raw.tile14);
        Log.d("Bitmap=" + bitmap.getWidth() + "x" + bitmap.getHeight());

        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 88, 124, 14).getType());
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 228, 104, 14).getType());
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 52, 92, 14).getType());
        assertTrue(parseMapPNG(bitmap, 108, 112, 14).isFound());
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

        int found = 0;
        found = parseMapPNG(bitmap, 150, 124, 12).isFound() ? found + 1 : found;
        found = parseMapPNG(bitmap, 176, 82, 12).isFound() ? found + 1 : found;
        found = parseMapPNG(bitmap, 240, 140, 12).isFound() ? found + 1 : found;
        found = parseMapPNG(bitmap, 211, 127, 12).isFound() ? found + 1 : found;

        assertEquals(7, multi);
        assertEquals(7, mystery);
        assertEquals(7, tradi);
        assertEquals(4, found);
    }

    public void testParseExtraMap1() {
        final Bitmap bitmap = getBitmap(R.raw.map1);
        assertTrue(parseMapPNG(bitmap, 128, 168, 12).isFound()); // GC3AT8B
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 172, 164, 12).getType()); // GC39EXB
        assertTrue(parseMapPNG(bitmap, 164, 156, 12).isFound()); // GC30M7M
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 204, 72, 12).getType()); // GC3AN5Z
        assertTrue(parseMapPNG(bitmap, 188, 92, 12).isFound()); // GC37T3R
        assertTrue(parseMapPNG(bitmap, 164, 132, 12).isFound()); // GC34JME
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 176, 148, 12).getType()); // GC37TCY
        assertEquals(CacheType.EARTH, parseMapPNG(bitmap, 180, 136, 12).getType()); // GC3947Z
        assertTrue(parseMapPNG(bitmap, 164, 100, 12).isFound()); // GC2ZY3X
        assertTrue(parseMapPNG(bitmap, 52, 104, 12).isFound()); // GC29RCW
        assertTrue(parseMapPNG(bitmap, 168, 88, 12).isFound()); // GC264JZ
        assertTrue(parseMapPNG(bitmap, 168, 140, 12).isFound()); // GC37RRV
    }

    public void testParseExtraMap2() {
        final Bitmap bitmap = getBitmap(R.raw.map2);

        assertTrue(parseMapPNG(bitmap, 132, 136, 12).isFound()); // GC3JDBW
        assertTrue(parseMapPNG(bitmap, 68, 24, 12).isFound()); // GC2T0AH
        assertTrue(parseMapPNG(bitmap, 176, 232, 12).isOwn()); // GC2RPBX
        assertTrue(parseMapPNG(bitmap, 148, 60, 12).isFound()); // GC31FY6
        assertTrue(parseMapPNG(bitmap, 216, 20, 12).isFound()); // GC2KP3M
        assertTrue(parseMapPNG(bitmap, 212, 184, 12).isOwn()); // GC30W3K
        assertTrue(parseMapPNG(bitmap, 148, 72, 12).isOwn()); // GC2RPAZ
        assertTrue(parseMapPNG(bitmap, 216, 48, 12).isOwn()); // GC2RP8W
        assertTrue(parseMapPNG(bitmap, 212, 60, 12).isFound()); // GC3CC97
        assertTrue(parseMapPNG(bitmap, 148, 100, 12).isOwn()); // GC2RPAT
        assertTrue(parseMapPNG(bitmap, 104, 136, 12).isFound()); // GC3AE31
        assertTrue(parseMapPNG(bitmap, 52, 96, 12).isOwn()); // GC2RPCH
        assertTrue(parseMapPNG(bitmap, 172, 156, 12).isOwn()); // GC2RQ07
        assertTrue(parseMapPNG(bitmap, 116, 56, 12).isFound()); // GC3AYR2
        assertTrue(parseMapPNG(bitmap, 208, 68, 12).isOwn()); // GC2RP93
        assertTrue(parseMapPNG(bitmap, 200, 52, 12).isOwn()); // GC2RPAA
        assertTrue(parseMapPNG(bitmap, 208, 44, 12).isFound()); // GC3HE15
        assertTrue(parseMapPNG(bitmap, 112, 76, 12).isOwn()); // GC2RPBE
        assertTrue(parseMapPNG(bitmap, 232, 192, 12).isOwn()); // GC2E1KF
        assertTrue(parseMapPNG(bitmap, 184, 76, 12).isFound()); // GC2NK5R
        assertTrue(parseMapPNG(bitmap, 132, 148, 12).isOwn()); // GC2RPBC
    }

    public void testParseExtraMap3() {
        final Bitmap bitmap = getBitmap(R.raw.map3);

        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 44, 0, 12).getType()); // GC1THF5
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 176, 100, 12).getType()); // GC29EGE
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 212, 128, 12).getType()); // GC1VR64
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 220, 56, 12).getType()); // GC1M13A
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 120, 80, 12).getType()); // GC1ZA2Z
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 148, 56, 12).getType()); // GC1MRD8
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 252, 8, 12).getType()); // GC3AGEX
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 76, 108, 12).getType()); // GC2C5RB
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 228, 188, 12).getType()); // GC33TWE
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 232, 128, 12).getType()); // GC38QDJ
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 228, 160, 12).getType()); // GC2G8M1
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 184, 64, 12).getType()); // GC2FYH4
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 60, 132, 12).getType()); // GC299CV
        assertEquals(CacheType.EVENT, parseMapPNG(bitmap, 244, 124, 12).getType()); // GC3E5FW
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 200, 160, 12).getType()); // GC29NR9
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 216, 116, 12).getType()); // GC17P5R
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 144, 92, 12).getType()); // GC1WYN3
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 80, 4, 12).getType()); // GC2Z90W
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 216, 148, 12).getType()); // GC29M3P
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 176, 148, 12).getType()); // GC2HJ88
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 68, 72, 12).getType()); // GC1VRB4
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 232, 100, 12).getType()); // GC29EG4
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 220, 68, 12).getType()); // GC2YXH8
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 248, 156, 12).getType()); // GC1F277
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 208, 80, 12).getType()); // GC2NV6T
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 60, 92, 12).getType()); // GC2Y2YY
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 188, 168, 12).getType()); // GC26RT7
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 224, 124, 12).getType()); // GC1ZBPC
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 144, 80, 12).getType()); // GC29NQJ
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 192, 124, 12).getType()); // GC1QRAP
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 104, 116, 12).getType()); // GC29NR1
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 240, 44, 12).getType()); // GC35KYR
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 168, 0, 12).getType()); // GC1VR78
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 200, 84, 12).getType()); // GC2YR8Z
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 52, 160, 12).getType()); // GC1MTD8
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 236, 156, 12).getType()); // GCYW8A

    }

    public void testParseExtraMap4() {
        final Bitmap bitmap = getBitmap(R.raw.map4);

        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 124, 84, 12).getType()); // GC2M3CD
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 92, 140, 12).getType()); // GC1W2A2
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 156, 108, 12).getType()); // GC3FR70
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 44, 72, 12).getType()); // GC10W91
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 104, 36, 12).getType()); // GCRC1W
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 88, 36, 12).getType()); // GC30PQF
        assertTrue(parseMapPNG(bitmap, 116, 36, 12).isFound()); // GC17VWA
        assertEquals(CacheType.EARTH, parseMapPNG(bitmap, 28, 56, 12).getType()); // GC1E6A6
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 96, 72, 12).getType()); // GCMVAC
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 140, 48, 12).getType()); // GCZPE4
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 88, 84, 12).getType()); // GC16G8B
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 116, 48, 12).getType()); // GCZPEB
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 148, 8, 12).getType()); // GC19QQ4
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 68, 124, 12).getType()); // GCXJGD
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 88, 156, 12).getType()); // GC1VNAE
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 24, 24, 12).getType()); // GC1AY4H
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 180, 60, 12).getType()); // GC3K4HB
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 56, 104, 12).getType()); // GC2M4EH
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 12, 132, 12).getType()); // GC2B92G
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 240, 180, 12).getType()); // GC2YJ88
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 220, 140, 12).getType()); // GC2AWBC
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 124, 44, 12).getType()); // GC16V66
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 116, 104, 12).getType()); // GC2MN5V
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 212, 4, 12).getType()); // GC3BF7V
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 168, 40, 12).getType()); // GC1PB21
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 252, 56, 12).getType()); // GC22VTB
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 108, 64, 12).getType()); // GCVE3B
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 20, 140, 12).getType()); // GC1R041
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 124, 244, 12).getType()); // GC3DWEA
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 240, 136, 12).getType()); // GC249ZE
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 124, 56, 12).getType()); // GC1X0XJ
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 56, 16, 12).getType()); // GC2ZVGB
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 164, 164, 12).getType()); // GC3D65W
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 240, 128, 12).getType()); // GC33KV9
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 220, 244, 12).getType()); // GC21VT0
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 84, 24, 12).getType()); // GC1949K
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 104, 88, 12).getType()); // GC1FKZY
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 56, 248, 12).getType()); // GC2Y5Z4
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 72, 32, 12).getType()); // GC395J6
        assertTrue(parseMapPNG(bitmap, 180, 4, 12).isFound()); // GC21MFG
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 96, 100, 12).getType()); // GC1W45E
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 144, 160, 12).getType()); // GC37BA1
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 12, 4, 12).getType()); // GC1K8KR
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 172, 92, 12).getType()); // GC3EZZ4
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 188, 132, 12).getType()); // GC26T9J
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 68, 192, 12).getType()); // GC1ZAMG
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 176, 180, 12).getType()); // GC21EZE
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 172, 76, 12).getType()); // GC1G5PT
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 208, 112, 12).getType()); // GC132VV
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 156, 40, 12).getType()); // GC264J4
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 252, 140, 12).getType()); // GC2JBNE
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 112, 76, 12).getType()); // GC16VKJ
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 16, 156, 12).getType()); // GC2ADX3
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 68, 48, 12).getType()); // GC2AZT1
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 176, 252, 12).getType()); // GC3DWNM
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 4, 156, 12).getType()); // GC30VHE
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 156, 120, 12).getType()); // GC1T9WM
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 40, 48, 12).getType()); // GC30MTZ
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 180, 232, 12).getType()); // GC2XVQA
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 72, 92, 12).getType()); // GC1VVA9
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 0, 132, 12).getType()); // GC1XNN4
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 92, 192, 12).getType()); // GC11D9P
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 52, 84, 12).getType()); // GC2M693
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 176, 196, 12).getType()); // GCZHVE
        assertTrue(parseMapPNG(bitmap, 140, 108, 12).isFound()); // GC1Q5PW
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 108, 148, 12).getType()); // GC2ZR0C
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 168, 8, 12).getType()); // GCYWQH
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 196, 92, 12).getType()); // GC39VXN
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 148, 136, 12).getType()); // GC2MM6C
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 168, 28, 12).getType()); // GC2H1TG
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 240, 52, 12).getType()); // GC2QTXT
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 152, 148, 12).getType()); // GC3E7QD
        assertTrue(parseMapPNG(bitmap, 160, 60, 12).isFound()); // GC2J3G9
        assertTrue(parseMapPNG(bitmap, 160, 100, 12).isFound()); // GC2327G
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 136, 32, 12).getType()); // GC2JVEH
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 208, 164, 12).getType()); // GC1NN15
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 84, 244, 12).getType()); // GC3E5JP
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 172, 16, 12).getType()); // GC1Z581
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 104, 20, 12).getType()); // GC2MENX
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 144, 60, 12).getType()); // GC1V3MG
        assertTrue(parseMapPNG(bitmap, 228, 56, 12).isFound()); // GC36WZN
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 144, 212, 12).getType()); // GCR9GB
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 180, 68, 12).getType()); // GC3JZ1K
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 228, 104, 12).getType()); // GCQ95T
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 84, 220, 12).getType()); // GCWTVM
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 200, 228, 12).getType()); // GC3CC1A
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 204, 56, 12).getType()); // GC1K0WX
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 244, 208, 12).getType()); // GC1JVXG
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 84, 128, 12).getType()); // GC2XQ6C
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 248, 164, 12).getType()); // GC3B1JK
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 232, 84, 12).getType()); // GC3AT8J
        assertTrue(parseMapPNG(bitmap, 160, 88, 12).isFound()); // GC2MB4P
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 132, 20, 12).getType()); // GC2NW3F
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 56, 132, 12).getType()); // GC22ERA
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 28, 32, 12).getType()); // GC2EFFK
    }

    public void testParseExtraMap5() {
        final Bitmap bitmap = getBitmap(R.raw.map5);

        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 60, 32, 12).getType()); // GC31DNK
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 200, 120, 12).getType()); // GCP89K
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 144, 152, 12).getType()); // GC22AR8
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 164, 92, 12).getType()); // GC1MFB7
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 16, 212, 12).getType()); // GC12F2K
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 188, 12, 12).getType()); // GC24J14
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 36, 72, 12).getType()); // GC2J8MY
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 152, 140, 12).getType()); // GC1H9WQ
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 44, 40, 12).getType()); // GC31DNZ
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 8, 152, 12).getType()); // GC34YFB
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 200, 216, 12).getType()); // GC30MK5
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 84, 20, 12).getType()); // GC304YY
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 192, 236, 12).getType()); // GC1D6AC
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 220, 48, 12).getType()); // GC1HQ8Y
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 136, 176, 12).getType()); // GC310B7
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 132, 232, 12).getType()); // GC12CR5
        assertTrue(parseMapPNG(bitmap, 240, 40, 12).isFound()); // GC24GW1
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 140, 116, 12).getType()); // GC2YYE7
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 124, 144, 12).getType()); // GC111RZ
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 48, 128, 12).getType()); // GC13A7V
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 136, 92, 12).getType()); // GC2BKW9
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 200, 184, 12).getType()); // GC30X0C
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 156, 200, 12).getType()); // GC17V4A
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 160, 120, 12).getType()); // GC2ZBWW
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 196, 36, 12).getType()); // GC14X25
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 192, 100, 12).getType()); // GC1HXAX
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 108, 168, 12).getType()); // GC3C043
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 232, 28, 12).getType()); // GC1TEAR
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 200, 204, 12).getType()); // GC3AKFV
        assertTrue(parseMapPNG(bitmap, 228, 28, 12).isFound()); // GC2NMPR
        //assertEquals(CacheType.VIRTUAL, parseMapPNG(bitmap, 232, 252, 12).getType()); // GC1AH0N - False detection
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 220, 188, 12).getType()); // GC1ZXDK
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 168, 212, 12).getType()); // GC3A919
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 152, 176, 12).getType()); // GC196WN
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 144, 180, 12).getType()); // GC12RE5
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 176, 116, 12).getType()); // GC1DY2M
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 44, 212, 12).getType()); // GC3MRNT
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 220, 36, 12).getType()); // GC3CWZD
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 48, 160, 12).getType()); // GC1A8E3
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 8, 252, 12).getType()); // GC10W6W
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 60, 92, 12).getType()); // GC2D9DD
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 96, 164, 12).getType()); // GC1Z4QX
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 220, 252, 12).getType()); // GCNEGK
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 32, 188, 12).getType()); // GC10916
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 204, 224, 12).getType()); // GC1CA2Y
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 120, 236, 12).getType()); // GC11B3J
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 248, 24, 12).getType()); // GCKX8C
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 128, 152, 12).getType()); // GC2V6AA
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 196, 48, 12).getType()); // GC2YG95
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 48, 64, 12).getType()); // GCHGR8
        assertEquals(CacheType.EVENT, parseMapPNG(bitmap, 188, 96, 12).getType()); // GC3KBPK
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 208, 140, 12).getType()); // GC1C9B0
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 164, 100, 12).getType()); // GC29JGA
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 156, 28, 12).getType()); // GCN690
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 232, 20, 12).getType()); // GC18Z53
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 220, 152, 12).getType()); // GC18RB6
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 200, 248, 12).getType()); // GC2378H
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 248, 244, 12).getType()); // GCV8QA
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 12, 232, 12).getType()); // GC2MXDG
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 48, 248, 12).getType()); // GCTHJR
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 216, 200, 12).getType()); // GC1EPM5
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 232, 60, 12).getType()); // GC2N0PB
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 88, 56, 12).getType()); // GC1ZWNX
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 248, 56, 12).getType()); // GC1N11P
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 100, 180, 12).getType()); // GCM6AE
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 220, 124, 12).getType()); // GC2A1RQ
        assertTrue(parseMapPNG(bitmap, 212, 4, 12).isFound()); // GC1TVKE
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 28, 212, 12).getType()); // GC2A1RR
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 128, 84, 12).getType()); // GC16AWC
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 220, 16, 12).getType()); // GC282V9
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 112, 240, 12).getType()); // GC18VT5
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 80, 248, 12).getType()); // GC10YEK
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 224, 228, 12).getType()); // GC1EA70
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 232, 244, 12).getType()); // GC14PNY
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 108, 32, 12).getType()); // GC2MMPN
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 144, 188, 12).getType()); // GC1CCF4
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 228, 208, 12).getType()); // GCV8C2
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 104, 252, 12).getType()); // GCTRPF
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 176, 92, 12).getType()); // GCRF8G
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 120, 140, 12).getType()); // GC210B9
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 204, 240, 12).getType()); // GC16NTW
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 192, 224, 12).getType()); // GC2PTVN
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 76, 116, 12).getType()); // GC1RPG0
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 144, 200, 12).getType()); // GC1FZ4T
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 172, 36, 12).getType()); // GC1ZYG8
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 248, 196, 12).getType()); // GC17FJQ
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 88, 140, 12).getType()); // GC1KWK0
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 168, 196, 12).getType()); // GC17MNG
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 20, 252, 12).getType()); // GC13M6V
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 120, 172, 12).getType()); // GC3B30A
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 104, 92, 12).getType()); // GC2GY9D
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 128, 120, 12).getType()); // GC2Y90M
        assertTrue(parseMapPNG(bitmap, 204, 40, 12).isFound()); // GC1BZ6P
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 56, 76, 12).getType()); // GC10K7X
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 196, 108, 12).getType()); // GC1F0R5
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 120, 196, 12).getType()); // GC1KQQW
    }

    public void testParseExtraMap11() {
        final Bitmap bitmap = getBitmap(R.raw.map11);
        assertEquals(CacheType.EVENT, parseMapPNG(bitmap, 132, 16, 11).getType());
        assertEquals(CacheType.MULTI, parseMapPNG(bitmap, 104, 48, 11).getType());
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 128, 124, 11).getType());
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 228, 8, 11).getType());
        assertEquals(CacheType.TRADITIONAL, parseMapPNG(bitmap, 160, 156, 11).getType());
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 208, 176, 11).getType());
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 252, 24, 11).getType());
        assertEquals(CacheType.MYSTERY, parseMapPNG(bitmap, 216, 96, 11).getType());
        assertEquals(CacheType.EARTH, parseMapPNG(bitmap, 24, 212, 11).getType());
    }

}
