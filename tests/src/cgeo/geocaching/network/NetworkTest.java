package cgeo.geocaching.network;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class NetworkTest extends TestCase {

    public static void testRfc3986URLEncode() {
        assertThat(Network.rfc3986URLEncode("*")).isEqualTo("*");
        assertThat(Network.rfc3986URLEncode("~")).isEqualTo("~");
        assertThat(Network.rfc3986URLEncode(" ")).isEqualTo("%20");
    }

}
