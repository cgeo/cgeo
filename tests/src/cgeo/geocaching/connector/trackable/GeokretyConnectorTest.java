package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import org.xml.sax.InputSource;

import java.util.List;

/**
 * test for {@link GeokretyConnector}
 */
public class GeokretyConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testCanHandleTrackable() {
        assertThat(new GeokretyConnector().canHandleTrackable("GK82A2")).isTrue();
        assertThat(new GeokretyConnector().canHandleTrackable("GKXYZ1")).isFalse(); // non hex
        assertThat(new GeokretyConnector().canHandleTrackable("TB1234")).isFalse();
        assertThat(new GeokretyConnector().canHandleTrackable("UNKNOWN")).isFalse();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(new GeokretyConnector().getTrackableCodeFromUrl("http://www.geokrety.org/konkret.php?id=46464")).isEqualTo("GKB580");
        assertThat(new GeokretyConnector().getTrackableCodeFromUrl("http://geokrety.org/konkret.php?id=46465")).isEqualTo("GKB581");
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
        assertThat(trackables.get(0).getUrl()).isEqualTo("http://geokrety.org/konkret.php?id=46464");
        assertThat(trackables.get(1).getUrl()).isEqualTo("http://geokrety.org/konkret.php?id=46465");
    }

    public void testSearchTrackable() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables).hasSize(2);
        final Trackable trackable1 = trackables.get(0);
        final Trackable trackable2 = trackables.get(1);

        assertThat(GeokretyConnector.searchTrackable("GKB580")).isEqualToComparingFieldByField(trackable1);
        assertThat(GeokretyConnector.searchTrackable("GKB581")).isEqualToComparingFieldByField(trackable2);
    }

    public void testSearchTrackables() throws Exception {
        // here it is assumed that:
        // * cache OX5BRQK contains these 2 objects only...
        // * objects never been moved
        // * GK website always return list in the same order
        final List<Trackable> trackables1 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        final List<Trackable> trackables2 = new GeokretyConnector().searchTrackables("OX5BRQK");
        assertThat(trackables1).hasSize(2);
        assertThat(trackables2).hasSize(2);
        assertThat(trackables1.get(0)).isEqualToComparingFieldByField(trackables2.get(0));
        assertThat(trackables1.get(1)).isEqualToComparingFieldByField(trackables2.get(1));
    }

    public void testGetIconBrand() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables).hasSize(2);
        assertThat(trackables.get(0).getIconBrand()).isEqualTo(TrackableBrand.GEOKRETY.getIconResource());
        assertThat(trackables.get(1).getIconBrand()).isEqualTo(TrackableBrand.GEOKRETY.getIconResource());
    }

    public void testGetTrackableLoggingManager() throws Exception {
        // how to test?
    }

    private static GeokretyConnector getConnector() {
        return new GeokretyConnector();
    }

    public static void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isTrue();
    }
}
