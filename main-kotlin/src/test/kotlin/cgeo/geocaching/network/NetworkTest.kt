// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.network

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class NetworkTest {

    @Test
    public Unit testRfc3986URLEncode() {
        assertThat(Network.rfc3986URLEncode("*")).isEqualTo("*")
        assertThat(Network.rfc3986URLEncode("~")).isEqualTo("~")
        assertThat(Network.rfc3986URLEncode(" ")).isEqualTo("%20")
        assertThat(Network.rfc3986URLEncode("foo @+%/")).isEqualTo("foo%20%40%2B%25%2F")
        assertThat(Network.rfc3986URLEncode("sales and marketing/Miami")).isEqualTo("sales%20and%20marketing%2FMiami")
    }

}
