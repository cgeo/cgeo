package cgeo.geocaching.connector.gc;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.RawRes;

import static cgeo.geocaching.enumerations.CacheType.CITO;
import static cgeo.geocaching.enumerations.CacheType.EARTH;
import static cgeo.geocaching.enumerations.CacheType.EVENT;
import static cgeo.geocaching.enumerations.CacheType.MULTI;
import static cgeo.geocaching.enumerations.CacheType.MYSTERY;
import static cgeo.geocaching.enumerations.CacheType.TRADITIONAL;
import static cgeo.geocaching.enumerations.CacheType.UNKNOWN;
import static cgeo.geocaching.enumerations.CacheType.VIRTUAL;
import static cgeo.geocaching.test.R.raw.map1_z13;
import static cgeo.geocaching.test.R.raw.map2_z13;
import static cgeo.geocaching.test.R.raw.map3_z12;
import static cgeo.geocaching.test.R.raw.map4_z12;
import static cgeo.geocaching.test.R.raw.map5_z15;
import static cgeo.geocaching.test.R.raw.map6_z11;
import static cgeo.geocaching.test.R.raw.map7_z14;
import static cgeo.geocaching.test.R.raw.map_all14;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class IconDecoderTest extends AbstractResourceInstrumentationTestCase {

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

    public void testParseMap1() {
        final Bitmap bitmap = getBitmap(map1_z13);
        assertThat(parseMapPNG(bitmap, 76, 30, 13).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 137, 98, 13).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 127, 76, 13).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 188, 114, 13).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 188, 174, 13).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 120, 93, 13).getType()).isEqualTo(UNKNOWN); // disabled TRADITIONAL
    }

    public void testParseMap2() {
        final Bitmap bitmap = getBitmap(map2_z13);
        assertThat(parseMapPNG(bitmap, 84, 168, 13).getType()).isEqualTo(EARTH);
        assertThat(parseMapPNG(bitmap, 184, 74, 13).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 198, 86, 13).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 200, 100, 13).getType()).isEqualTo(MYSTERY);
    }

    public void testParseMap3() {
        final Bitmap bitmap = getBitmap(map3_z12);
        assertThat(parseMapPNG(bitmap, 98, 192, 12).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 147, 182, 12).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 97, 232, 12).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 174, 193, 12).getType()).isEqualTo(UNKNOWN); // disabled TRADITIONAL
        assertThat(parseMapPNG(bitmap, 172, 76, 12).getType()).isEqualTo(UNKNOWN); // disabled MYSTERY
        assertThat(parseMapPNG(bitmap, 252, 148, 13).getType()).isEqualTo(TRADITIONAL);
    }

    public void testParseMap4() {
        final Bitmap bitmap = getBitmap(map4_z12);
        assertThat(parseMapPNG(bitmap, 140, 64, 12).isOwner()).isTrue();
        assertThat(parseMapPNG(bitmap, 143, 17, 12).isOwner()).isTrue();
        assertThat(parseMapPNG(bitmap, 52, 52, 12).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 179, 124, 12).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 78, 178, 12).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 157, 104, 12).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 21, 206, 13).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 184, 225, 12).getType()).isEqualTo(UNKNOWN); // disabled TRADITIONAL
    }

    public void testParseMap5() {
        final Bitmap bitmap = getBitmap(map5_z15);
        assertThat(parseMapPNG(bitmap, 56, 214, 15).getType()).isEqualTo(EVENT);
        assertThat(parseMapPNG(bitmap, 74, 78, 15).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 96, 131, 15).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 114, 218, 15).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 199, 216, 15).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 101, 224, 12).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 7, 248, 15).getType()).isEqualTo(MULTI);
    }

    public void testParseMap6() {
        final Bitmap bitmap = getBitmap(map6_z11);
        assertThat(parseMapPNG(bitmap, 243, 196, 11).getType()).isEqualTo(EARTH); // GC56E9 wrong, it's a VIRTUAL
        assertThat(parseMapPNG(bitmap, 250, 198, 11).getType()).isEqualTo(EARTH); // GC6F12 wrong, it's a VIRTUAL
        assertThat(parseMapPNG(bitmap, 224, 106, 11).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 48, 78, 11).getType()).isEqualTo(EVENT); // GC6W5TP
        assertThat(parseMapPNG(bitmap, 82, 203, 11).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 16, 246, 11).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 34, 168, 11).getType()).isEqualTo(TRADITIONAL);
    }

    public void testParseMap7() {
        final Bitmap bitmap = getBitmap(map7_z14);
        assertThat(parseMapPNG(bitmap, 25, 41, 14).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 44, 62, 14).isFound()).isTrue();
        assertThat(parseMapPNG(bitmap, 153, 124, 14).getType()).isEqualTo(EVENT);
        assertThat(parseMapPNG(bitmap, 136, 55, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 74, 145, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 168, 196, 14).getType()).isEqualTo(UNKNOWN); // disabled TRADITIONAL
        assertThat(parseMapPNG(bitmap, 251, 104, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 196, 205, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 238, 229, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 103, 180, 14).getType()).isEqualTo(MYSTERY);
    }

    public void testParseMapAll() {
        final Bitmap bitmap = getBitmap(map_all14);
        assertThat(parseMapPNG(bitmap, 25, 14, 14).getType()).isEqualTo(TRADITIONAL);
        assertThat(parseMapPNG(bitmap, 76, 14, 14).getType()).isEqualTo(TRADITIONAL); // PROJECT_APE
        assertThat(parseMapPNG(bitmap, 126, 14, 14).getType()).isEqualTo(UNKNOWN); // GCHQ
        assertThat(parseMapPNG(bitmap, 176, 14, 14).getType()).isEqualTo(MULTI);
        assertThat(parseMapPNG(bitmap, 226, 14, 14).getType()).isEqualTo(EVENT);

        assertThat(parseMapPNG(bitmap, 26, 46, 14).getType()).isEqualTo(CITO);
        assertThat(parseMapPNG(bitmap, 75, 46, 14).getType()).isEqualTo(EVENT); // MEGA_EVENT
        assertThat(parseMapPNG(bitmap, 125, 46, 14).getType()).isEqualTo(EVENT); // GIGA_EVENT
        assertThat(parseMapPNG(bitmap, 174, 46, 14).getType()).isEqualTo(EVENT); // GPS_EXHIBIT
        assertThat(parseMapPNG(bitmap, 225, 46, 14).getType()).isEqualTo(EARTH);

        assertThat(parseMapPNG(bitmap, 24, 76, 14).getType()).isEqualTo(VIRTUAL);
        assertThat(parseMapPNG(bitmap, 76, 76, 14).getType()).isEqualTo(VIRTUAL); // WEBCAM
        assertThat(parseMapPNG(bitmap, 125, 76, 14).getType()).isEqualTo(UNKNOWN); // LOCATIONLESS
        assertThat(parseMapPNG(bitmap, 176, 76, 14).getType()).isEqualTo(MYSTERY);
        assertThat(parseMapPNG(bitmap, 224, 76, 14).getType()).isEqualTo(MYSTERY); // LETTERBOX

        assertThat(parseMapPNG(bitmap, 75, 107, 14).getType()).isEqualTo(MYSTERY); // WHERIGO
        assertThat(parseMapPNG(bitmap, 124, 107, 14).isOwner()).isTrue();
        assertThat(parseMapPNG(bitmap, 176, 107, 14).isFound()).isTrue();

        // disabled caches
        assertThat(parseMapPNG(bitmap, 24, 136, 14).getType()).isEqualTo(UNKNOWN); // TRADITIONAL
        assertThat(parseMapPNG(bitmap, 75, 136, 14).getType()).isEqualTo(UNKNOWN); // PROJECT_APE
        assertThat(parseMapPNG(bitmap, 126, 136, 14).getType()).isEqualTo(UNKNOWN); // GCHQ
        assertThat(parseMapPNG(bitmap, 174, 136, 14).getType()).isEqualTo(UNKNOWN); // MULTI
        assertThat(parseMapPNG(bitmap, 225, 136, 14).getType()).isEqualTo(UNKNOWN); // EVENT

        assertThat(parseMapPNG(bitmap, 26, 164, 14).getType()).isEqualTo(UNKNOWN); // CITO
        assertThat(parseMapPNG(bitmap, 75, 164, 14).getType()).isEqualTo(MYSTERY); // MEGA_EVENT
        assertThat(parseMapPNG(bitmap, 125, 164, 14).getType()).isEqualTo(UNKNOWN); // GIGA_EVENT
        assertThat(parseMapPNG(bitmap, 174, 164, 14).getType()).isEqualTo(UNKNOWN); // GPS_EXHIBIT
        assertThat(parseMapPNG(bitmap, 225, 164, 14).getType()).isEqualTo(UNKNOWN); // EARTH

        assertThat(parseMapPNG(bitmap, 24, 193, 14).getType()).isEqualTo(UNKNOWN); // VIRTUAL
        assertThat(parseMapPNG(bitmap, 76, 193, 14).getType()).isEqualTo(UNKNOWN); // WEBCAM
        assertThat(parseMapPNG(bitmap, 125, 193, 14).getType()).isEqualTo(UNKNOWN); // LOCATIONLESS
        assertThat(parseMapPNG(bitmap, 176, 193, 14).getType()).isEqualTo(UNKNOWN); // MYSTERY
        assertThat(parseMapPNG(bitmap, 224, 193, 14).getType()).isEqualTo(UNKNOWN); // LETTERBOX

        assertThat(parseMapPNG(bitmap, 75, 224, 14).getType()).isEqualTo(UNKNOWN); // WHERIGO
        assertThat(parseMapPNG(bitmap, 124, 224, 14).getType()).isEqualTo(UNKNOWN); // OWN
        assertThat(parseMapPNG(bitmap, 176, 224, 14).getType()).isEqualTo(UNKNOWN); // FOUND
    }
}
