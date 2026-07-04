package cgeo.geocaching.wherigo.openwig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Exercises {@link Zone#walk}, which is pure point-in-polygon/distance geometry with no
 * dependency on {@link Engine} - except for the "Active" Lua setter, which is why this test
 * activates the zone via reflection instead of going through that setter. Assertions read the
 * private "ncontain" field (via reflection) rather than the public "contain" field, since walk()
 * only ever updates "ncontain" - "contain" is only synced from it later, by setcontain()/tick().
 */
public class ZoneTest {

    // roughly a 1.1km x 1.1km square at the equator, where 0.01 degrees is ~1113m in both axes
    private static final ZonePoint[] SQUARE = new ZonePoint[]{
        new ZonePoint(0.00, 0.00, 0),
        new ZonePoint(0.00, 0.01, 0),
        new ZonePoint(0.01, 0.01, 0),
        new ZonePoint(0.01, 0.00, 0),
    };

    private static Zone activatedZone(final ZonePoint[] points) throws Exception {
        final Zone zone = new Zone();
        zone.points = points;
        setPrivateField(zone, "active", true);
        invokePrivateNoArgMethod(zone, "preprocess");
        return zone;
    }

    private static void setPrivateField(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setPrivateField(final Zone zone, final String fieldName, final double value) throws Exception {
        setPrivateField(zone, fieldName, (Object) value);
    }

    private static void invokePrivateNoArgMethod(final Object target, final String methodName) throws Exception {
        final Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static int ncontain(final Zone zone) throws Exception {
        final Field field = Zone.class.getDeclaredField("ncontain");
        field.setAccessible(true);
        return (int) field.get(zone);
    }

    @Test
    public void pointAtCenterOfSquareIsInside() throws Exception {
        final Zone zone = activatedZone(SQUARE);

        zone.walk(new ZonePoint(0.005, 0.005, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.INSIDE);
        assertThat(zone.distance).isEqualTo(0.0);
    }

    @Test
    public void pointOnVertexResolvesToProximityNotInside() throws Exception {
        final Zone zone = activatedZone(SQUARE);

        // a vertex sits exactly on the bounding box edge, and the box check is a strict
        // inequality, so it never reaches the ray-casting test - it falls back to a
        // nearest-edge-point distance of 0, which classifies as PROXIMITY (not INSIDE)
        zone.walk(new ZonePoint(0.00, 0.00, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.PROXIMITY);
        assertThat(zone.distance).isEqualTo(0.0);
    }

    @Test
    public void pointFarAwayIsDistantWhenRangesAreUnlimited() throws Exception {
        final Zone zone = activatedZone(SQUARE);

        // ~157km away - clearly outside both the polygon and its proximity bounding box
        zone.walk(new ZonePoint(1.0, 1.0, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.DISTANT);
        assertThat(zone.distance).isGreaterThan(100_000);
    }

    @Test
    public void pointFarAwayIsNowhereWhenBeyondAnExplicitDistanceRange() throws Exception {
        final Zone zone = activatedZone(SQUARE);
        setPrivateField(zone, "distanceRange", 1000.0);
        invokePrivateNoArgMethod(zone, "preprocess");

        zone.walk(new ZonePoint(1.0, 1.0, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.NOWHERE);
    }

    @Test
    public void pointJustOutsideTheEdgeIsProximityByDefault() throws Exception {
        final Zone zone = activatedZone(SQUARE);

        // same latitude as the square's middle, ~222m west of its left edge (lon=0)
        zone.walk(new ZonePoint(0.005, -0.002, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.PROXIMITY);
        assertThat(zone.distance).isGreaterThan(0).isLessThan(1500);
    }

    @Test
    public void distanceForPointOutsideBoundingBoxIsMeasuredToNearestEdgePoint() throws Exception {
        final Zone zone = activatedZone(SQUARE);

        // due west of the square's left edge, same latitude as the bottom-left corner
        zone.walk(new ZonePoint(0.00, -0.002, 0));

        assertThat(zone.nearestPoint.longitude).isCloseTo(0.0, within(1e-9));
    }

    @Test
    public void inactiveZoneIsUnaffectedByWalk() throws Exception {
        final Zone zone = new Zone();
        zone.points = SQUARE;
        // deliberately not activated

        zone.walk(new ZonePoint(0.005, 0.005, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.NOWHERE);
    }

    @Test
    public void zoneWithNoPointsIsUnaffectedByWalk() throws Exception {
        final Zone zone = new Zone();
        setPrivateField(zone, "active", true);

        zone.walk(new ZonePoint(0.005, 0.005, 0));

        assertThat(ncontain(zone)).isEqualTo(Zone.NOWHERE);
    }

    @Test
    public void nullPointIsIgnoredByWalk() throws Exception {
        final Zone zone = activatedZone(SQUARE);

        zone.walk(null);

        assertThat(ncontain(zone)).isEqualTo(Zone.NOWHERE);
    }
}
