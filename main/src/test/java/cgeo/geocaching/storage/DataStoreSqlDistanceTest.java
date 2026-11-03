package cgeo.geocaching.storage;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.formulas.Formula;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class DataStoreSqlDistanceTest {

    private static final Geopoint CENTER = new Geopoint(48.0, 11.0);

    @Test
    public void distanceEast() {
        assertSqlDistanceMatches(new Geopoint(48.0, 12.0));
        assertSqlDistanceMatches(new Geopoint(48.0, 16.5));
    }

    @Test
    public void distanceNorth() {
        assertSqlDistanceMatches(new Geopoint(49.0, 11.0));
    }

    @Test
    public void distanceDiagonal() {
        assertSqlDistanceMatches(new Geopoint(47.5, 9.5));
    }

    private static void assertSqlDistanceMatches(final Geopoint other) {
        final double expected = other.distanceTo(CENTER);
        assertThat(sqlDistance(other, CENTER)).isCloseTo(expected, within(expected * 0.02));
    }

    /** evaluates the generated SQL expression with numeric lat/lon instead of column names */
    private static double sqlDistance(final Geopoint point1, final Geopoint point2) {
        final String sql = DataStore.getSqlDistanceSquare(String.valueOf(point1.getLatitude()), String.valueOf(point1.getLongitude()), point2);
        return Math.sqrt(Formula.compile(sql).evaluate().getAsDouble());
    }
}
