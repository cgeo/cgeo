package cgeo.geocaching.connector.su;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SuConnectorTest {

    @Test
    public void testCanHandle() {
        final SuConnector connector = SuConnector.getInstance();
        assertThat(connector.canHandle("TR12")).isTrue();
        assertThat(connector.canHandle("VI12")).isTrue();
        assertThat(connector.canHandle("MS32113")).isTrue();
        assertThat(connector.canHandle("MV32113")).isTrue();
        assertThat(connector.canHandle("LT421")).isTrue();
        assertThat(connector.canHandle("LV421")).isTrue();
    }

    @Test
    public void testCanHandleSU() {
        final SuConnector connector = SuConnector.getInstance();
        assertThat(connector.canHandle("SU12")).isTrue();
    }

    @Test
    public void testCanNotHandle() {
        final SuConnector connector = SuConnector.getInstance();
        assertThat(connector.canHandle("GC12")).isFalse();
        assertThat(connector.canHandle("OC412")).isFalse();
    }

}
