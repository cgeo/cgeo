package cgeo.geocaching.network;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CookiesTest {

    private static final HttpUrl GC_WWW = HttpUrl.get("https://www.geocaching.com/account/signin");

    @Test
    public void currentFormatRestoresExpiryAndPersistence() {
        final long expiresAt = System.currentTimeMillis() + 3600_000L;
        final Cookie c = Cookies.InMemoryCookieJar.parsePersistedCookie("gspkauth=geocaching.com=" + expiresAt + "=abc=def==ghi");
        assertThat(c).isNotNull();
        assertThat(c.name()).isEqualTo("gspkauth");
        assertThat(c.domain()).isEqualTo("geocaching.com");
        assertThat(c.value()).isEqualTo("abc=def==ghi");   // '=' inside the value survives
        assertThat(c.expiresAt()).isEqualTo(expiresAt);
        assertThat(c.persistent()).isTrue();               // survives the next dumpCookieStore()
        assertThat(c.hostOnly()).isFalse();
        assertThat(c.matches(GC_WWW)).isTrue();           // sent to www.geocaching.com
    }

    @Test
    public void legacyFormatBecomesPersistent() {
        final Cookie c = Cookies.InMemoryCookieJar.parsePersistedCookie("gspkauth=abc123=geocaching.com");
        assertThat(c).isNotNull();
        assertThat(c.value()).isEqualTo("abc123");
        assertThat(c.domain()).isEqualTo("geocaching.com");
        assertThat(c.persistent()).isTrue();
        assertThat(c.expiresAt()).isGreaterThan(System.currentTimeMillis());
        assertThat(c.matches(GC_WWW)).isTrue();
    }

    @Test
    public void garbageIsIgnored() {
        assertThat(Cookies.InMemoryCookieJar.parsePersistedCookie("gspkauth")).isNull();
        assertThat(Cookies.InMemoryCookieJar.parsePersistedCookie("a=geocaching.com=notanumber=value")).isNull();
    }

    @Test
    public void cookieBuiltWithoutExpiryIsNotPersistent() {
        // the property the old restore path relied on without knowing it
        final Cookie c = new Cookie.Builder().name("gspkauth").value("abc").domain("geocaching.com").build();
        assertThat(c.persistent()).isFalse();
    }
}
