package cgeo.geocaching.connector.ec;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class ECApiTest {

    @Test
    public void testGetIdFromGeocode() throws Exception {
        assertThat(ECApi.getIdFromGeocode("EC242")).isEqualTo("242");
        assertThat(ECApi.getIdFromGeocode("ec242")).isEqualTo("242");
    }

}
