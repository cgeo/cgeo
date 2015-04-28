package cgeo.geocaching.connector;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.mock.GC1ZXX2;

import java.util.Collection;

public class ConnectorFactoryTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetConnectors() {
        final Collection<IConnector> connectors = ConnectorFactory.getConnectors();
        assertThat(connectors).isNotNull();
        assertThat(connectors.isEmpty()).isFalse(); // unknown connector must exist
    }

    public static void testCanHandle() {
        assertThat(ConnectorFactory.canHandle("")).isFalse();
        assertThat(ConnectorFactory.canHandle("GC12345")).isTrue();
        assertThat(ConnectorFactory.canHandle("some string")).isTrue(); // using unknown connector
        assertThat(ConnectorFactory.canHandle("[/start with special char")).isFalse();
    }

    public static void testGeocodeOpenCaching() {
        assertThat(ConnectorFactory.getConnector("OZ12345")).isInstanceOf(OCConnector.class); // opencaching CZ
        assertThat(ConnectorFactory.getConnector("OC12345")).isInstanceOf(OCConnector.class); // opencaching DE
        assertThat(ConnectorFactory.getConnector("OU12345")).isInstanceOf(OCConnector.class); // opencaching US
        assertThat(ConnectorFactory.getConnector("OK12345")).isInstanceOf(OCConnector.class); // opencaching UK
        assertThat(ConnectorFactory.getConnector("OJ12345")).isInstanceOf(OCConnector.class); // opencaching JP
        assertThat(ConnectorFactory.getConnector("OS12345")).isInstanceOf(OCConnector.class); // opencaching NO
        assertThat(ConnectorFactory.getConnector("OB12345")).isInstanceOf(OCConnector.class); // opencaching NL
        assertThat(ConnectorFactory.getConnector("OP12345")).isInstanceOf(OCConnector.class); // opencaching PL
    }

    public static void testGeocodeInvalidFormat() {
        // all codes are invalid
        assertThat(ConnectorFactory.getConnector("GC")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("OC")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("OX")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("GC 1234")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("OC 1234")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("OX 1234")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("GC-1234")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("OC-1234")).isInstanceOf(UnknownConnector.class);
        assertThat(ConnectorFactory.getConnector("OX-1234")).isInstanceOf(UnknownConnector.class);
    }

    public static void testGetConnectorCgCache() {
        assertThat(ConnectorFactory.getConnector(new GC1ZXX2())).isEqualTo(GCConnector.getInstance());
    }

    public static void testGetConnectorString() {
        final IConnector connector = ConnectorFactory.getConnector("GC12345");
        assertThat(connector).isNotNull();
        assertThat(connector.getName()).isEqualTo(GCConnector.getInstance().getName());
    }

    public static void testTrim() {
        assertThat(ConnectorFactory.getConnector("   OZ12345   ")).isInstanceOf(OCConnector.class); // opencaching CZ
        assertThat(ConnectorFactory.getConnector("   OZ 12345   ")).isInstanceOf(UnknownConnector.class);
    }

    public static void testGetGeocodeFromUrl() {
        assertThat(ConnectorFactory.getGeocodeFromURL("http://coord.info/GC34PLO")).isEqualTo("GC34PLO");
        assertThat(ConnectorFactory.getGeocodeFromURL("http://www.coord.info/GC34PLO")).isEqualTo("GC34PLO");
        assertThat(ConnectorFactory.getGeocodeFromURL("http://www.opencaching.com/#!geocache/OX1234")).isEqualTo("OX1234");

        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");

        // trackable URLs
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/TB1234")).isNull();

        // make sure that a mixture of different connector and geocode is recognized as invalid
        assertThat(ConnectorFactory.getGeocodeFromURL("http://www.opencaching.com/#!geocache/" + "GC12345")).isNull();

        // lowercase URL
        assertThat(ConnectorFactory.getGeocodeFromURL("http://coord.info/gc77")).isEqualTo("GC77");
    }

    public static void testGetTrackableFromURL() throws Exception {
        assertThat(ConnectorFactory.getTrackableFromURL("http://www.geokrety.org/konkret.php?id=30970")).isEqualTo("GK78FA");
        assertThat(ConnectorFactory.getTrackableFromURL("http://geokrety.org/konkret.php?id=30970")).isEqualTo("GK78FA");
        assertThat(ConnectorFactory.getTrackableFromURL("http://coord.info/TB1234")).isEqualTo("TB1234");
        assertThat(ConnectorFactory.getTrackableFromURL("http://www.coord.info/TB1234")).isEqualTo("TB1234");
        assertThat(ConnectorFactory.getTrackableFromURL("http://geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234");
        assertThat(ConnectorFactory.getTrackableFromURL("http://www.geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234");

        // cache URLs
        assertThat(ConnectorFactory.getTrackableFromURL("http://coord.info/GC1234")).isNull();
        assertThat(ConnectorFactory.getTrackableFromURL("http://www.coord.info/GC1234")).isNull();
    }
}
