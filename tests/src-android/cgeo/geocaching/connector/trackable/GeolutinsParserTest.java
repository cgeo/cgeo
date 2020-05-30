package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import java.util.List;

import org.xml.sax.InputSource;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeolutinsParserTest extends AbstractResourceInstrumentationTestCase {

    public void testParse() {
        final List<Trackable> trackables1 = GeolutinsParser.parse(new InputSource(getResourceStream(R.raw.geolutins19_xml)));
        assertThat(trackables1).hasSize(2);

        // Check Geolutins in list
        final Trackable trackable1 = trackables1.get(0);
        assertThat(trackable1.getName()).isEqualTo("c:geo tests");
        assertThat(trackable1.getGeocode()).isEqualTo("GL007B8");
        assertThat(trackable1.getDetails()).isEqualTo("Virtual GeoLutins for testing c:geo android application.");
        assertThat(trackable1.getType()).isNull();
        assertThat(trackable1.getOwner()).isEqualTo("kumy");

        // Check the same xml but pretty printed
        final Trackable trackable2 = trackables1.get(1);
        assertThat(trackable1.getName()).isEqualTo(trackable2.getName());
        assertThat(trackable1.getGeocode()).isEqualTo(trackable2.getGeocode());
        assertThat(trackable1.getDetails()).isEqualTo(trackable2.getDetails());
        assertThat(trackable1.getType()).isEqualTo(trackable2.getType());
        assertThat(trackable1.getOwner()).isEqualTo(trackable2.getOwner());

        // Another GL with logs
        final List<Trackable> trackables2 = GeolutinsParser.parse(new InputSource(getResourceStream(R.raw.geolutins1_xml)));
        assertThat(trackables2).hasSize(1);

        // Check Geolutins in list
        final Trackable trackable3 = trackables2.get(0);
        assertThat(trackable3.getName()).isEqualTo("Géolutin #1");
        assertThat(trackable3.getGeocode()).isEqualTo("GL00001");
        assertThat(trackable3.getDetails()).isEqualTo("Je suis le premier Géolutin ! Mon but est simple : voyager le plus possible afin de faire découvrir mes futurs copains !<br><br>");
        assertThat(trackable3.getType()).isNull();
        assertThat(trackable3.getOwner()).isEqualTo("redregis");
        assertThat(trackable3.getLogs()).hasSize(23);
    }

    public static void testService() {
        /*

        disabled for now,
        as the page throws a http 403
        as of 2020-05-30


        final InputStream page = Network.getResponseStream(Network.getRequest("http://www.geolutins.com/xml/api.php?G=GL007B8"));
        assertThat(page).isNotNull();

        try {
            final List<Trackable> trackables1 = GeolutinsParser.parse(new InputSource(page));
            assertThat(trackables1).hasSize(1);

            // Check Geolutins in list
            final Trackable trackable1 = trackables1.get(0);
            assertThat(trackable1.getName()).isEqualTo("c:geo tests");
            assertThat(trackable1.getGeocode()).isEqualTo("GL007B8");
            assertThat(trackable1.getDetails()).isEqualTo("Virtual GeoLutins for testing c:geo android application.");
            assertThat(trackable1.getOwner()).isEqualTo("kumy");
        } finally {
            IOUtils.closeQuietly(page);
        }
        */
    }
}
