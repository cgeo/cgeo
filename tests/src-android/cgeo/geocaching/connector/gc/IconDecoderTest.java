package cgeo.geocaching.connector.gc;

import static cgeo.geocaching.enumerations.CacheType.CITO;
import static cgeo.geocaching.enumerations.CacheType.EARTH;
import static cgeo.geocaching.enumerations.CacheType.EVENT;
import static cgeo.geocaching.enumerations.CacheType.LETTERBOX;
import static cgeo.geocaching.enumerations.CacheType.MEGA_EVENT;
import static cgeo.geocaching.enumerations.CacheType.MULTI;
import static cgeo.geocaching.enumerations.CacheType.MYSTERY;
import static cgeo.geocaching.enumerations.CacheType.TRADITIONAL;
import static cgeo.geocaching.enumerations.CacheType.VIRTUAL;
import static cgeo.geocaching.enumerations.CacheType.WEBCAM;
import static cgeo.geocaching.enumerations.CacheType.WHERIGO;
import static cgeo.geocaching.test.R.raw.map1;
import static cgeo.geocaching.test.R.raw.map11;
import static cgeo.geocaching.test.R.raw.map3;
import static cgeo.geocaching.test.R.raw.map4;
import static cgeo.geocaching.test.R.raw.map5;
import static cgeo.geocaching.test.R.raw.map_all14;
import static cgeo.geocaching.test.R.raw.tile13;
import static cgeo.geocaching.test.R.raw.tile14;
import static cgeo.geocaching.utils.Log.d;
import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.RawRes;

public class IconDecoderTest extends AbstractResourceInstrumentationTestCase {

    public void testparseMapPNG14() {
        final Bitmap bitmap = getBitmap(tile14);
        d("Bitmap=" + bitmap.getWidth() + "x" + bitmap.getHeight());

        assertThat(parseMapPNG(bitmap, 88, 124, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 228, 104, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 52, 92, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 108, 112, 14).isFound()).isTrue();
    }

    private Bitmap getBitmap(@RawRes final int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        final Bitmap bitmap = BitmapFactory.decodeStream(getInstrumentation().getContext().getResources().openRawResource(resourceId));
        assertThat(bitmap.getWidth()).isEqualTo(Tile.TILE_SIZE);
        return bitmap;
    }

    private static Geocache parseMapPNG(final Bitmap bitmap, final int x, final int y, final int zoomlevel) {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC30");
        IconDecoder.parseMapPNG(cache, bitmap, new UTFGridPosition(x / 4, y / 4), zoomlevel);
        return cache;
    }

