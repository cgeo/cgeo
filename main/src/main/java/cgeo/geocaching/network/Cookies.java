package cgeo.geocaching.network;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import okhttp3.Cookie;
import okhttp3.Cookie.Builder;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

public final class Cookies {

    public static final InMemoryCookieJar cookieJar = new InMemoryCookieJar();

    public static List<Cookie> extractCookies(@NonNull final String url, final String cookieString, final Predicate<Cookie> filter) {
        if (cookieString == null) {
            return Collections.emptyList();
        }
        final HttpUrl httpUrl = HttpUrl.get(url);
        final List<Cookie> cookies = new ArrayList<>();
        for (String cookie : cookieString.split("; ")) {
            final Cookie c = Cookie.parse(httpUrl, cookie);
            if (c != null && (filter == null || filter.test(c))) {
                cookies.add(c);
            }
        }
        return cookies;
    }

    public static class InMemoryCookieJar implements CookieJar {

        /** Expiry given to legacy persisted entries (written without one) when restoring them. */
        private static final long LEGACY_COOKIE_LIFETIME_MS = 365L * 24 * 60 * 60 * 1000;

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

        /**
         * Restores the persisted cookies. Each restored cookie gets its expiry back (or, for entries
         * written by the previous format that had none, a far-future one), because an OkHttp cookie
         * built without {@code expiresAt} is NOT persistent — such cookies were silently dropped by the
         * next {@link #dumpCookieStore()}, i.e. by the first cookie change after an app start, which
         * lost the gc.com session cookie between app starts.
         */
        private synchronized void restoreCookieStore() {
            final String oldCookies = Settings.getPersistentCookies();
            if (oldCookies != null) {
                for (final String cookie : StringUtils.split(oldCookies, ';')) {
                    try {
                        final Cookie restored = parsePersistedCookie(cookie);
                        if (restored != null) {
                            addCookie(restored);
                        }
                    } catch (final RuntimeException ignored) {
                        // ignore
                    }
                }
            }
        }

        /**
         * Persisted formats: current {@code name=domain=expiresAt=value} (the value comes last because it
         * may itself contain '='); legacy {@code name=value=domain} from versions that did not persist the
         * expiry. Returns null for anything else.
         */
        @Nullable
        static Cookie parsePersistedCookie(final String persisted) {
            final String[] split = StringUtils.split(persisted, "=", 4);
            if (split.length == 4) {
                final long expiresAt;
                try {
                    expiresAt = Long.parseLong(split[2]);
                } catch (final NumberFormatException ignored) {
                    return null;
                }
                return new Builder().name(split[0]).domain(split[1]).expiresAt(expiresAt).value(split[3]).build();
            }
            if (split.length == 3) {
                // legacy entry: keep it persistent, it was persistent when it got written
                return new Builder().name(split[0]).value(split[1]).domain(split[2]).expiresAt(System.currentTimeMillis() + LEGACY_COOKIE_LIFETIME_MS).build();
            }
            return null;
        }

        private void dumpCookieStore() {
            final StringBuilder persistentCookies = new StringBuilder();
            for (final Cookie cookie : allCookies.values()) {
                if (!cookie.persistent()) {
                    continue;
                }
                persistentCookies.append(cookie.name());
                persistentCookies.append('=');
                persistentCookies.append(cookie.domain());
                persistentCookies.append('=');
                persistentCookies.append(cookie.expiresAt());
                persistentCookies.append('=');
                persistentCookies.append(cookie.value());
                persistentCookies.append(';');
            }
            Settings.setPersistentCookies(persistentCookies.toString());
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
