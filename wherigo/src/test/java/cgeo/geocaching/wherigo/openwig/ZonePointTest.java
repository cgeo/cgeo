package cgeo.geocaching.wherigo.openwig;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class ZonePointTest {

    @Test
    public void distanceIsSymmetric() {
        final ZonePoint a = new ZonePoint(49.0, 16.0, 0);
        final ZonePoint b = new ZonePoint(49.01, 16.02, 0);
        // not perfectly symmetric: lon2m() uses the "to" point's latitude for the cos() term,
        // so a->b and b->a scale longitude degrees to metres slightly differently
        assertThat(a.distance(b)).isCloseTo(b.distance(a), within(0.5));
    }

    @Test
    public void distanceOfPointToItselfIsZero() {
        final ZonePoint a = new ZonePoint(49.0, 16.0, 0);
        assertThat(a.distance(a)).isEqualTo(0.0);
    }

    @Test
    public void distanceForOneDegreeOfLatitudeMatchesLatitudeCoefficient() {
        // ZonePoint uses its own flat-earth LATITUDE_COEF (110940.00...) rather than the
        // true WGS84 value (~111320m) for latitude degrees -> metres
        final ZonePoint a = new ZonePoint(49.0, 16.0, 0);
        final ZonePoint b = new ZonePoint(50.0, 16.0, 0);
        assertThat(a.distance(b)).isCloseTo(ZonePoint.LATITUDE_COEF, within(0.01));
    }

    @Test
    public void distanceShrinksAsLongitudeDegreesApproachThePoles() {
        // one degree of longitude covers less ground the closer you are to a pole
        final double distanceNearEquator = ZonePoint.distance(0, 0, 0, 1);
        final double distanceNearPole = ZonePoint.distance(80, 0, 80, 1);
        assertThat(distanceNearPole).isLessThan(distanceNearEquator);
    }

    @Test
    public void bearingToDueNorthPointIsPositiveNinetyDegreesInRadians() {
        final ZonePoint here = new ZonePoint(49.01, 16.0, 0);
        final ZonePoint south = new ZonePoint(49.0, 16.0, 0);
        // bearing() computes "from specified point to here": south -> here is due north
        assertThat(here.bearing(south)).isCloseTo(Math.PI / 2, within(0.01));
    }

    @Test
    public void bearingToDueEastPointIsZero() {
        final ZonePoint here = new ZonePoint(49.0, 16.01, 0);
        final ZonePoint west = new ZonePoint(49.0, 16.0, 0);
        assertThat(here.bearing(west)).isCloseTo(0, within(0.01));
    }

    @Test
    public void bearingToDueSouthPointIsNegativeNinetyDegreesInRadians() {
        final ZonePoint here = new ZonePoint(49.0, 16.0, 0);
        final ZonePoint north = new ZonePoint(49.01, 16.0, 0);
        assertThat(here.bearing(north)).isCloseTo(-Math.PI / 2, within(0.01));
    }

    @Test
    public void angle2azimuthConvertsMathAngleToCompassBearing() {
        assertThat(ZonePoint.angle2azimuth(Math.PI / 2)).isCloseTo(0.0, within(0.001));    // "north" angle -> 0 deg azimuth
        assertThat(ZonePoint.angle2azimuth(0)).isCloseTo(90.0, within(0.001));              // "east" angle -> 90 deg azimuth
        assertThat(ZonePoint.angle2azimuth(-Math.PI / 2)).isCloseTo(180.0, within(0.001));  // "south" angle -> 180 deg azimuth
        assertThat(ZonePoint.angle2azimuth(Math.PI)).isCloseTo(270.0, within(0.001));       // "west" angle -> 270 deg azimuth
    }

    @Test
    public void azimuth2angleIsTheInverseOfAngle2azimuth() {
        for (double azimuth = 0; azimuth < 360; azimuth += 30) {
            final double angle = ZonePoint.azimuth2angle(azimuth);
            final double roundTripped = ZonePoint.angle2azimuth(angle);
            assertThat(roundTripped).isCloseTo(azimuth, within(0.001));
        }
    }

    @Test
    public void translateNorthIncreasesLatitudeOnly() {
        final ZonePoint start = new ZonePoint(49.0, 16.0, 0);
        final ZonePoint moved = start.translate(0 /* azimuth: north */, 1000);

        assertThat(moved.latitude).isGreaterThan(start.latitude);
        assertThat(moved.longitude).isCloseTo(start.longitude, within(1e-9));
        assertThat(start.distance(moved)).isCloseTo(1000, within(1.0));
    }

    @Test
    public void translateEastIncreasesLongitudeOnly() {
        final ZonePoint start = new ZonePoint(49.0, 16.0, 0);
        final ZonePoint moved = start.translate(90 /* azimuth: east */, 500);

        assertThat(moved.longitude).isGreaterThan(start.longitude);
        assertThat(moved.latitude).isCloseTo(start.latitude, within(1e-9));
        assertThat(start.distance(moved)).isCloseTo(500, within(1.0));
    }

    @Test
    public void translateThenDistanceRoundTripsForAnyAzimuth() {
        final ZonePoint start = new ZonePoint(49.0, 16.0, 0);
        for (double azimuth = 0; azimuth < 360; azimuth += 45) {
            final ZonePoint moved = start.translate(azimuth, 250);
            assertThat(start.distance(moved)).isCloseTo(250, within(0.5));
        }
    }

    @Test
    public void convertDistanceRoundTripsThroughKnownUnits() {
        assertThat(ZonePoint.convertDistanceFrom(1, "kilometers")).isEqualTo(1000.0);
        assertThat(ZonePoint.convertDistanceTo(1000, "kilometers")).isEqualTo(1.0);
        assertThat(ZonePoint.convertDistanceFrom(1, "miles")).isCloseTo(1609.344, within(0.001));
        assertThat(ZonePoint.convertDistanceTo(1609.344, "miles")).isCloseTo(1.0, within(0.0001));
    }

    @Test
    public void convertDistanceIsUnchangedForUnknownOrNullUnit() {
        assertThat(ZonePoint.convertDistanceFrom(42, "furlongs")).isEqualTo(42.0);
        assertThat(ZonePoint.convertDistanceTo(42, null)).isEqualTo(42.0);
    }

    @Test
    public void friendlyDistanceUsesKilometersAboveFifteenHundredMeters() {
        assertThat(ZonePoint.makeFriendlyDistance(2000)).isEqualTo("2.0 km");
    }

    @Test
    public void friendlyDistanceUsesWholeMetersBetweenOneHundredAndFifteenHundred() {
        // (long) dist is widened back to double for Double.toString(), so it keeps a ".0"
        assertThat(ZonePoint.makeFriendlyDistance(500)).isEqualTo("500.0 m");
    }

    @Test
    public void friendlyDistanceUsesFractionalMetersBelowOneHundred() {
        assertThat(ZonePoint.makeFriendlyDistance(50)).isEqualTo("50.0 m");
    }

    @Test
    public void friendlyLatitudeUsesNorthForPositiveAngle() {
        assertThat(ZonePoint.makeFriendlyLatitude(45.5)).startsWith("N");
    }

    @Test
    public void friendlyLatitudeUsesSouthForNegativeAngle() {
        assertThat(ZonePoint.makeFriendlyLatitude(-45.5)).startsWith("S");
    }

    @Test
    public void friendlyLongitudeUsesEastForPositiveAngle() {
        assertThat(ZonePoint.makeFriendlyLongitude(45.5)).startsWith("E");
    }

    @Test
    public void friendlyLongitudeUsesWestForNegativeAngle() {
        assertThat(ZonePoint.makeFriendlyLongitude(-45.5)).startsWith("W");
    }

    @Test
    public void rawgetAndRawsetRoundTripLatitudeLongitudeAltitude() {
        final ZonePoint zp = new ZonePoint();
        zp.rawset("latitude", 12.5);
        zp.rawset("longitude", -34.5);
        zp.rawset("altitude", 100.0);

        assertThat(zp.rawget("latitude")).isEqualTo(12.5);
        assertThat(zp.rawget("longitude")).isEqualTo(-34.5);
        assertThat(zp.rawget("altitude")).isEqualTo(100.0);
        assertThat(zp.rawget("nonsense")).isNull();
    }

    @Test
    public void rawsetIgnoresNullKey() {
        final ZonePoint zp = new ZonePoint(1, 2, 3);
        zp.rawset(null, 42.0);

        assertThat(zp.latitude).isEqualTo(1.0);
        assertThat(zp.longitude).isEqualTo(2.0);
        assertThat(zp.altitude).isEqualTo(3.0);
    }

    @Test
    public void copyCreatesIndependentEqualInstance() {
        final ZonePoint original = new ZonePoint(1.0, 2.0, 3.0);
        final ZonePoint copy = ZonePoint.copy(original);

        assertThat(copy).isNotSameAs(original);
        assertThat(copy.latitude).isEqualTo(original.latitude);
        assertThat(copy.longitude).isEqualTo(original.longitude);
        assertThat(copy.altitude).isEqualTo(original.altitude);

        copy.latitude = 99;
        assertThat(original.latitude).isEqualTo(1.0);
    }

    @Test
    public void copyOfNullIsNull() {
        assertThat(ZonePoint.copy(null)).isNull();
    }

    @Test
    public void syncCopiesLatitudeAndLongitudeButNotAltitude() {
        final ZonePoint target = new ZonePoint(0, 0, 999);
        final ZonePoint source = new ZonePoint(11.0, 22.0, 1.0);

        target.sync(source);

        assertThat(target.latitude).isEqualTo(11.0);
        assertThat(target.longitude).isEqualTo(22.0);
        assertThat(target.altitude).isEqualTo(999.0);
    }

    @Test
    public void serializeAndDeserializeRoundTrip() throws Exception {
        final ZonePoint original = new ZonePoint(48.123456, 17.654321, 321.0);

        final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        original.serialize(new java.io.DataOutputStream(bytes));

        final ZonePoint restored = new ZonePoint();
        restored.deserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));

        assertThat(restored.latitude).isEqualTo(original.latitude);
        assertThat(restored.longitude).isEqualTo(original.longitude);
        assertThat(restored.altitude).isEqualTo(original.altitude);
    }

    @Test
    public void lenIsAlwaysThree() {
        assertThat(new ZonePoint().len()).isEqualTo(3);
    }

    @Test
    public void keysReturnsLatitudeLongitudeAltitude() {
        final java.util.Iterator<Object> keys = new ZonePoint().keys();
        assertThat(keys).toIterable().containsExactly("latitude", "longitude", "altitude");
    }
}
