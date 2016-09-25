package cgeo.geocaching.network;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.Log;

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

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

        private synchronized void clear() {
            allCookies.clear();
        }

        private synchronized void restoreCookieStore(final String oldCookies) {
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

        private void syncFromNetworkClientToWebView(final String url) {
            Log.d("cookie jar size " + allCookies.size());
            if (!allCookies.isEmpty()) {

                // TODO: find out whether the sync manager is still needed or not
                CookieSyncManager.createInstance(CgeoApplication.getInstance());
                final CookieManager cookieManager = CookieManager.getInstance();

                final HttpUrl targetUrl = HttpUrl.parse(url);
                //sync all the cookies with the webview by generating cookie string
                for (final Cookie cookie : allCookies.values()) {
                    if (cookie.matches(targetUrl)) {
                        Log.d("syncing cookie " + cookie.name());
                        final String cookieString = cookie.name() + "=" + cookie.value() + "; domain=" + cookie.domain();
                        cookieManager.setCookie(url, cookieString);
                    }
                }
                CookieSyncManager.getInstance().sync();
            }
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

    public static void syncFromNetworkClientToWebView(final String url) {
        cookieJar.syncFromNetworkClientToWebView(url);
    }
}
