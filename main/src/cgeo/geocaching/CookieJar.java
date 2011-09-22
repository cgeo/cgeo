package cgeo.geocaching;

import org.apache.commons.lang3.StringUtils;

import android.content.SharedPreferences;
import android.util.Log;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handle cookies obtained from web sites.
 *
 * No other place should touch cookies directly, as we want to make sure
 * that the stored information is up-to-date.
 *
 */
final public class CookieJar {

    static private boolean cookiesLoaded = false;
    final static private HashMap<String, String> cookies = new HashMap<String, String>();

    static private String cache = null; // Cache information, or null if it has been invalidated

    static private void loadCookiesIfNeeded(final SharedPreferences prefs) {
        if (!cookiesLoaded) {
            cookies.clear();
            for (final Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().startsWith("cookie_")) {
                    cookies.put(entry.getKey().substring(7), (String) entry.getValue());
                }
            }
            cookiesLoaded = true;
        }
    }

    static public synchronized void setCookie(final SharedPreferences prefs, final String name, final String value) {
        loadCookiesIfNeeded(prefs);
        if (!cookies.containsKey(name) || !cookies.get(name).equals(value)) {
            final SharedPreferences.Editor editor = prefs.edit();
            cookies.put(name, value);
            editor.putString(name, value);
            cache = null;
            editor.commit();
        }
    }

    static public synchronized void setCookie(final SharedPreferences prefs, final String headerField) {
        final int semiIndex = headerField.indexOf(';');
        final String cookie = semiIndex == -1 ? headerField : headerField.substring(0, semiIndex);
        final int equalIndex = headerField.indexOf('=');
        if (equalIndex > 0) {
            setCookie(prefs, cookie.substring(0, equalIndex), cookie.substring(equalIndex + 1));
        } else {
            Log.w(cgSettings.tag, "CookieJar.setCookie: ignoring header " + headerField);
        }
    }

    static public synchronized void setCookies(final SharedPreferences prefs, final URLConnection uc) {
        final Map<String, List<String>> headers = uc.getHeaderFields();
        if (headers == null) {
            // If a request failed, there might not be headers.
            return;
        }
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if ("set-cookie".equalsIgnoreCase(entry.getKey())) {
                for (final String field : entry.getValue()) {
                    setCookie(prefs, field);
                }
            }
        }
    }

    static public synchronized String getCookiesAsString(final SharedPreferences prefs) {
        if (cache == null) {
            loadCookiesIfNeeded(prefs);
            final ArrayList<String> built = new ArrayList<String>();
            for (final Map.Entry<String, String> entry : cookies.entrySet()) {
                built.add(entry.getKey() + "=" + entry.getValue());
            }
            cache = StringUtils.join(built, ';');
        }
        return cache;
    }

    static public synchronized void deleteCookies(final SharedPreferences prefs) {
        loadCookiesIfNeeded(prefs);
        final SharedPreferences.Editor editor = prefs.edit();
        for (final String key : cookies.keySet()) {
            editor.remove("cookie_" + key);
        }
        editor.commit();
        cookies.clear();
        cache = "";
    }

}
