package cgeo.geocaching.network;

import cgeo.geocaching.settings.DiskCookieStore;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.Cookie.Builder;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

public final class Cookies {

    static final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

    private static class InMemoryCookieJar implements CookieJar {

        final HashMap<String, Cookie> allCookies = new HashMap<>();

        @Override
        public synchronized void saveFromResponse(@NonNull final HttpUrl url, final List<Cookie> cookies) {
            boolean needStoreUpdate = false;
            for (final Cookie cookie : cookies) {
                needStoreUpdate |= addCookie(cookie);
            }
            if (needStoreUpdate) {
                DiskCookieStore.setCookieStore(dumpCookieStore());
            }
        }

        private boolean addCookie(final Cookie cookie) {
            final String key = cookie.domain() + ';' + cookie.name();
            final Cookie oldCookie = allCookies.get(key);
            if (oldCookie == null || !oldCookie.equals(cookie)) {
                allCookies.put(key, cookie);
                return true;
            }
            return false;
        }

        @Override
        @NonNull
        public List<Cookie> loadForRequest(@NonNull final HttpUrl url) {
            final List<Cookie> cookies = new LinkedList<>();
            synchronized (this) {
                for (final Cookie cookie : allCookies.values()) {
                    if (cookie.matches(url)) {
                        cookies.add(cookie);
                    }
                }
            }
            return cookies;
        }

        public synchronized void clear() {
            allCookies.clear();
            dumpCookieStore();
        }

        private synchronized void restoreCookieStore() {
            final String oldCookies = DiskCookieStore.getCookieStore();
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
        }

        private String dumpCookieStore() {
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

    public static void clearCookies() {
        cookieJar.clear();
        cookieJar.dumpCookieStore();
    }

    // To be called once when starting the application
    public static void restoreCookies() {
        cookieJar.restoreCookieStore();
    }
}
