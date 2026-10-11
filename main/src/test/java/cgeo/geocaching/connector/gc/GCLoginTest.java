package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GCLoginTest {

    private static final String GEOCACHE_URL = "https://www.geocaching.com/geocache/GC12345";
    private static final String LEGACY_CACHE_URL = "https://www.geocaching.com/seek/cache_details.aspx?wp=GC12345";

    @Test
    public void effectiveRemoveWhitespaceHonorsExplicitFalse() {
        assertThat(GCLogin.getEffectiveRemoveWhitespace(GEOCACHE_URL, false)).isFalse();
    }

    @Test
    public void effectiveRemoveWhitespaceHonorsExplicitTrue() {
        assertThat(GCLogin.getEffectiveRemoveWhitespace(LEGACY_CACHE_URL, true)).isTrue();
    }

    @Test
    public void effectiveRemoveWhitespaceUsesUriDefault() {
        assertThat(GCLogin.getEffectiveRemoveWhitespace(GEOCACHE_URL, null)).isTrue();
        assertThat(GCLogin.getEffectiveRemoveWhitespace(LEGACY_CACHE_URL, null)).isFalse();
    }
}
