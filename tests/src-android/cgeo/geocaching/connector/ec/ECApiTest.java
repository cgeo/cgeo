package cgeo.geocaching.connector.ec;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class ECApiTest extends TestCase {

    public static void testGetIdFromGeocode() throws Exception {
        assertThat(ECApi.getIdFromGeocode("EC242")).isEqualTo("242");
        assertThat(ECApi.getIdFromGeocode("ec242")).isEqualTo("242");
    }

}
