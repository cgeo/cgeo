package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.formulas.Value;

import android.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class CalculatedCoordinateTest {

    @Test
    public void basicCalculations() {
        final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B}");
        assertThat(cc.getLatitudePattern()).isEqualTo("N48 12.A45");
        assertThat(cc.getLongitudePattern()).isEqualTo("E13 8.67B");
        assertThat(cc.getLatitudeString(x -> Value.of(3))).isEqualTo("N48°12.345'");
        assertThat(cc.getLongitudeString(x -> Value.of(3))).isEqualTo("E13°8.673'");
        final Geopoint gp = cc.calculateGeopoint(x -> Value.of(3));
        assertThat(gp.getLatitude()).isEqualTo(48.20575, offset(1e-8));
        assertThat(gp.getLongitude()).isEqualTo(13.14455, offset(1e-8));
    }

    @Test
    public void northSouthEastWest() {
        CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig("{CC|N48.123|W13.456}");
        assertThat(cc.calculateGeopoint(null).getLatitude()).isEqualTo(48.123);
        assertThat(cc.calculateGeopoint(null).getLongitude()).isEqualTo(-13.456);

        cc = CalculatedCoordinate.createFromConfig("{CC|S48.123|E13.456}");
        assertThat(cc.calculateGeopoint(null).getLatitude()).isEqualTo(-48.123);
        assertThat(cc.calculateGeopoint(null).getLongitude()).isEqualTo(13.456);
    }

    @Test
    public void calculateGeopoints() {
        final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig("{CC|N48 1.A2C|W13 1.B56}");
        final Map<String, String> varPatterns = new HashMap<>();
        varPatterns.put("A", "0");
        varPatterns.put("B", "2");
        List<Pair<String, Geopoint>> gps = cc.calculateGeopoints(s -> Value.of(1), varPatterns);
        assertThat(gps.get(0).first).isEqualTo("A=0,B=2");
        assertThat(gps.get(0).second).isEqualTo(new Geopoint("N48 1.021 W13 1.256"));

        varPatterns.put("A", "0-2");
        varPatterns.put("B", "2-4,1,^3");
        gps = cc.calculateGeopoints(s -> Value.of(1), varPatterns);
        assertContainsExactlyInAnyOrder(gps,
            new Pair<>("A=0,B=2", new Geopoint("N48 1.021 W13 1.256")),
            new Pair<>("A=1,B=2", new Geopoint("N48 1.121 W13 1.256")),
            new Pair<>("A=2,B=2", new Geopoint("N48 1.221 W13 1.256")),
            new Pair<>("A=0,B=1", new Geopoint("N48 1.021 W13 1.156")),
            new Pair<>("A=1,B=1", new Geopoint("N48 1.121 W13 1.156")),
            new Pair<>("A=2,B=1", new Geopoint("N48 1.221 W13 1.156")),
            new Pair<>("A=0,B=4", new Geopoint("N48 1.021 W13 1.456")),
            new Pair<>("A=1,B=4", new Geopoint("N48 1.121 W13 1.456")),
            new Pair<>("A=2,B=4", new Geopoint("N48 1.221 W13 1.456"))
        );
    }

    private void assertContainsExactlyInAnyOrder(final List<Pair<String, Geopoint>> gps, final Pair<String, Geopoint> ... expected) {
        assertThat(gps.size()).isEqualTo(expected.length);
        for (Pair<String, Geopoint> exp : expected) {
            boolean found = false;
            for (Pair<String, Geopoint> candidate : gps) {
                if (candidate.first.equals(exp.first) && candidate.second.equals(exp.second)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Did not find " + exp);
            }
        }
    }
}
