package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.formulas.Value;
import static cgeo.geocaching.models.CalculatedCoordinateType.DEGREE;
import static cgeo.geocaching.models.CalculatedCoordinateType.DEGREE_MINUTE_SEC;
import static cgeo.geocaching.models.CalculatedCoordinateType.PLAIN;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CalculatedCoordinateTest {

    @Test
    public void basicCalculations() {
        final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B}");
        assertThat(cc.getLatitudePattern()).isEqualTo("N48 12.A45");
        assertThat(cc.getLongitudePattern()).isEqualTo("E13 8.67B");
        assertThat(cc.calculateLatitudeData(x -> Value.of(3)).middle).isEqualTo("N48°12.345'");
        assertThat(cc.calculateLongitudeData(x -> Value.of(3)).middle).isEqualTo("E13°8.673'");
        final Geopoint gp = cc.calculateGeopoint(x -> Value.of(3));
        assertThat(gp.getLatitude()).isEqualTo(48.20575, offset(1e-8));
        assertThat(gp.getLongitude()).isEqualTo(13.14455, offset(1e-8));
    }

    @Test
    public void createFromConfig() {
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B}").getType()).isEqualTo(PLAIN);
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|p}").getType()).isEqualTo(PLAIN);
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|dMs}").getType()).isEqualTo(DEGREE_MINUTE_SEC);
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|degreE}").getType()).isEqualTo(DEGREE);
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|degreE}").toConfig()).isEqualTo("{CC|N48 12.A45|E13 8.67B|DDD}");
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
}
