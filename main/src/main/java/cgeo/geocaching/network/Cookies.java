package cgeo.geocaching.network;

import cgeo.geocaching.settings.DiskCookieStore;
import cgeo.geocaching.utils.Log;

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
            final boolean doLogging = Log.isEnabled(Log.LogLevel.DEBUG);
            final StringBuilder cookieLogString = doLogging ? new StringBuilder() : null;
            for (final Cookie cookie : cookies) {
                needStoreUpdate |= addCookie(cookie);
                if (doLogging) {
                    cookieLogString.append(";").append(cookie.name()).append("=").append(prepareCookieValueForLog(cookie.value()));
                }
            }
            if (doLogging) {
                Log.d("HTTP-COOKIES: SAVE for " + url + ": " + cookieLogString);
            }
            if (needStoreUpdate) {
                dumpCookieStore();
            }
        }

        private static String prepareCookieValueForLog(final String value) {
            return StringUtils.isBlank(value) || value.length() < 50 ? value : value.substring(0, 10) + "#" + value.length() + "#" + value.substring(value.length() - 3);

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
            final boolean doLogging = Log.isEnabled(Log.LogLevel.DEBUG);
            final StringBuilder cookieLogString = doLogging ? new StringBuilder() : null;
            synchronized (this) {
                for (final Cookie cookie : allCookies.values()) {
                    if (cookie.matches(url)) {
                        cookies.add(cookie);
                        if (doLogging) {
                            cookieLogString.append(";").append(cookie.name()).append("=").append(prepareCookieValueForLog(cookie.value()));
                        }
                    }
                }
            }
            if (doLogging) {
                Log.d("HTTP-COOKIES: SEND for " + url + ": " + cookieLogString);
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

        private void dumpCookieStore() {
            final StringBuilder persistentCookies = new StringBuilder();
            for (final Cookie cookie : allCookies.values()) {
                if (!cookie.persistent()) {
                    continue;
                }
                persistentCookies.append(cookie.name());
                persistentCookies.append('=');
                persistentCookies.append(cookie.value());
                persistentCookies.append('=');
                persistentCookies.append(cookie.domain());
                persistentCookies.append(';');
            }
            DiskCookieStore.setCookieStore(persistentCookies.toString());
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
