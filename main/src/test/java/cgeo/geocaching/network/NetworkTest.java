package cgeo.geocaching.network;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class NetworkTest {

    @Test
    public void testRfc3986URLEncode() {
        assertThat(Network.rfc3986URLEncode("*")).isEqualTo("*");
        assertThat(Network.rfc3986URLEncode("~")).isEqualTo("~");
        assertThat(Network.rfc3986URLEncode(" ")).isEqualTo("%20");
        assertThat(Network.rfc3986URLEncode("foo @+%/")).isEqualTo("foo%20%40%2B%25%2F");
        assertThat(Network.rfc3986URLEncode("sales and marketing/Miami")).isEqualTo("sales%20and%20marketing%2FMiami");
    }

}
