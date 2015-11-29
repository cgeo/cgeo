package cgeo.geocaching.network;

import junit.framework.TestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkTest extends TestCase {

    public static void testRfc3986URLEncode() {
        assertThat(Network.rfc3986URLEncode("*")).isEqualTo("*");
        assertThat(Network.rfc3986URLEncode("~")).isEqualTo("~");
        assertThat(Network.rfc3986URLEncode(" ")).isEqualTo("%20");
    }

}
