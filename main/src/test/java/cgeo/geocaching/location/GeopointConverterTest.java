package cgeo.geocaching.location;

import cgeo.geocaching.utils.functions.Func1;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class GeopointConverterTest {

    private static final double DELTA = 1e-10;

    private static class TestPoint {
        final double lat;
        final double lon;
        TestPoint(final double lat, final double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    private final GeopointConverter<TestPoint> converter =
            new GeopointConverter<>(
                    // Geopoint -> TestPoint
                    gp -> new TestPoint(gp.getLatitude(), gp.getLongitude()),
                    // TestPoint -> Geopoint
                    tp -> new Geopoint(tp.lat, tp.lon)
            );


    // convert to other type
    @Test
    public void to_shouldConvertGeopointToOtherType() {
        Geopoint src = new Geopoint(48.858222, 2.2945);
        TestPoint tp = converter.to(src);

        assertNotNull(tp);
        assertEquals(src.getLatitude(), tp.lat, DELTA);
        assertEquals(src.getLongitude(), tp.lon, DELTA);
    }

    @Test
    public void from_shouldConvertOtherTypeToGeopoint() {
        TestPoint tp = new TestPoint(-33.8675, 151.2070);   // Sydney
        Geopoint gp = converter.from(tp);

        assertNotNull(gp);
        assertEquals(tp.lat, gp.getLatitude(), DELTA);
        assertEquals(tp.lon, gp.getLongitude(), DELTA);
    }

    @Test
    public void toAndFrom_shouldTreatNullAsNull() {
        assertNull(converter.to(null));
        assertNull(converter.from(null));
    }

    // converting in a batch
    @Test
    public void toList_shouldConvertCollectionPreservingOrder() {
        List<Geopoint> src = Arrays.asList(
                new Geopoint(0, 0),
                new Geopoint(1, 1),
                new Geopoint(2, 2)
        );

        List<TestPoint> dst = converter.toList(src);

        assertEquals(src.size(), dst.size());
        for (int i = 0; i < src.size(); i++) {
            assertEquals(src.get(i).getLatitude(),  dst.get(i).lat, DELTA);
            assertEquals(src.get(i).getLongitude(), dst.get(i).lon, DELTA);
        }
    }

    @Test
    public void fromList_shouldConvertCollectionPreservingOrder() {
        List<TestPoint> src = Arrays.asList(
                new TestPoint(10, 20),
                new TestPoint(30, 40)
        );

        List<Geopoint> dst = converter.fromList(src);

        assertEquals(src.size(), dst.size());
        for (int i = 0; i < src.size(); i++) {
            assertEquals(src.get(i).lat, dst.get(i).getLatitude(), DELTA);
            assertEquals(src.get(i).lon, dst.get(i).getLongitude(), DELTA);
        }
    }

    @Test
    public void listMethods_shouldReturnEmptyListWhenInputNull() {
        assertTrue(converter.toList(null).isEmpty());
        assertTrue(converter.fromList(null).isEmpty());
    }

    @Test
    public void listMethods_shouldHandleEmptyInputList() {
        assertTrue(converter.toList(Collections.emptyList()).isEmpty());
        assertTrue(converter.fromList(Collections.emptyList()).isEmpty());
    }
}
