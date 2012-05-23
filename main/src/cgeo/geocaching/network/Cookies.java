package cgeo.geocaching.network;

import ch.boye.httpclientandroidlib.client.CookieStore;
import ch.boye.httpclientandroidlib.cookie.Cookie;
import ch.boye.httpclientandroidlib.impl.client.BasicCookieStore;
import ch.boye.httpclientandroidlib.impl.cookie.BasicClientCookie;
import org.apache.commons.lang3.StringUtils;

public abstract class Cookies {

    private static boolean cookieStoreRestored = false;
    final static CookieStore cookieStore = new BasicCookieStore();

    public static void restoreCookieStore(final String oldCookies) {
        if (!cookieStoreRestored) {
            clearCookies();
            if (oldCookies != null) {
                for (final String cookie : StringUtils.split(oldCookies, ';')) {
                    final String[] split = StringUtils.split(cookie, "=", 3);
                    if (split.length == 3) {
                        final BasicClientCookie newCookie = new BasicClientCookie(split[0], split[1]);
                        newCookie.setDomain(split[2]);
                        cookieStore.addCookie(newCookie);
                    }
                }
            }
            cookieStoreRestored = true;
        }
    }

    public static String dumpCookieStore() {
        StringBuilder cookies = new StringBuilder();
        for (final Cookie cookie : cookieStore.getCookies()) {
            cookies.append(cookie.getName());
            cookies.append('=');
            cookies.append(cookie.getValue());
            cookies.append('=');
            cookies.append(cookie.getDomain());
            cookies.append(';');
        }
        return cookies.toString();
    }

    public static void clearCookies() {
        cookieStore.clear();
    }
}
