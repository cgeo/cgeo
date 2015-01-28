package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.network.Network;

import junit.framework.TestCase;

public class SwaggieParserTest extends TestCase {

    public static void testParse() {
        final Trackable trackableIn = new Trackable();
        trackableIn.setGeocode("SW0017");

        final String cacheUrl = ConnectorFactory.getTrackableConnector(trackableIn.getGeocode()).getUrl(trackableIn);
        assertThat(cacheUrl).isNotNull();

        final String page = Network.getResponseData(Network.getRequest(cacheUrl));
        assertThat(page).isNotNull();

        assert page != null;
        final Trackable trackable = SwaggieParser.parse(page);
        assertThat(trackable).isNotNull();
        assert trackable != null;

        assertThat(trackable.getName()).isEqualTo("Law and Order 1");
        assertThat(trackable.getGeocode()).isEqualTo("SW0017");
        assertThat(trackable.getDetails()).isEqualTo("A mini CD that may or may not contain a DivX .AVI of the Law and Order: CSI episode that features geocaching as part of the storyline.");
        assertThat(trackable.getType()).isEqualTo("Swaggie");
        assertThat(trackable.getOwner()).isEqualTo("Bear_Left");
    }
}
