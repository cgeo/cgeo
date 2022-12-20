package cgeo.geocaching.connector.oc;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class OCCZConnectorTest {

    @Test
    public void testGetGeocodeFromUrl() throws Exception {
        final OCCZConnector connector = new OCCZConnector();
        assertThat(connector.getGeocodeFromUrl("http://opencaching.cz/viewcache.php?cacheid=610")).isEqualTo("OZ0262");
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.de/viewcache.php?cacheid=151223")).isNull();
    }

}
