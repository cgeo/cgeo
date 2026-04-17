package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class OpenLocationCodePointTest {

    @Test
    public void shortCodeWithLocalityCanBeRecoveredUsingReferenceLocation() {
        final Geopoint reference = new Geopoint(53.2194, 6.5665);
        final String fullCode = OpenLocationCodePoint.latLong2OLC(reference).toString();
        final String shortCodeWithLocality = fullCode.substring(4) + " Groningen";

        final OpenLocationCodePoint olcPoint = new OpenLocationCodePoint(shortCodeWithLocality, reference);

        assertThat(olcPoint.toString()).isEqualTo(fullCode);
        assertThat(reference.distanceTo(olcPoint.toLatLong())).isLessThan(20.0f);
    }

    @Test
    public void shortCodeWithoutReferenceLocationFails() {
        assertThatThrownBy(() -> new OpenLocationCodePoint("6HCG+FQ Groningen"))
                .isInstanceOf(OpenLocationCodePoint.ParseException.class);
    }
}
