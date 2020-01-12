package cgeo.geocaching.location;

import static cgeo.geocaching.location.UTMPoint.latLong2UTM;

import java.util.Arrays;
import java.util.Collection;
import static java.lang.String.valueOf;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Test the UTMPoint class with testcases from an Excel sheet provided here: http://www.uwgb.edu/dutchs/UsefulData/UTMFormulas.htm
 *
 * Other Tools to verify results:
 * Online Batch Converter: http://www.hamstermap.com/
 * http://www.latlong.net/lat-long-utm.html
 * http://www.rcn.montana.edu/resources/converter.aspx
 * http://www.synnatschke.de/geo-tools/coordinate-converter.php
 */
@RunWith(Parameterized.class)
public class UTMPointConversionTest {

    private final double lat;
    private final double lon;
    private final int zone;
    private final String zoneLetter;
    private final double easting;
    private final double northing;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // lat, lon, zone, zoneLetter, easting, northing
                {85d, 102d, 48, "Z", 470821.25d, 9440493.90d},
                {84d, 102d, 48, "X", 465005.34493d, 9329005.2d},
                {51d, 60d, 41, "U", 289511.142963d, 5654109.2d},
                {37d, 151d, 56, "S", 322037.81022d, 4096742.1d},
                {24d, 14d, 33, "R", 398285.44617d, 2654587.6d},
                {-21d, 17d, 33, "K", 707889.2160d, 7676551.7d},
                {-36d, 85d, 45, "H", 319733.3418d, 6014201.8d},
                {-50d, 117d, 50, "F", 500000d, 4461369.3d},
                {-68d, 146d, 55, "D", 458196.7256d, 2456797.5d},
                {-81d, 171d, 59, "B", 500000d, 1006795.6d}, // should be B, but some lib say Z
                {-83d, -12d, 29, "A", 459200.2563d, 782480.6d}, // should be A, but some lib say Z
                {-61d, -164d, 3, "E", 554084.38101d, 3236799.8d},
                {-43d, -92d, 15, "G", 581508.6478d, 5238700.1d},
                {-21d, -53d, 22, "K", 292110.7839d, 7676551.7d},
                {15d, -33d, 25, "P", 500000d, 1658326.0d},
                {32d, -122d, 10, "S", 594457.4634d, 3540872.5d},
                {57d, -82d, 17, "V", 439253.3763d, 6317830.5d},
                {74d, -177d, 1, "X", 500000d, 8212038.1d},
                {-55.98d, -67.28917d, 19, "F", 606750d, 3794825d},
                {-61.453333d, -55.495752d, 21, "E", 580191d, 3185792d},
                {37.81972d, -122.47861d, 10, "S", 545889d, 4185941d},
                {40.713d, -74.0135d, 18, "T", 583326d, 4507366d},
                {3.15785d, 101.71165d, 47, "N", 801396d, 349434d},
                {27.98806d, 86.92528d, 45, "R", 492652d, 3095881d},
                {-3.07583d, 37.35333d, 37, "M", 317004d, 9659884d},
                {25.19714d, 55.27411d, 40, "R", 326103d, 2787892d},
                {41.00855d, 28.97994d, 35, "T", 666499d, 4541594d},
                {41.90222d, 12.45333d, 33, "T", 288761d, 4642057d},
                {48.85822d, 2.2945d, 31, "U", 448252d, 5411935d},
                {71.1725d, 25.78444d, 35, "W", 456220d, 7897075d},
                {-34.35806d, 18.47194d, 34, "H", 267496d, 6195246d},
                {83.627d, -32.664d, 25, "X", 504163.90d, 9286465.67d},
                {-55.98d, -67.28917d, 19, "F", 606750d, 3794825d},
                {-61.453333d, -55.495752d, 21, "E", 580191d, 3185792},
                {37.81972d, -122.47861d, 10, "S", 545889d, 4185941},
                {40.713d, -74.0135d, 18, "T", 583326d, 4507366},
                {3.15785d, 101.71165d, 47, "N", 801396d, 349434},
                {27.98806d, 86.92528d, 45, "R", 492652d, 3095881},
                {-3.07583d, 37.35333d, 37, "M", 317004d, 9659884},
                {25.19714d, 55.27411d, 40, "R", 326103d, 2787892},
                {41.00855d, 28.97994d, 35, "T", 666499d, 4541594},
                {41.90222d, 12.45333d, 33, "T", 288761d, 4642057},
                {48.85822d, 2.2945d, 31, "U", 448252d, 5411935},
                {71.1725d, 25.78444d, 35, "W", 456220d, 7897075},
                {-34.35806d, 18.47194d, 34, "H", 267496d, 6195246},
                {-13.16333d, -72.54556d, 18, "L", 766062d, 8543503},
                {-32.65343d, -70.01108d, 19, "H", 405179d, 6386681},
                {-43.59575d, 170.14104d, 59, "G", 430668d, 5172667},
                {-33.85867d, 151.21403d, 56, "H", 334786d, 6252080},
                {25.03361d, 121.565d, 51, "R", 355224d, 2769437},
                {35.35806d, 138.73111d, 54, "S", 293848d, 3915114},
                {56.05611d, 160.64417d, 57, "V", 602389d, 6213543},
                {71.38889d, -156.47917d, 4, "W", 589769d, 7922642},
                {21.365d, -157.95d, 4, "Q", 608862d, 2362907},
                {-27.11667d, -109.36667d, 12, "J", 661897d, 6999591},
                {-37.11111d, -12.28833d, 28, "H", 740947d, 5889360},
                {-77.85d, 166.66667d, 58, "C", 539154d, 1357814},

