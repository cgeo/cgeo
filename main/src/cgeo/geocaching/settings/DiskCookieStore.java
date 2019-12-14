package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class DiskCookieStore {

    private static final SharedPreferences COOKIES_PREFS = CgeoApplication.getInstance().getSharedPreferences("cookies", Context.MODE_PRIVATE);
    private static final String COOKIES_KEY = "cookies";

    private DiskCookieStore() {
        // Utility class, do not instantiate
    }

    public static void setCookieStore(@Nullable final String cookies) {
        final SharedPreferences.Editor e = COOKIES_PREFS.edit();
        if (StringUtils.isBlank(cookies)) {
            // erase cookies
            e.remove(COOKIES_KEY);
        } else {
            e.putString(COOKIES_KEY, cookies);
        }
        e.apply();
    }

    @Nullable
    public static String getCookieStore() {
        return COOKIES_PREFS.getString(COOKIES_KEY, null);
    }
}
