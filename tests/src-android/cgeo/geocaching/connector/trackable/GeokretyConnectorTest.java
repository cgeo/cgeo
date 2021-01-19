package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import java.util.List;

import org.xml.sax.InputSource;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * test for {@link GeokretyConnector}
 */
public class GeokretyConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testCanHandleTrackable() {
        assertThat(getConnector().canHandleTrackable("GK82A2")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB1234")).isFalse();
        assertThat(getConnector().canHandleTrackable("UNKNOWN")).isFalse();

        assertThat(getConnector().canHandleTrackable("GKXYZ1")).isFalse(); // non hex
        assertThat(getConnector().canHandleTrackable("GKXYZ1", TrackableBrand.GEOKRETY)).isTrue(); // non hex, but match secret codes pattern
        assertThat(getConnector().canHandleTrackable("123456", TrackableBrand.GEOKRETY)).isTrue();  // Secret code
        assertThat(getConnector().canHandleTrackable("012345", TrackableBrand.GEOKRETY)).isFalse(); // blacklisted 0/O
        assertThat(getConnector().canHandleTrackable("ABCDEF", TrackableBrand.GEOKRETY)).isTrue();  // Secret code
        assertThat(getConnector().canHandleTrackable("LMNOPQ", TrackableBrand.GEOKRETY)).isFalse(); // blacklisted 0/O

        assertThat(getConnector().canHandleTrackable("GC1234")).isFalse();
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.UNKNOWN)).isFalse();
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.TRAVELBUG)).isFalse();
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.GEOKRETY)).isTrue();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(getConnector().getTrackableCodeFromUrl("http://www.geokrety.org/konkret.php?id=46464")).isEqualTo("GKB580");
        assertThat(getConnector().getTrackableCodeFromUrl("https://www.geokrety.org/konkret.php?id=46464")).isEqualTo("GKB580");
        assertThat(getConnector().getTrackableCodeFromUrl("http://geokrety.org/konkret.php?id=46465")).isEqualTo("GKB581");
        assertThat(getConnector().getTrackableCodeFromUrl("https://geokrety.org/konkret.php?id=46465")).isEqualTo("GKB581");

        assertThat(getConnector().getTrackableCodeFromUrl("https://api.geokrety.org/gk/46464")).isEqualTo("GKB580");
        assertThat(getConnector().getTrackableCodeFromUrl("https://api.geokrety.org/gk/46464/details")).isEqualTo("GKB580");
    }

    public static void testGeocode() throws Exception {
        assertThat(GeokretyConnector.geocode(46464)).isEqualTo("GKB580");
    }

    public static void testGetId() throws Exception {
        assertThat(GeokretyConnector.getId("GKB581")).isEqualTo(46465);
    }

    public void testGetUrl() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables).hasSize(2);
        assertThat(trackables.get(0).getUrl()).isEqualTo("https://geokrety.org/konkret.php?id=46464");
        assertThat(trackables.get(1).getUrl()).isEqualTo("https://geokrety.org/konkret.php?id=46465");
    }

    public void testSearchTrackable() throws Exception {
        final Trackable geokret = GeokretyConnector.searchTrackable("GKB580");
        assertThat(geokret).isNotNull();
        assertThat(geokret.getBrand()).isEqualTo(TrackableBrand.GEOKRETY);
        assertThat(geokret.getName()).isEqualTo("c:geo One");
        assertThat(geokret.getDetails()).isEqualTo("GeoKret for the c:geo project :)<br />DO NOT MOVE");
        assertThat(geokret.getOwner()).isEqualTo("kumy");
        assertThat(geokret.isMissing()).isTrue();
        assertThat(geokret.isLoggable()).isTrue();
        assertThat(geokret.getSpottedName()).isEqualTo("OX5BRQK");
        assertThat(geokret.getSpottedType()).isEqualTo(Trackable.SPOTTED_CACHE);
    }

    public void testSearchTrackables() throws Exception {
        // here it is assumed that:
        // * cache OX5BRQK contains these 2 objects only...
        // * objects never been moved
        // * GK website always return list in the same order
        final List<Trackable> trackables = new GeokretyConnector().searchTrackables("OX5BRQK");
        assertThat(trackables).hasSize(2);
        assertThat(trackables).extracting("name").containsOnly("c:geo One", "c:geo Two");
    }

    public void testGetIconBrand() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables).hasSize(2);
        assertThat(trackables).extracting("brand").containsOnly(TrackableBrand.GEOKRETY, TrackableBrand.GEOKRETY);
    }

    private static GeokretyConnector getConnector() {
        return new GeokretyConnector();
    }

    public static void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isTrue();
    }
}
