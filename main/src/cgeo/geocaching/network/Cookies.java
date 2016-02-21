package cgeo.geocaching.network;

import ch.boye.httpclientandroidlib.client.CookieStore;
import ch.boye.httpclientandroidlib.cookie.Cookie;
import ch.boye.httpclientandroidlib.impl.cookie.BasicClientCookie;
import okhttp3.Cookie.Builder;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public final class Cookies {

    final static InMemoryCookieJar cookieJar = new InMemoryCookieJar();
    final static CookieStore cookieStore = new CompatibilityCookieStore(cookieJar);

    static class CompatibilityCookieStore implements CookieStore {

        private final InMemoryCookieJar cookieJar;

        CompatibilityCookieStore(final InMemoryCookieJar cookieJar) {
            this.cookieJar = cookieJar;
        }

        @Override
        public void addCookie(final Cookie cookie) {
            final String domain = cookie.getDomain();
            final okhttp3.Cookie newCookie = new Builder()
                    .domain(domain.startsWith(".") ? domain.substring(1) : domain)
                    .name(cookie.getName())
                    .value(cookie.getValue())
                    .build();
            cookieJar.addCookie(newCookie);
        }

        @Override
        public List<Cookie> getCookies() {
            final List<okhttp3.Cookie> allCookies;
            synchronized (cookieJar) {
                allCookies = new ArrayList<>(cookieJar.allCookies.values());
            }
            final List<Cookie> cookies = new ArrayList<>(allCookies.size());
            for (final okhttp3.Cookie cookie: allCookies) {
                final BasicClientCookie newCookie = new BasicClientCookie(cookie.name(), cookie.value());
                newCookie.setDomain('.' + cookie.domain());
                cookies.add(newCookie);
            }
            return cookies;
        }

        @Override
        public boolean clearExpired(final Date date) {
            return false;
        }

        @Override
        public void clear() {
            cookieJar.clear();
        }
    }

    static class InMemoryCookieJar implements CookieJar {

        final HashMap<String, okhttp3.Cookie> allCookies = new HashMap<>();
        private boolean cookieStoreRestored = false;

        @Override
        public synchronized void saveFromResponse(final HttpUrl url, final List<okhttp3.Cookie> cookies) {
            for (final okhttp3.Cookie cookie : cookies) {
                addCookie(cookie);
            }
        }

        private void addCookie(final okhttp3.Cookie cookie) {
            final String key = cookie.domain() + ';' + cookie.name();
            allCookies.put(key, cookie);
        }

        @Override
        public List<okhttp3.Cookie> loadForRequest(final HttpUrl url) {
            final List<okhttp3.Cookie> cookies = new LinkedList<>();
            synchronized(this) {
                for (final okhttp3.Cookie cookie: allCookies.values()) {
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
                            } catch (final Exception ignored) {
                           }
                        }
                    }
                }
                cookieStoreRestored = true;
            }
        }

        String dumpCookieStore() {
            final StringBuilder cookies = new StringBuilder();
            for (final okhttp3.Cookie cookie : allCookies.values()) {
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
