package cgeo.geocaching.network;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.Cookie.Builder;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class Cookies {

    static final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

    static class InMemoryCookieJar implements CookieJar {

        final HashMap<String, Cookie> allCookies = new HashMap<>();
        private boolean cookieStoreRestored = false;

        @Override
        public synchronized void saveFromResponse(final HttpUrl url, final List<Cookie> cookies) {
            for (final Cookie cookie : cookies) {
                addCookie(cookie);
            }
        }

        private void addCookie(final Cookie cookie) {
            final String key = cookie.domain() + ';' + cookie.name();
            allCookies.put(key, cookie);
        }

        @Override
        public List<Cookie> loadForRequest(final HttpUrl url) {
            final List<Cookie> cookies = new LinkedList<>();
            synchronized (this) {
                for (final Cookie cookie: allCookies.values()) {
                    if (cookie.matches(url)) {
                        cookies.add(cookie);
                    }
                }
            }
            return cookies;
        }

        public synchronized void clear() {
            allCookies.clear();
        }

        public synchronized void restoreCookieStore(final String oldCookies) {
            if (!cookieStoreRestored) {
                clearCookies();
                if (oldCookies != null) {
                    for (final String cookie : StringUtils.split(oldCookies, ';')) {
                        final String[] split = StringUtils.split(cookie, "=", 3);
                        if (split.length == 3) {
                            try {
                                addCookie(new Builder().name(split[0]).value(split[1]).domain(split[2]).build());
                            } catch (final RuntimeException ignored) {
                                // ignore
                            }
                        }
                    }
                }
                cookieStoreRestored = true;
            }
        }

        String dumpCookieStore() {
            final StringBuilder cookies = new StringBuilder();
            for (final Cookie cookie : allCookies.values()) {
                cookies.append(cookie.name());
                cookies.append('=');
                cookies.append(cookie.value());
                cookies.append('=');
                cookies.append(cookie.domain());
                cookies.append(';');
            }
            return cookies.toString();
        }
    }

    private Cookies() {
        // Utility class
    }

    public static void restoreCookieStore(final String oldCookies) {
        cookieJar.restoreCookieStore(oldCookies);
    }

    public static String dumpCookieStore() {
        return cookieJar.dumpCookieStore();
    }

    public static void clearCookies() {
        cookieJar.clear();
    }
}
