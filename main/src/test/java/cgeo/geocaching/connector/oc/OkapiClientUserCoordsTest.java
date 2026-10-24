package cgeo.geocaching.connector.oc;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.JsonUtils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class OkapiClientUserCoordsTest {

    private static ArrayNode wpts(final String json) throws Exception {
        return (ArrayNode) JsonUtils.reader.readTree(json);
    }

    @Test
    public void testUserCoordsFound() throws Exception {
        final ArrayNode json = wpts("[{\"name\":\"OC1234-1\",\"location\":\"50.1|8.1\",\"type\":\"parking\",\"description\":\"\"},"
                + "{\"name\":\"OC1234-USER-COORDS\",\"location\":\"50.123456|8.654321\",\"type\":\"user-coords\",\"description\":\"\"}]");
        assertThat(OkapiClient.parseUserCoords(json)).isEqualTo(new Geopoint(50.123456, 8.654321));
    }

    @Test
    public void testNoUserCoords() throws Exception {
        assertThat(OkapiClient.parseUserCoords(wpts("[{\"name\":\"OC1234-1\",\"location\":\"50.1|8.1\",\"type\":\"parking\",\"description\":\"\"}]"))).isNull();
        assertThat(OkapiClient.parseUserCoords(wpts("[]"))).isNull();
    }

    @Test
    public void testZeroUserCoordsAreIgnored() throws Exception {
        assertThat(OkapiClient.parseUserCoords(wpts("[{\"name\":\"OC1234-USER-COORDS\",\"location\":\"0|0\",\"type\":\"user-coords\",\"description\":\"\"}]"))).isNull();
    }

}