    public void testParseMap13() {
        final Bitmap bitmap = getBitmap(tile13);

        assertThat(parseMapPNG(bitmap, 146, 225, 13).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 181, 116, 13).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 118, 230, 13).getType()).isEqualTo(MULTI);
    }

    public void testParseMap12() {
        final Bitmap bitmap = getBitmap(R.raw.tile12);

        int multi = 0;
        multi = parseMapPNG(bitmap, 130, 92, 12).getType() == MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 93, 222, 12).getType() == MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 129, 227, 12).getType() == MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 234, 170, 12).getType() == MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 195, 113, 12).getType() == MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 195, 124, 12).getType() == MULTI ? multi + 1 : multi;
        multi = parseMapPNG(bitmap, 111, 74, 12).getType() == MULTI ? multi + 1 : multi;

        int mystery = 0;
        mystery = parseMapPNG(bitmap, 37, 25, 12).getType() == MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 49, 183, 12).getType() == MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 184, 181, 12).getType() == MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 176, 94, 12).getType() == MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 161, 124, 12).getType() == MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 168, 118, 12).getType() == MYSTERY ? mystery + 1 : mystery;
        mystery = parseMapPNG(bitmap, 231, 114, 12).getType() == MYSTERY ? mystery + 1 : mystery;

        int tradi = 0;
        tradi = parseMapPNG(bitmap, 179, 27, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 106, 93, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 145, 147, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 204, 163, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 9, 146, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 117, 225, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;
        tradi = parseMapPNG(bitmap, 90, 107, 12).getType() == TRADITIONAL ? tradi + 1 : tradi;

        int found = 0;
        found = parseMapPNG(bitmap, 150, 124, 12).isFound() ? found + 1 : found;
        found = parseMapPNG(bitmap, 176, 82, 12).isFound() ? found + 1 : found;
        found = parseMapPNG(bitmap, 240, 140, 12).isFound() ? found + 1 : found;
        found = parseMapPNG(bitmap, 211, 127, 12).isFound() ? found + 1 : found;

        assertThat(multi).isEqualTo(7);
        assertThat(mystery).isEqualTo(7);
        assertThat(tradi).isEqualTo(7);
        assertThat(found).isEqualTo(4);
    }

    public void testParseExtraMap1() {
        final Bitmap bitmap = getBitmap(map1);
        assertThat(parseMapPNG(bitmap, 128, 168, 12).isFound()).isTrue(); // GC3AT8B
        assertThat(parseMapPNG(bitmap, 172, 164, 12).getType()).isEqualTo(MYSTERY); // GC39EXB
        assertThat(parseMapPNG(bitmap, 164, 156, 12).isFound()).isTrue(); // GC30M7M
        assertThat(parseMapPNG(bitmap, 204, 72, 12).getType()).isEqualTo(MULTI); // GC3AN5Z
        assertThat(parseMapPNG(bitmap, 188, 92, 12).isFound()).isTrue(); // GC37T3R
        assertThat(parseMapPNG(bitmap, 164, 132, 12).isFound()).isTrue(); // GC34JME
        assertThat(parseMapPNG(bitmap, 176, 148, 12).getType()).isEqualTo(MULTI); // GC37TCY
        assertThat(parseMapPNG(bitmap, 180, 136, 12).getType()).isEqualTo(EARTH); // GC3947Z
        assertThat(parseMapPNG(bitmap, 164, 100, 12).isFound()).isTrue(); // GC2ZY3X
        assertThat(parseMapPNG(bitmap, 52, 104, 12).isFound()).isTrue(); // GC29RCW
        assertThat(parseMapPNG(bitmap, 168, 88, 12).isFound()).isTrue(); // GC264JZ
        assertThat(parseMapPNG(bitmap, 168, 140, 12).isFound()).isTrue(); // GC37RRV
    }

    public void testParseExtraMap2() {
        final Bitmap bitmap = getBitmap(R.raw.map2);

        assertThat(parseMapPNG(bitmap, 132, 136, 12).isFound()).isTrue(); // GC3JDBW
        assertThat(parseMapPNG(bitmap, 68, 24, 12).isFound()).isTrue(); // GC2T0AH
        assertThat(parseMapPNG(bitmap, 176, 232, 12).isOwner()).isTrue(); // GC2RPBX
        assertThat(parseMapPNG(bitmap, 148, 60, 12).isFound()).isTrue(); // GC31FY6
        assertThat(parseMapPNG(bitmap, 216, 20, 12).isFound()).isTrue(); // GC2KP3M
        assertThat(parseMapPNG(bitmap, 212, 184, 12).isOwner()).isTrue(); // GC30W3K
        assertThat(parseMapPNG(bitmap, 148, 72, 12).isOwner()).isTrue(); // GC2RPAZ
        assertThat(parseMapPNG(bitmap, 216, 48, 12).isOwner()).isTrue(); // GC2RP8W
        assertThat(parseMapPNG(bitmap, 212, 60, 12).isFound()).isTrue(); // GC3CC97
        assertThat(parseMapPNG(bitmap, 148, 100, 12).isOwner()).isTrue(); // GC2RPAT
        assertThat(parseMapPNG(bitmap, 104, 136, 12).isFound()).isTrue(); // GC3AE31
        assertThat(parseMapPNG(bitmap, 52, 96, 12).isOwner()).isTrue(); // GC2RPCH
        assertThat(parseMapPNG(bitmap, 172, 156, 12).isOwner()).isTrue(); // GC2RQ07
        assertThat(parseMapPNG(bitmap, 116, 56, 12).isFound()).isTrue(); // GC3AYR2
        assertThat(parseMapPNG(bitmap, 208, 68, 12).isOwner()).isTrue(); // GC2RP93
        assertThat(parseMapPNG(bitmap, 200, 52, 12).isOwner()).isTrue(); // GC2RPAA
        assertThat(parseMapPNG(bitmap, 208, 44, 12).isFound()).isTrue(); // GC3HE15
        assertThat(parseMapPNG(bitmap, 112, 76, 12).isOwner()).isTrue(); // GC2RPBE
        assertThat(parseMapPNG(bitmap, 232, 192, 12).isOwner()).isTrue(); // GC2E1KF
        assertThat(parseMapPNG(bitmap, 184, 76, 12).isFound()).isTrue(); // GC2NK5R
        assertThat(parseMapPNG(bitmap, 132, 148, 12).isOwner()).isTrue(); // GC2RPBC
    }

    public void testParseExtraMap3() {
        final Bitmap bitmap = getBitmap(map3);

        assertThat(parseMapPNG(bitmap, 44, 0, 12).getType()).isEqualTo(TRADITIONAL); // GC1THF5
        assertThat(parseMapPNG(bitmap, 176, 100, 12).getType()).isEqualTo(MYSTERY); // GC29EGE
        assertThat(parseMapPNG(bitmap, 212, 128, 12).getType()).isEqualTo(TRADITIONAL); // GC1VR64
        assertThat(parseMapPNG(bitmap, 220, 56, 12).getType()).isEqualTo(MYSTERY); // GC1M13A
        assertThat(parseMapPNG(bitmap, 120, 80, 12).getType()).isEqualTo(TRADITIONAL); // GC1ZA2Z
        assertThat(parseMapPNG(bitmap, 148, 56, 12).getType()).isEqualTo(MULTI); // GC1MRD8
        assertThat(parseMapPNG(bitmap, 252, 8, 12).getType()).isEqualTo(MULTI); // GC3AGEX
        assertThat(parseMapPNG(bitmap, 76, 108, 12).getType()).isEqualTo(TRADITIONAL); // GC2C5RB
        assertThat(parseMapPNG(bitmap, 228, 188, 12).getType()).isEqualTo(MULTI); // GC33TWE
        assertThat(parseMapPNG(bitmap, 232, 128, 12).getType()).isEqualTo(TRADITIONAL); // GC38QDJ
        assertThat(parseMapPNG(bitmap, 228, 160, 12).getType()).isEqualTo(TRADITIONAL); // GC2G8M1
        assertThat(parseMapPNG(bitmap, 184, 64, 12).getType()).isEqualTo(TRADITIONAL); // GC2FYH4
        assertThat(parseMapPNG(bitmap, 60, 132, 12).getType()).isEqualTo(TRADITIONAL); // GC299CV
        assertThat(parseMapPNG(bitmap, 244, 124, 12).getType()).isEqualTo(EVENT); // GC3E5FW
        assertThat(parseMapPNG(bitmap, 200, 160, 12).getType()).isEqualTo(MYSTERY); // GC29NR9
        assertThat(parseMapPNG(bitmap, 216, 116, 12).getType()).isEqualTo(TRADITIONAL); // GC17P5R
        assertThat(parseMapPNG(bitmap, 144, 92, 12).getType()).isEqualTo(MYSTERY); // GC1WYN3
        assertThat(parseMapPNG(bitmap, 80, 4, 12).getType()).isEqualTo(TRADITIONAL); // GC2Z90W
        assertThat(parseMapPNG(bitmap, 216, 148, 12).getType()).isEqualTo(MULTI); // GC29M3P
        assertThat(parseMapPNG(bitmap, 176, 148, 12).getType()).isEqualTo(TRADITIONAL); // GC2HJ88
        assertThat(parseMapPNG(bitmap, 68, 72, 12).getType()).isEqualTo(TRADITIONAL); // GC1VRB4
        assertThat(parseMapPNG(bitmap, 232, 100, 12).getType()).isEqualTo(MYSTERY); // GC29EG4
        assertThat(parseMapPNG(bitmap, 220, 68, 12).getType()).isEqualTo(TRADITIONAL); // GC2YXH8
        assertThat(parseMapPNG(bitmap, 248, 156, 12).getType()).isEqualTo(TRADITIONAL); // GC1F277
        assertThat(parseMapPNG(bitmap, 208, 80, 12).getType()).isEqualTo(MYSTERY); // GC2NV6T
        assertThat(parseMapPNG(bitmap, 60, 92, 12).getType()).isEqualTo(TRADITIONAL); // GC2Y2YY
        assertThat(parseMapPNG(bitmap, 188, 168, 12).getType()).isEqualTo(TRADITIONAL); // GC26RT7
        assertThat(parseMapPNG(bitmap, 224, 124, 12).getType()).isEqualTo(MULTI); // GC1ZBPC
        assertThat(parseMapPNG(bitmap, 144, 80, 12).getType()).isEqualTo(MYSTERY); // GC29NQJ
        assertThat(parseMapPNG(bitmap, 192, 124, 12).getType()).isEqualTo(TRADITIONAL); // GC1QRAP
        assertThat(parseMapPNG(bitmap, 104, 116, 12).getType()).isEqualTo(TRADITIONAL); // GC29NR1
        assertThat(parseMapPNG(bitmap, 240, 44, 12).getType()).isEqualTo(TRADITIONAL); // GC35KYR
        assertThat(parseMapPNG(bitmap, 168, 0, 12).getType()).isEqualTo(TRADITIONAL); // GC1VR78
        assertThat(parseMapPNG(bitmap, 200, 84, 12).getType()).isEqualTo(TRADITIONAL); // GC2YR8Z
        assertThat(parseMapPNG(bitmap, 52, 160, 12).getType()).isEqualTo(MULTI); // GC1MTD8
        assertThat(parseMapPNG(bitmap, 236, 156, 12).getType()).isEqualTo(MULTI); // GCYW8A

    }

    public void testParseExtraMap4() {
        final Bitmap bitmap = getBitmap(map4);

        assertThat(parseMapPNG(bitmap, 124, 84, 12).getType()).isEqualTo(MYSTERY); // GC2M3CD
        assertThat(parseMapPNG(bitmap, 92, 140, 12).getType()).isEqualTo(MULTI); // GC1W2A2
        assertThat(parseMapPNG(bitmap, 156, 108, 12).getType()).isEqualTo(MYSTERY); // GC3FR70
        assertThat(parseMapPNG(bitmap, 44, 72, 12).getType()).isEqualTo(MULTI); // GC10W91
        assertThat(parseMapPNG(bitmap, 104, 36, 12).getType()).isEqualTo(MYSTERY); // GCRC1W
        assertThat(parseMapPNG(bitmap, 88, 36, 12).getType()).isEqualTo(TRADITIONAL); // GC30PQF
        assertThat(parseMapPNG(bitmap, 116, 36, 12).isFound()).isTrue(); // GC17VWA
        assertThat(parseMapPNG(bitmap, 28, 56, 12).getType()).isEqualTo(EARTH); // GC1E6A6
        assertThat(parseMapPNG(bitmap, 96, 72, 12).getType()).isEqualTo(TRADITIONAL); // GCMVAC
        assertThat(parseMapPNG(bitmap, 140, 48, 12).getType()).isEqualTo(MYSTERY); // GCZPE4
        assertThat(parseMapPNG(bitmap, 88, 84, 12).getType()).isEqualTo(MULTI); // GC16G8B
        assertThat(parseMapPNG(bitmap, 116, 48, 12).getType()).isEqualTo(MYSTERY); // GCZPEB
        assertThat(parseMapPNG(bitmap, 148, 8, 12).getType()).isEqualTo(MYSTERY); // GC19QQ4
        assertThat(parseMapPNG(bitmap, 68, 124, 12).getType()).isEqualTo(TRADITIONAL); // GCXJGD
        assertThat(parseMapPNG(bitmap, 88, 156, 12).getType()).isEqualTo(TRADITIONAL); // GC1VNAE
        assertThat(parseMapPNG(bitmap, 24, 24, 12).getType()).isEqualTo(TRADITIONAL); // GC1AY4H
        assertThat(parseMapPNG(bitmap, 180, 60, 12).getType()).isEqualTo(MYSTERY); // GC3K4HB
        assertThat(parseMapPNG(bitmap, 56, 104, 12).getType()).isEqualTo(MYSTERY); // GC2M4EH
        assertThat(parseMapPNG(bitmap, 12, 132, 12).getType()).isEqualTo(MYSTERY); // GC2B92G
        assertThat(parseMapPNG(bitmap, 240, 180, 12).getType()).isEqualTo(MULTI); // GC2YJ88
        assertThat(parseMapPNG(bitmap, 220, 140, 12).getType()).isEqualTo(MULTI); // GC2AWBC
        assertThat(parseMapPNG(bitmap, 124, 44, 12).getType()).isEqualTo(MYSTERY); // GC16V66
        assertThat(parseMapPNG(bitmap, 116, 104, 12).getType()).isEqualTo(MYSTERY); // GC2MN5V
        assertThat(parseMapPNG(bitmap, 212, 4, 12).getType()).isEqualTo(TRADITIONAL); // GC3BF7V
        assertThat(parseMapPNG(bitmap, 168, 40, 12).getType()).isEqualTo(MYSTERY); // GC1PB21
        assertThat(parseMapPNG(bitmap, 252, 56, 12).getType()).isEqualTo(TRADITIONAL); // GC22VTB
        assertThat(parseMapPNG(bitmap, 108, 64, 12).getType()).isEqualTo(MULTI); // GCVE3B
        assertThat(parseMapPNG(bitmap, 20, 140, 12).getType()).isEqualTo(TRADITIONAL); // GC1R041
        assertThat(parseMapPNG(bitmap, 124, 244, 12).getType()).isEqualTo(MYSTERY); // GC3DWEA
        assertThat(parseMapPNG(bitmap, 240, 136, 12).getType()).isEqualTo(MYSTERY); // GC249ZE
        assertThat(parseMapPNG(bitmap, 124, 56, 12).getType()).isEqualTo(MYSTERY); // GC1X0XJ
        assertThat(parseMapPNG(bitmap, 56, 16, 12).getType()).isEqualTo(TRADITIONAL); // GC2ZVGB
        assertThat(parseMapPNG(bitmap, 164, 164, 12).getType()).isEqualTo(MYSTERY); // GC3D65W
        assertThat(parseMapPNG(bitmap, 240, 128, 12).getType()).isEqualTo(TRADITIONAL); // GC33KV9
        assertThat(parseMapPNG(bitmap, 220, 244, 12).getType()).isEqualTo(TRADITIONAL); // GC21VT0
        assertThat(parseMapPNG(bitmap, 84, 24, 12).getType()).isEqualTo(TRADITIONAL); // GC1949K
        assertThat(parseMapPNG(bitmap, 104, 88, 12).getType()).isEqualTo(MULTI); // GC1FKZY
        assertThat(parseMapPNG(bitmap, 56, 248, 12).getType()).isEqualTo(TRADITIONAL); // GC2Y5Z4
        assertThat(parseMapPNG(bitmap, 72, 32, 12).getType()).isEqualTo(TRADITIONAL); // GC395J6
        assertThat(parseMapPNG(bitmap, 180, 4, 12).isFound()).isTrue(); // GC21MFG
        assertThat(parseMapPNG(bitmap, 96, 100, 12).getType()).isEqualTo(TRADITIONAL); // GC1W45E
        assertThat(parseMapPNG(bitmap, 144, 160, 12).getType()).isEqualTo(TRADITIONAL); // GC37BA1
        assertThat(parseMapPNG(bitmap, 12, 4, 12).getType()).isEqualTo(MULTI); // GC1K8KR
        assertThat(parseMapPNG(bitmap, 172, 92, 12).getType()).isEqualTo(MULTI); // GC3EZZ4
        assertThat(parseMapPNG(bitmap, 188, 132, 12).getType()).isEqualTo(MYSTERY); // GC26T9J
        assertThat(parseMapPNG(bitmap, 68, 192, 12).getType()).isEqualTo(MULTI); // GC1ZAMG
        assertThat(parseMapPNG(bitmap, 176, 180, 12).getType()).isEqualTo(MYSTERY); // GC21EZE
        assertThat(parseMapPNG(bitmap, 172, 76, 12).getType()).isEqualTo(MYSTERY); // GC1G5PT
        assertThat(parseMapPNG(bitmap, 208, 112, 12).getType()).isEqualTo(MULTI); // GC132VV
        assertThat(parseMapPNG(bitmap, 156, 40, 12).getType()).isEqualTo(MYSTERY); // GC264J4
        assertThat(parseMapPNG(bitmap, 252, 140, 12).getType()).isEqualTo(MULTI); // GC2JBNE
        assertThat(parseMapPNG(bitmap, 112, 76, 12).getType()).isEqualTo(MULTI); // GC16VKJ
        assertThat(parseMapPNG(bitmap, 16, 156, 12).getType()).isEqualTo(MYSTERY); // GC2ADX3
        assertThat(parseMapPNG(bitmap, 68, 48, 12).getType()).isEqualTo(MYSTERY); // GC2AZT1
        assertThat(parseMapPNG(bitmap, 176, 252, 12).getType()).isEqualTo(MULTI); // GC3DWNM
        assertThat(parseMapPNG(bitmap, 4, 156, 12).getType()).isEqualTo(TRADITIONAL); // GC30VHE
        assertThat(parseMapPNG(bitmap, 156, 120, 12).getType()).isEqualTo(MYSTERY); // GC1T9WM
        assertThat(parseMapPNG(bitmap, 40, 48, 12).getType()).isEqualTo(MYSTERY); // GC30MTZ
        assertThat(parseMapPNG(bitmap, 180, 232, 12).getType()).isEqualTo(MULTI); // GC2XVQA
        assertThat(parseMapPNG(bitmap, 72, 92, 12).getType()).isEqualTo(TRADITIONAL); // GC1VVA9
        assertThat(parseMapPNG(bitmap, 0, 132, 12).getType()).isEqualTo(TRADITIONAL); // GC1XNN4
        assertThat(parseMapPNG(bitmap, 92, 192, 12).getType()).isEqualTo(MULTI); // GC11D9P
        assertThat(parseMapPNG(bitmap, 52, 84, 12).getType()).isEqualTo(MYSTERY); // GC2M693
        assertThat(parseMapPNG(bitmap, 176, 196, 12).getType()).isEqualTo(MYSTERY); // GCZHVE
        assertThat(parseMapPNG(bitmap, 140, 108, 12).isFound()).isTrue(); // GC1Q5PW
        assertThat(parseMapPNG(bitmap, 108, 148, 12).getType()).isEqualTo(MYSTERY); // GC2ZR0C
        assertThat(parseMapPNG(bitmap, 168, 8, 12).getType()).isEqualTo(MULTI); // GCYWQH
        assertThat(parseMapPNG(bitmap, 196, 92, 12).getType()).isEqualTo(MYSTERY); // GC39VXN
        assertThat(parseMapPNG(bitmap, 148, 136, 12).getType()).isEqualTo(MYSTERY); // GC2MM6C
        assertThat(parseMapPNG(bitmap, 168, 28, 12).getType()).isEqualTo(MULTI); // GC2H1TG
        assertThat(parseMapPNG(bitmap, 240, 52, 12).getType()).isEqualTo(MYSTERY); // GC2QTXT
        assertThat(parseMapPNG(bitmap, 152, 148, 12).getType()).isEqualTo(MYSTERY); // GC3E7QD
        assertThat(parseMapPNG(bitmap, 160, 60, 12).isFound()).isTrue(); // GC2J3G9
        assertThat(parseMapPNG(bitmap, 160, 100, 12).isFound()).isTrue(); // GC2327G
        assertThat(parseMapPNG(bitmap, 136, 32, 12).getType()).isEqualTo(MYSTERY); // GC2JVEH
        assertThat(parseMapPNG(bitmap, 208, 164, 12).getType()).isEqualTo(TRADITIONAL); // GC1NN15
        assertThat(parseMapPNG(bitmap, 84, 244, 12).getType()).isEqualTo(TRADITIONAL); // GC3E5JP
        assertThat(parseMapPNG(bitmap, 172, 16, 12).getType()).isEqualTo(MULTI); // GC1Z581
        assertThat(parseMapPNG(bitmap, 104, 20, 12).getType()).isEqualTo(MYSTERY); // GC2MENX
        assertThat(parseMapPNG(bitmap, 144, 60, 12).getType()).isEqualTo(TRADITIONAL); // GC1V3MG
        assertThat(parseMapPNG(bitmap, 228, 56, 12).isFound()).isTrue(); // GC36WZN
        assertThat(parseMapPNG(bitmap, 144, 212, 12).getType()).isEqualTo(MULTI); // GCR9GB
        assertThat(parseMapPNG(bitmap, 180, 68, 12).getType()).isEqualTo(MYSTERY); // GC3JZ1K
        assertThat(parseMapPNG(bitmap, 228, 104, 12).getType()).isEqualTo(MULTI); // GCQ95T
        assertThat(parseMapPNG(bitmap, 84, 220, 12).getType()).isEqualTo(MULTI); // GCWTVM
        assertThat(parseMapPNG(bitmap, 200, 228, 12).getType()).isEqualTo(MYSTERY); // GC3CC1A
        assertThat(parseMapPNG(bitmap, 204, 56, 12).getType()).isEqualTo(TRADITIONAL); // GC1K0WX
        assertThat(parseMapPNG(bitmap, 244, 208, 12).getType()).isEqualTo(MYSTERY); // GC1JVXG
        assertThat(parseMapPNG(bitmap, 84, 128, 12).getType()).isEqualTo(TRADITIONAL); // GC2XQ6C
        assertThat(parseMapPNG(bitmap, 248, 164, 12).getType()).isEqualTo(MYSTERY); // GC3B1JK
        assertThat(parseMapPNG(bitmap, 232, 84, 12).getType()).isEqualTo(MYSTERY); // GC3AT8J
        assertThat(parseMapPNG(bitmap, 160, 88, 12).isFound()).isTrue(); // GC2MB4P
        assertThat(parseMapPNG(bitmap, 132, 20, 12).getType()).isEqualTo(MYSTERY); // GC2NW3F
        assertThat(parseMapPNG(bitmap, 56, 132, 12).getType()).isEqualTo(MYSTERY); // GC22ERA
        assertThat(parseMapPNG(bitmap, 28, 32, 12).getType()).isEqualTo(MYSTERY); // GC2EFFK
    }

    public void testParseExtraMap5() {
        final Bitmap bitmap = getBitmap(map5);

        assertThat(parseMapPNG(bitmap, 60, 32, 12).getType()).isEqualTo(TRADITIONAL); // GC31DNK
        assertThat(parseMapPNG(bitmap, 200, 120, 12).getType()).isEqualTo(TRADITIONAL); // GCP89K
        assertThat(parseMapPNG(bitmap, 144, 152, 12).getType()).isEqualTo(TRADITIONAL); // GC22AR8
        assertThat(parseMapPNG(bitmap, 164, 92, 12).getType()).isEqualTo(MULTI); // GC1MFB7
        assertThat(parseMapPNG(bitmap, 16, 212, 12).getType()).isEqualTo(MYSTERY); // GC12F2K
        assertThat(parseMapPNG(bitmap, 188, 12, 12).getType()).isEqualTo(MYSTERY); // GC24J14
        assertThat(parseMapPNG(bitmap, 36, 72, 12).getType()).isEqualTo(MYSTERY); // GC2J8MY
        assertThat(parseMapPNG(bitmap, 152, 140, 12).getType()).isEqualTo(MYSTERY); // GC1H9WQ
        assertThat(parseMapPNG(bitmap, 44, 40, 12).getType()).isEqualTo(TRADITIONAL); // GC31DNZ
        assertThat(parseMapPNG(bitmap, 8, 152, 12).getType()).isEqualTo(MYSTERY); // GC34YFB
        assertThat(parseMapPNG(bitmap, 200, 216, 12).getType()).isEqualTo(MYSTERY); // GC30MK5
        assertThat(parseMapPNG(bitmap, 84, 20, 12).getType()).isEqualTo(TRADITIONAL); // GC304YY
        assertThat(parseMapPNG(bitmap, 192, 236, 12).getType()).isEqualTo(MULTI); // GC1D6AC
        assertThat(parseMapPNG(bitmap, 220, 48, 12).getType()).isEqualTo(MULTI); // GC1HQ8Y
        assertThat(parseMapPNG(bitmap, 136, 176, 12).getType()).isEqualTo(MYSTERY); // GC310B7
        assertThat(parseMapPNG(bitmap, 132, 232, 12).getType()).isEqualTo(MYSTERY); // GC12CR5
        assertThat(parseMapPNG(bitmap, 240, 40, 12).isFound()).isTrue(); // GC24GW1
        assertThat(parseMapPNG(bitmap, 140, 116, 12).getType()).isEqualTo(MYSTERY); // GC2YYE7
        assertThat(parseMapPNG(bitmap, 124, 144, 12).getType()).isEqualTo(MYSTERY); // GC111RZ
        assertThat(parseMapPNG(bitmap, 48, 128, 12).getType()).isEqualTo(TRADITIONAL); // GC13A7V
        assertThat(parseMapPNG(bitmap, 136, 92, 12).getType()).isEqualTo(MULTI); // GC2BKW9
        assertThat(parseMapPNG(bitmap, 200, 184, 12).getType()).isEqualTo(MYSTERY); // GC30X0C
        assertThat(parseMapPNG(bitmap, 156, 200, 12).getType()).isEqualTo(TRADITIONAL); // GC17V4A
        assertThat(parseMapPNG(bitmap, 160, 120, 12).getType()).isEqualTo(MYSTERY); // GC2ZBWW
        assertThat(parseMapPNG(bitmap, 196, 36, 12).getType()).isEqualTo(MYSTERY); // GC14X25
        assertThat(parseMapPNG(bitmap, 192, 100, 12).getType()).isEqualTo(MULTI); // GC1HXAX
        assertThat(parseMapPNG(bitmap, 108, 168, 12).getType()).isEqualTo(MYSTERY); // GC3C043
        assertThat(parseMapPNG(bitmap, 232, 28, 12).getType()).isEqualTo(MYSTERY); // GC1TEAR
        assertThat(parseMapPNG(bitmap, 200, 204, 12).getType()).isEqualTo(MYSTERY); // GC3AKFV
        assertThat(parseMapPNG(bitmap, 228, 28, 12).isFound()).isTrue(); // GC2NMPR
        //assertThat(parseMapPNG(bitmap, 232, 252, 12).getType()).isEqualTo(VIRTUAL); // GC1AH0N - False detection
        assertThat(parseMapPNG(bitmap, 220, 188, 12).getType()).isEqualTo(MYSTERY); // GC1ZXDK
        assertThat(parseMapPNG(bitmap, 168, 212, 12).getType()).isEqualTo(MYSTERY); // GC3A919
        assertThat(parseMapPNG(bitmap, 152, 176, 12).getType()).isEqualTo(MULTI); // GC196WN
        assertThat(parseMapPNG(bitmap, 144, 180, 12).getType()).isEqualTo(MYSTERY); // GC12RE5
        assertThat(parseMapPNG(bitmap, 176, 116, 12).getType()).isEqualTo(MYSTERY); // GC1DY2M
        assertThat(parseMapPNG(bitmap, 44, 212, 12).getType()).isEqualTo(TRADITIONAL); // GC3MRNT
        assertThat(parseMapPNG(bitmap, 220, 36, 12).getType()).isEqualTo(TRADITIONAL); // GC3CWZD
        assertThat(parseMapPNG(bitmap, 48, 160, 12).getType()).isEqualTo(MYSTERY); // GC1A8E3
        assertThat(parseMapPNG(bitmap, 8, 252, 12).getType()).isEqualTo(TRADITIONAL); // GC10W6W
        assertThat(parseMapPNG(bitmap, 60, 92, 12).getType()).isEqualTo(MYSTERY); // GC2D9DD
        assertThat(parseMapPNG(bitmap, 96, 164, 12).getType()).isEqualTo(MYSTERY); // GC1Z4QX
        assertThat(parseMapPNG(bitmap, 220, 252, 12).getType()).isEqualTo(TRADITIONAL); // GCNEGK
        assertThat(parseMapPNG(bitmap, 32, 188, 12).getType()).isEqualTo(MULTI); // GC10916
        assertThat(parseMapPNG(bitmap, 204, 224, 12).getType()).isEqualTo(TRADITIONAL); // GC1CA2Y
        assertThat(parseMapPNG(bitmap, 120, 236, 12).getType()).isEqualTo(MYSTERY); // GC11B3J
        assertThat(parseMapPNG(bitmap, 248, 24, 12).getType()).isEqualTo(TRADITIONAL); // GCKX8C
        assertThat(parseMapPNG(bitmap, 128, 152, 12).getType()).isEqualTo(TRADITIONAL); // GC2V6AA
        assertThat(parseMapPNG(bitmap, 196, 48, 12).getType()).isEqualTo(TRADITIONAL); // GC2YG95
        assertThat(parseMapPNG(bitmap, 48, 64, 12).getType()).isEqualTo(MULTI); // GCHGR8
        assertThat(parseMapPNG(bitmap, 188, 96, 12).getType()).isEqualTo(EVENT); // GC3KBPK
        assertThat(parseMapPNG(bitmap, 208, 140, 12).getType()).isEqualTo(MYSTERY); // GC1C9B0
        assertThat(parseMapPNG(bitmap, 164, 100, 12).getType()).isEqualTo(MYSTERY); // GC29JGA
        assertThat(parseMapPNG(bitmap, 156, 28, 12).getType()).isEqualTo(TRADITIONAL); // GCN690
        assertThat(parseMapPNG(bitmap, 232, 20, 12).getType()).isEqualTo(MYSTERY); // GC18Z53
        assertThat(parseMapPNG(bitmap, 220, 152, 12).getType()).isEqualTo(TRADITIONAL); // GC18RB6
        assertThat(parseMapPNG(bitmap, 200, 248, 12).getType()).isEqualTo(MYSTERY); // GC2378H
        assertThat(parseMapPNG(bitmap, 248, 244, 12).getType()).isEqualTo(TRADITIONAL); // GCV8QA
        assertThat(parseMapPNG(bitmap, 12, 232, 12).getType()).isEqualTo(MYSTERY); // GC2MXDG
        assertThat(parseMapPNG(bitmap, 48, 248, 12).getType()).isEqualTo(MYSTERY); // GCTHJR
        assertThat(parseMapPNG(bitmap, 216, 200, 12).getType()).isEqualTo(MULTI); // GC1EPM5
        assertThat(parseMapPNG(bitmap, 232, 60, 12).getType()).isEqualTo(MYSTERY); // GC2N0PB
        assertThat(parseMapPNG(bitmap, 88, 56, 12).getType()).isEqualTo(MULTI); // GC1ZWNX
        assertThat(parseMapPNG(bitmap, 248, 56, 12).getType()).isEqualTo(TRADITIONAL); // GC1N11P
        assertThat(parseMapPNG(bitmap, 100, 180, 12).getType()).isEqualTo(MYSTERY); // GCM6AE
        assertThat(parseMapPNG(bitmap, 220, 124, 12).getType()).isEqualTo(TRADITIONAL); // GC2A1RQ
        assertThat(parseMapPNG(bitmap, 212, 4, 12).isFound()).isTrue(); // GC1TVKE
        assertThat(parseMapPNG(bitmap, 28, 212, 12).getType()).isEqualTo(TRADITIONAL); // GC2A1RR
        assertThat(parseMapPNG(bitmap, 128, 84, 12).getType()).isEqualTo(MYSTERY); // GC16AWC
        assertThat(parseMapPNG(bitmap, 220, 16, 12).getType()).isEqualTo(MULTI); // GC282V9
        assertThat(parseMapPNG(bitmap, 112, 240, 12).getType()).isEqualTo(MYSTERY); // GC18VT5
        assertThat(parseMapPNG(bitmap, 80, 248, 12).getType()).isEqualTo(MYSTERY); // GC10YEK
        assertThat(parseMapPNG(bitmap, 224, 228, 12).getType()).isEqualTo(MYSTERY); // GC1EA70
        assertThat(parseMapPNG(bitmap, 232, 244, 12).getType()).isEqualTo(MYSTERY); // GC14PNY
        assertThat(parseMapPNG(bitmap, 108, 32, 12).getType()).isEqualTo(TRADITIONAL); // GC2MMPN
        assertThat(parseMapPNG(bitmap, 144, 188, 12).getType()).isEqualTo(TRADITIONAL); // GC1CCF4
        assertThat(parseMapPNG(bitmap, 228, 208, 12).getType()).isEqualTo(TRADITIONAL); // GCV8C2
        assertThat(parseMapPNG(bitmap, 104, 252, 12).getType()).isEqualTo(MYSTERY); // GCTRPF
        assertThat(parseMapPNG(bitmap, 176, 92, 12).getType()).isEqualTo(TRADITIONAL); // GCRF8G
        assertThat(parseMapPNG(bitmap, 120, 140, 12).getType()).isEqualTo(MYSTERY); // GC210B9
        assertThat(parseMapPNG(bitmap, 204, 240, 12).getType()).isEqualTo(TRADITIONAL); // GC16NTW
        assertThat(parseMapPNG(bitmap, 192, 224, 12).getType()).isEqualTo(MYSTERY); // GC2PTVN
        assertThat(parseMapPNG(bitmap, 76, 116, 12).getType()).isEqualTo(MULTI); // GC1RPG0
        assertThat(parseMapPNG(bitmap, 144, 200, 12).getType()).isEqualTo(MYSTERY); // GC1FZ4T
        assertThat(parseMapPNG(bitmap, 172, 36, 12).getType()).isEqualTo(MYSTERY); // GC1ZYG8
        assertThat(parseMapPNG(bitmap, 248, 196, 12).getType()).isEqualTo(TRADITIONAL); // GC17FJQ
        assertThat(parseMapPNG(bitmap, 88, 140, 12).getType()).isEqualTo(MULTI); // GC1KWK0
        assertThat(parseMapPNG(bitmap, 168, 196, 12).getType()).isEqualTo(MYSTERY); // GC17MNG
        assertThat(parseMapPNG(bitmap, 20, 252, 12).getType()).isEqualTo(MULTI); // GC13M6V
        assertThat(parseMapPNG(bitmap, 120, 172, 12).getType()).isEqualTo(MYSTERY); // GC3B30A
        assertThat(parseMapPNG(bitmap, 104, 92, 12).getType()).isEqualTo(TRADITIONAL); // GC2GY9D
        assertThat(parseMapPNG(bitmap, 128, 120, 12).getType()).isEqualTo(MYSTERY); // GC2Y90M
        assertThat(parseMapPNG(bitmap, 204, 40, 12).isFound()).isTrue(); // GC1BZ6P
        assertThat(parseMapPNG(bitmap, 56, 76, 12).getType()).isEqualTo(MYSTERY); // GC10K7X
        assertThat(parseMapPNG(bitmap, 196, 108, 12).getType()).isEqualTo(MULTI); // GC1F0R5
        assertThat(parseMapPNG(bitmap, 120, 196, 12).getType()).isEqualTo(MYSTERY); // GC1KQQW
    }

    public void testParseExtraMap11() {
        final Bitmap bitmap = getBitmap(map11);
        assertThat(parseMapPNG(bitmap, 132, 16, 11).getType()).isEqualTo(EVENT);
        assertThat(parseMapPNG(bitmap, 104, 48, 11).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 128, 124, 11).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 228, 8, 11).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 160, 156, 11).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 208, 176, 11).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 252, 24, 11).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 216, 96, 11).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 24, 212, 11).getType()).isEqualTo(EARTH);
    }

    public void testParseExtraMapall14() {
        final Bitmap bitmap = getBitmap(map_all14);
        assertThat(parseMapPNG(bitmap, 40, 16, 14).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 72, 16, 14).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 100, 16, 14).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 128, 16, 14).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 44, 44, 14).isOwner()).isTrue();
        assertThat(parseMapPNG(bitmap, 76, 44, 14).isOwner()).isTrue();
        assertThat(parseMapPNG(bitmap, 132, 44, 14).isOwner()).isTrue();
        assertThat(parseMapPNG(bitmap, 40, 72, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 72, 72, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 100, 72, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 128, 72, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 40, 96, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 72, 96, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 100, 96, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 128, 96, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 40, 124, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 72, 124, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 100, 124, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 128, 124, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 40, 160, 14).getType()).isEqualTo(WHERIGO);
        assertThat(parseMapPNG(bitmap, 72, 160, 14).getType()).isEqualTo(WHERIGO);
        assertThat(parseMapPNG(bitmap, 100, 160, 14).getType()).isEqualTo(WHERIGO);
        assertThat(parseMapPNG(bitmap, 128, 160, 14).getType()).isEqualTo(WHERIGO);
        assertThat(parseMapPNG(bitmap, 40, 184, 14).getType()).isEqualTo(LETTERBOX);
        assertThat(parseMapPNG(bitmap, 72, 184, 14).getType()).isEqualTo(LETTERBOX);
        assertThat(parseMapPNG(bitmap, 100, 184, 14).getType()).isEqualTo(LETTERBOX);
        assertThat(parseMapPNG(bitmap, 128, 184, 14).getType()).isEqualTo(LETTERBOX);

        assertThat(parseMapPNG(bitmap, 12, 224, 14).getType()).isEqualTo(CITO);
        assertThat(parseMapPNG(bitmap, 40, 220, 14).getType()).isEqualTo(EVENT);
        assertThat(parseMapPNG(bitmap, 68, 224, 14).getType()).isEqualTo(EARTH);
        assertThat(parseMapPNG(bitmap, 96, 224, 14).getType()).isEqualTo(MEGA_EVENT);
        assertThat(parseMapPNG(bitmap, 120, 224, 14).getType()).isEqualTo(WEBCAM);
        assertThat(parseMapPNG(bitmap, 144, 224, 14).getType()).isEqualTo(VIRTUAL);
    }
}
