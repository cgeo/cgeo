package cgeo.geocaching.wherigo.openwig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for ZonePoint class.
 */
public class ZonePointTest {

    @Test
    public void testZonePointCreation() {
        ZonePoint point = new ZonePoint(50.0, 10.0, 100.0);
        
        assertNotNull("ZonePoint should be created", point);
        assertThat(point.latitude).isEqualTo(50.0);
        assertThat(point.longitude).isEqualTo(10.0);
        assertThat(point.altitude).isEqualTo(100.0);
    }

    @Test
    public void testZonePointCopy() {
        ZonePoint original = new ZonePoint(50.0, 10.0, 100.0);
        ZonePoint copy = ZonePoint.copy(original);
        
        assertNotNull("Copy should be created", copy);
        assertThat(copy.latitude).isEqualTo(original.latitude);
        assertThat(copy.longitude).isEqualTo(original.longitude);
        assertThat(copy.altitude).isEqualTo(original.altitude);
        
        // Verify it's a different object
        assertThat(copy).isNotSameAs(original);
    }

    @Test
    public void testZonePointDistance() {
        ZonePoint point1 = new ZonePoint(50.0, 10.0, 0);
        ZonePoint point2 = new ZonePoint(50.1, 10.1, 0);
        
        double distance = point1.distance(point2);
        
        assertThat(distance).isGreaterThan(0);
        assertThat(distance).isLessThan(20000); // Less than 20km
    }

    @Test
    public void testZonePointDistanceToSelf() {
        ZonePoint point = new ZonePoint(50.0, 10.0, 100.0);
        
        double distance = point.distance(point);
        
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    public void testZonePointBearing() {
        ZonePoint point1 = new ZonePoint(50.0, 10.0, 0);
        ZonePoint point2 = new ZonePoint(51.0, 10.0, 0);
        
        double bearing = point1.bearing(point2);
        
        assertThat(bearing).isGreaterThanOrEqualTo(-180.0);
        assertThat(bearing).isLessThanOrEqualTo(180.0);
    }

    @Test
    public void testZonePointWithNegativeCoordinates() {
        ZonePoint point = new ZonePoint(-50.0, -10.0, 0);
        
        assertThat(point.latitude).isEqualTo(-50.0);
        assertThat(point.longitude).isEqualTo(-10.0);
    }

    @Test
    public void testZonePointWithZeroAltitude() {
        ZonePoint point = new ZonePoint(50.0, 10.0, 0);
        
        assertThat(point.altitude).isEqualTo(0.0);
    }

    @Test
    public void testZonePointWithHighAltitude() {
        ZonePoint point = new ZonePoint(50.0, 10.0, 8848.0); // Mt. Everest height
        
        assertThat(point.altitude).isEqualTo(8848.0);
    }

    @Test
    public void testAngle2Azimuth() {
        double angle = 45.0;
        double azimuth = ZonePoint.angle2azimuth(angle);
        
        assertThat(azimuth).isGreaterThanOrEqualTo(0.0);
        assertThat(azimuth).isLessThan(360.0);
    }

    @Test
    public void testMultipleZonePoints() {
        ZonePoint[] points = new ZonePoint[4];
        points[0] = new ZonePoint(50.0, 10.0, 0);
        points[1] = new ZonePoint(50.1, 10.0, 0);
        points[2] = new ZonePoint(50.1, 10.1, 0);
        points[3] = new ZonePoint(50.0, 10.1, 0);
        
        for (ZonePoint point : points) {
            assertNotNull("Each point should be created", point);
        }
    }

    @Test
    public void testDistanceBetweenDistantPoints() {
        ZonePoint newYork = new ZonePoint(40.7128, -74.0060, 0);
        ZonePoint london = new ZonePoint(51.5074, -0.1278, 0);
        
        double distance = newYork.distance(london);
        
        // Distance should be roughly 5,500 km
        assertThat(distance).isGreaterThan(5000000); // > 5000 km
        assertThat(distance).isLessThan(6000000); // < 6000 km
    }
}