                // some testcases from geocaching.com

                // https://www.geocaching.com/geocache/GC4DF1X_my-precious
                {82.42825d, -62.1368333d, 20, "X", 512697d, 9152728},

                // https://www.geocaching.com/geocache/GCC3E3_byrd-surface-camp
                {-80.01405d, -119.5220833d, 11, "A", 451190, 1115787}, // should be A, but some lib say Z

                // https://www.geocaching.com/geocache/GC636CW_bulandet-kapell
                // Zone 32V is bigger, because Norway wanted to be in one Zone only
                {61.2832667d, 4.6271d, 32, "V", 265719, 6802185},

                // https://www.geocaching.com/geocache/GC2Y0BW_panelian-aseman-metsa
                {61.19925d, 21.9684667, 34, "V", 552050, 6785366}
        });
    }

    public UTMPointConversionTest(final double lat, final double lon, final int zone, final String zoneLetter, final double easting, final double northing) {
        this.lat = lat;
        this.lon = lon;
        this.zone = zone;
        this.zoneLetter = zoneLetter;
        this.easting = easting;
        this.northing = northing;
    }

    @Test
    public void testLatLong2UTM() throws Exception {
        final UTMPoint utm = latLong2UTM(new Geopoint(this.lat, this.lon));
        assertThat(utm.getEasting()).isEqualTo(this.easting, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(this.northing, offset(1.1d));
        assertThat(utm.getZoneNumber()).isEqualTo(zone);
        if ("ABY".contains(this.zoneLetter)) { // if we expect A,B or Y then Z is ok, too
            assertThat(valueOf(utm.getZoneLetter())).isIn(zoneLetter, "Z");
        } else {
            assertThat(valueOf(utm.getZoneLetter())).isEqualTo(zoneLetter);
        }
    }

    @Test
    public void testUTM2LatLong() throws Exception {
        final Geopoint gp = new UTMPoint(this.zone, this.zoneLetter.charAt(0), this.easting, this.northing).toLatLong();
        assertThat(gp.getLatitude()).isEqualTo(this.lat, offset(1.1d));
        assertThat(gp.getLongitude()).isEqualTo(this.lon, offset(1.1d));
    }

}
