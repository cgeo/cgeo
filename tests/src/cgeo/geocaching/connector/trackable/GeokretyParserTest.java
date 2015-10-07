package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.xml.sax.InputSource;

import java.util.List;

public class GeokretyParserTest extends AbstractResourceInstrumentationTestCase {

    public void testParse() throws Exception {
        final CgeoApplication app = CgeoApplication.getInstance();

        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables).hasSize(2);
        assert trackables != null;

        // Check first GK in list
        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("c:geo One");
        assertThat(trackable1.getGeocode()).isEqualTo("GKB580");
        assertThat(trackable1.getDistance()).isEqualTo(0);
        assertThat(trackable1.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_traditional));

        // Check second GK in list
        final Trackable trackable2 = trackables.get(1);
        assertThat(trackable2).isNotNull();
        assertThat(trackable2.getName()).isEqualTo("c:geo Two");
        assertThat(trackable2.getGeocode()).isEqualTo("GKB581");
        assertThat(trackable2.getDistance()).isEqualTo(0);
        assertThat(trackable2.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_post));
    }

    public void testParseResponse() throws Exception {
        final ImmutablePair<Integer, List<String>> response1 = GeokretyParser.parseResponse(getFileContent(R.raw.geokret142_xml));
        assertThat(response1).isNotNull();
        assert response1 != null;
        assertThat(response1.getLeft()).isNotNull();
        assertThat(response1.getLeft()).isEqualTo(0);
        assertThat(response1.getRight()).isNotNull();
        assertThat(response1.getRight()).hasSize(2);
        assertThat(response1.getRight().get(0)).isEqualTo("Identical log has been submited.");
        assertThat(response1.getRight().get(1)).isEqualTo("There is an entry with this date. Correct the date or the hour.");

        final ImmutablePair<Integer, List<String>> response2 = GeokretyParser.parseResponse(getFileContent(R.raw.geokret143_xml));
        assertThat(response2).isNotNull();
        assert response2 != null;
        assertThat(response2.getLeft()).isNotNull();
        assertThat(response2.getLeft()).isEqualTo(27334);
        assertThat(response2.getRight()).isNotNull();
        assertThat(response2.getRight()).hasSize(0);

        final ImmutablePair<Integer, List<String>> response3 = GeokretyParser.parseResponse(getFileContent(R.raw.geokret144_xml));
        assertThat(response3).isNotNull();
        assert response3 != null;
        assertThat(response3.getLeft()).isNotNull();
        assertThat(response3.getLeft()).isEqualTo(0);
        assertThat(response3.getRight()).isNotNull();
        assertThat(response3.getRight()).hasSize(2);
        assertThat(response3.getRight().get(0)).isEqualTo("Wrong secid");
        assertThat(response3.getRight().get(1)).isEqualTo("Wrond date or time");
    }

    public static void testGetType() throws Exception {
        final CgeoApplication app = CgeoApplication.getInstance();
        assertEquals(GeokretyParser.getType(0), app.getString(cgeo.geocaching.R.string.geokret_type_traditional));
        assertEquals(GeokretyParser.getType(1), app.getString(cgeo.geocaching.R.string.geokret_type_book_or_media));
        assertEquals(GeokretyParser.getType(2), app.getString(cgeo.geocaching.R.string.geokret_type_human));
        assertEquals(GeokretyParser.getType(3), app.getString(cgeo.geocaching.R.string.geokret_type_coin));
        assertEquals(GeokretyParser.getType(4), app.getString(cgeo.geocaching.R.string.geokret_type_post));
        assertNull(GeokretyParser.getType(5));
        assertNull(GeokretyParser.getType(42));
    }

    public void testParseNoValueFields() throws Exception {
        final CgeoApplication app = CgeoApplication.getInstance();

        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret146_xml)));
        assertThat(trackables).hasSize(1);
        assert trackables != null;

        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("Wojna");
        assertThat(trackable1.getGeocode()).isEqualTo("GKC241");
        assertThat(trackable1.getReleased()).isNull();
        assertThat(trackable1.getDistance()).isEqualTo(-1.0f);
        assertThat(trackable1.getImage()).isNull();
        assertThat(trackable1.getSpottedType()).isEqualTo(Trackable.SPOTTED_OWNER);
        assertThat(trackable1.getSpottedName()).isNull();
        assertThat(trackable1.getType()).isEqualTo(app.getString(cgeo.geocaching.R.string.geokret_type_traditional));
    }

    public void testParseDescription() throws Exception {
        final List<Trackable> trackables = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret145_xml)));
        assertThat(trackables).hasSize(1);
        assert trackables != null;

        // Check first GK in list
        final Trackable trackable1 = trackables.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.getName()).isEqualTo("c:geo Test");
        assertThat(trackable1.getGeocode()).isEqualTo("GKC240");
        assertThat(trackable1.getDistance()).isEqualTo(2254);
        assertThat(trackable1.getDetails()).isEqualTo("Dieser Geokret dient zum Testen von c:geo.<br />" +
                "Er befindet sich nicht wirklich im gelisteten Cache. <br />" +
                "<br />" +
                "Bitte ignorieren.");
    }

    public void testMissing() throws Exception {
        final List<Trackable> trackables1 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret145_xml)));
        assertThat(trackables1).hasSize(1);
        final Trackable trackable1 = trackables1.get(0);
        assertThat(trackable1).isNotNull();
        assertThat(trackable1.isMissing()).isTrue();

        final List<Trackable> trackables2 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret146_xml)));
        assertThat(trackables2).hasSize(1);
        final Trackable trackable2 = trackables2.get(0);
        assertThat(trackable2).isNotNull();
        assertThat(trackable2.isMissing()).isFalse();

        final List<Trackable> trackables3 = GeokretyParser.parse(new InputSource(getResourceStream(R.raw.geokret141_xml)));
        assertThat(trackables3).hasSize(2);
        final Trackable trackable3 = trackables3.get(0);
        assertThat(trackable3).isNotNull();
        assertThat(trackable3.isMissing()).isFalse();
        final Trackable trackable4 = trackables3.get(1);
        assertThat(trackable4).isNotNull();
        assertThat(trackable4.isMissing()).isTrue();
    }
}
