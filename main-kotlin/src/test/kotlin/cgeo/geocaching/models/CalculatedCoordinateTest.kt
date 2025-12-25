// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.formulas.Value
import cgeo.geocaching.models.CalculatedCoordinateType.DEGREE
import cgeo.geocaching.models.CalculatedCoordinateType.DEGREE_MINUTE_SEC
import cgeo.geocaching.models.CalculatedCoordinateType.PLAIN

import org.junit.Test
import org.assertj.core.api.Assertions.offset
import org.assertj.core.api.Java6Assertions.assertThat

class CalculatedCoordinateTest {

    @Test
    public Unit basicCalculations() {
        val cc: CalculatedCoordinate = CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B}")
        assertThat(cc.getLatitudePattern()).isEqualTo("N48 12.A45")
        assertThat(cc.getLongitudePattern()).isEqualTo("E13 8.67B")
        assertThat(cc.calculateLatitudeData(x -> Value.of(3)).middle).isEqualTo("N48°12.345'")
        assertThat(cc.calculateLongitudeData(x -> Value.of(3)).middle).isEqualTo("E13°8.673'")
        val gp: Geopoint = cc.calculateGeopoint(x -> Value.of(3))
        assertThat(gp.getLatitude()).isEqualTo(48.20575, offset(1e-8))
        assertThat(gp.getLongitude()).isEqualTo(13.14455, offset(1e-8))
    }

    @Test
    public Unit createFromConfig() {
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B}").getType()).isEqualTo(PLAIN)
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|p}").getType()).isEqualTo(PLAIN)
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|dMs}").getType()).isEqualTo(DEGREE_MINUTE_SEC)
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|degreE}").getType()).isEqualTo(DEGREE)
        assertThat(CalculatedCoordinate.createFromConfig("{CC|N48 12.A45|E13 8.67B|degreE}").toConfig()).isEqualTo("{CC|N48 12.A45|E13 8.67B|DDD}")
    }

    @Test
    public Unit northSouthEastWest() {
        CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig("{CC|N48.123|W13.456}")
        assertThat(cc.calculateGeopoint(null).getLatitude()).isEqualTo(48.123)
        assertThat(cc.calculateGeopoint(null).getLongitude()).isEqualTo(-13.456)

        cc = CalculatedCoordinate.createFromConfig("{CC|S48.123|E13.456}")
        assertThat(cc.calculateGeopoint(null).getLatitude()).isEqualTo(-48.123)
        assertThat(cc.calculateGeopoint(null).getLongitude()).isEqualTo(13.456)
    }
}
