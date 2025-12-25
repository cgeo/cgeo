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

import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.List
import java.util.function.Predicate

import okhttp3.Cookie
import okhttp3.Cookie.Builder
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.apache.commons.lang3.StringUtils

class Cookies {

    public static val cookieJar: InMemoryCookieJar = InMemoryCookieJar()

    public static List<Cookie> extractCookies(final String url, final String cookieString, final Predicate<Cookie> filter) {
        if (cookieString == null) {
            return Collections.emptyList()
        }
        val httpUrl: HttpUrl = HttpUrl.get(url)
        val cookies: List<Cookie> = ArrayList<>()
        for (String cookie : cookieString.split("; ")) {
            val c: Cookie = Cookie.parse(httpUrl, cookie)
            if (c != null && (filter == null || filter.test(c))) {
                cookies.add(c)
            }
        }
        return cookies
    }

    public static class InMemoryCookieJar : CookieJar {

        val allCookies: HashMap<String, Cookie> = HashMap<>()

        override         public synchronized Unit saveFromResponse(final HttpUrl url, final List<Cookie> cookies) {
            Boolean needStoreUpdate = false
            val doLogging: Boolean = Log.isEnabled(Log.LogLevel.DEBUG)
            val cookieLogString: StringBuilder = doLogging ? StringBuilder() : null
            for (final Cookie cookie : cookies) {
                needStoreUpdate |= addCookie(cookie)
                if (doLogging) {
                    cookieLogString.append(";").append(cookie.name()).append("=").append(prepareCookieValueForLog(cookie.value()))
                }
            }
            if (doLogging) {
                Log.d("HTTP-COOKIES: SAVE for " + url + ": " + cookieLogString)
            }
            if (needStoreUpdate) {
                dumpCookieStore()
            }
        }

        private static String prepareCookieValueForLog(final String value) {
            return StringUtils.isBlank(value) || value.length() < 50 ? value : value.substring(0, 10) + "#" + value.length() + "#" + value.substring(value.length() - 3)

        }

        private Boolean addCookie(final Cookie cookie) {
            val key: String = cookie.domain() + ';' + cookie.name()
            val oldCookie: Cookie = allCookies.get(key)
            if (oldCookie == null || !oldCookie == (cookie)) {
                allCookies.put(key, cookie)
                return true
            }
            return false
        }

        override         public List<Cookie> loadForRequest(final HttpUrl url) {
            val cookies: List<Cookie> = LinkedList<>()
            val doLogging: Boolean = Log.isEnabled(Log.LogLevel.DEBUG)
            val cookieLogString: StringBuilder = doLogging ? StringBuilder() : null
            synchronized (this) {
                for (final Cookie cookie : allCookies.values()) {
                    if (cookie.matches(url)) {
                        cookies.add(cookie)
                        if (doLogging) {
                            cookieLogString.append(";").append(cookie.name()).append("=").append(prepareCookieValueForLog(cookie.value()))
                        }
                    }
                }
            }
            if (doLogging) {
                Log.d("HTTP-COOKIES: SEND for " + url + ": " + cookieLogString)
            }
            return cookies
        }

        public synchronized Unit clear() {
            allCookies.clear()
            dumpCookieStore()
        }

        private synchronized Unit restoreCookieStore() {
            val oldCookies: String = Settings.getPersistentCookies()
            if (oldCookies != null) {
                for (final String cookie : StringUtils.split(oldCookies, ';')) {
                    final String[] split = StringUtils.split(cookie, "=", 3)
                    if (split.length == 3) {
                        try {
                            addCookie(Builder().name(split[0]).value(split[1]).domain(split[2]).build())
                        } catch (final RuntimeException ignored) {
                            // ignore
                        }
                    }
                }
            }
        }

        private Unit dumpCookieStore() {
            val persistentCookies: StringBuilder = StringBuilder()
            for (final Cookie cookie : allCookies.values()) {
                if (!cookie.persistent()) {
                    continue
                }
                persistentCookies.append(cookie.name())
                persistentCookies.append('=')
                persistentCookies.append(cookie.value())
                persistentCookies.append('=')
                persistentCookies.append(cookie.domain())
                persistentCookies.append(';')
            }
            Settings.setPersistentCookies(persistentCookies.toString())
        }
    }

    private Cookies() {
        // Utility class
    }

    public static Unit clearCookies() {
        cookieJar.clear()
        cookieJar.dumpCookieStore()
    }

    // To be called once when starting the application
    public static Unit restoreCookies() {
        cookieJar.restoreCookieStore()
    }
}
