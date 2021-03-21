/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cz.matejcik.openwig.Engine;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.gui.SaveGame;
import menion.android.whereyougo.gui.activity.WhereYouGoActivity;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.ExceptionHandler;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;
import menion.android.whereyougo.utils.StringToken;
import menion.android.whereyougo.utils.Utils;

public class MainApplication extends Application {

    private static final String TAG = "MainApplication";

    private static Timer mTimer;
    private static Context applicationContext;
    private Locale locale = null;
    // screen ON/OFF receiver
    private ScreenReceiver mScreenReceiver;
    private boolean mScreenOff = false;

    public static Context getContext() {
        return applicationContext;
    }

    public static void onActivityPause() {
        // Logger.i(TAG, "onActivityPause()");
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                LocationState.onActivityPauseInstant(PreferenceValues.getCurrentActivity());
                mTimer = null;
            }
        }, 2000);
    }

    public void destroy() {
        try {
            unregisterReceiver(mScreenReceiver);
        } catch (Exception e) {
            Logger.w(TAG, "destroy(), e:" + e);
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public boolean setRoot(String pathCustom) {
        String pathExternal = null;
        try {
            pathExternal = getExternalFilesDir(null).getAbsolutePath();
        } catch (Exception e1) {
        }
        String pathInternal = null;
        try {
            pathInternal = getFilesDir().getAbsolutePath();
        } catch (Exception e2) {
        }

        final boolean set = FileSystem.setRootDirectory(pathCustom)
                || FileSystem.setRootDirectory(pathExternal)
                || FileSystem.setRootDirectory(pathInternal);

        Preferences.GLOBAL_ROOT = FileSystem.ROOT;
        Preferences.setStringPreference(R.string.pref_KEY_S_ROOT, Preferences.GLOBAL_ROOT);
        if (!set) {
            ManagerNotify.toastShortMessage(this, getString(R.string.filesystem_cannot_create_root));
        }
        return set;
    }

    private void initCore() {
        // register screen on/off receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver, filter);

        setRoot(Preferences.GLOBAL_ROOT);

        try {
            FileSystem.CACHE = getExternalCacheDir().getAbsolutePath();
        } catch (Exception e1) {
            try {
                FileSystem.CACHE = getCacheDir().getAbsolutePath();
            } catch (Exception e2) {
                FileSystem.CACHE = FileSystem.ROOT + "cache/";
            }
        }
        if (!FileSystem.CACHE.endsWith("/"))
            FileSystem.CACHE += "/";
        FileSystem.CACHE_AUDIO = FileSystem.CACHE + "audio/";

        // set location state
        LocationState.init(this);
        // initialize DPI
        Utils.getDpPixels(this, 1.0f);

        // set DeviceID for OpenWig
        try {
            String name = String.format("%s, app:%s", A.getAppName(), A.getAppVersion());
            String platform = String.format("Android %s", android.os.Build.VERSION.RELEASE);
            cz.matejcik.openwig.WherigoLib.env.put(cz.matejcik.openwig.WherigoLib.DEVICE_ID, name);
            cz.matejcik.openwig.WherigoLib.env.put(cz.matejcik.openwig.WherigoLib.PLATFORM, platform);
        } catch (Exception e) {
            // not really important
        }
    }

    public boolean isScreenOff() {
        return mScreenOff;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (locale != null) {
            newConfig.locale = locale;
            Locale.setDefault(locale);
            getBaseContext().getResources().updateConfiguration(newConfig,
                    getBaseContext().getResources().getDisplayMetrics());
        }
    }

    /* LEGACY SUPPORT - less v0.8.14
     * Converts preference - comes from a former version (less 0.8.14)
     * which are not stored as string into string.
     */
    private void legacySupport4PreferencesFloat(int prefId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(prefId);

        try {
            sharedPref.getString(key, "");
        } catch (Exception e) {
            try {
                Log.d(TAG, "legacySupport4PreferencesFloat() - LEGACY SUPPORT: convert float to string");
                Float value = sharedPref.getFloat(key, 0.0f);
                sharedPref.edit().remove(key).commit();
                sharedPref.edit().putString(key, String.valueOf(value)).commit();
            } catch (Exception ee) {
                Log.e(TAG, "legacySupport4PreferencesFloat() - panic remove", ee);
                sharedPref.edit().remove(key).commit();
            }
        }
    }

    private void legacySupport4PreferencesInt(int prefId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(prefId);

        try {
            sharedPref.getString(key, "");
        } catch (Exception e) {
            try {
                Log.d(TAG, "legacySupport4PreferencesInt() - LEGACY SUPPORT: convert int to string");
                int value = sharedPref.getInt(key, 0);
                sharedPref.edit().remove(key).commit();
                sharedPref.edit().putString(key, String.valueOf(value)).commit();
            } catch (Exception ee) {
                Log.e(TAG, "legacySupportFloat2Int() - panic remove", ee);
                sharedPref.edit().remove(key).commit();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        Log.d(TAG, "onCreate()");
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

    /* LEGACY SUPPORT - less v0.8.14
     * Converts preference - comes from a former version (less 0.8.14)
     * which are not stored as string into string.
     */
        try {
            // legacySupport4PreferencesFloat( R.string.pref_KEY_S_GPS_ALTITUDE_MANUAL_CORRECTION );
            legacySupport4PreferencesFloat(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_LATITUDE);
            legacySupport4PreferencesFloat(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_LONGITUDE);
            legacySupport4PreferencesFloat(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_ALTITUDE);
            legacySupport4PreferencesInt(R.string.pref_KEY_S_APPLICATION_VERSION_LAST);
        } catch (Exception e) {
            Log.e(TAG, "onCreate() - PANIC! Wipe out preferences", e);
            PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();
        }
    /* LEGECY SUPPORT -- END */

        // set basic settings values
        PreferenceManager.setDefaultValues(this, R.xml.whereyougo_preferences, false);
        Preferences.setContext(this);
        Preferences.init(this);

        // get language
        Configuration config = getBaseContext().getResources().getConfiguration();
        String lang = Preferences.getStringPreference(R.string.pref_KEY_S_LANGUAGE);

        // set language
        if (!lang.equals(getString(R.string.pref_language_default_value))
                && !config.locale.getLanguage().equals(lang)) {
            ArrayList<String> loc = StringToken.parse(lang, "_");
            if (loc.size() == 1) {
                locale = new Locale(lang);
            } else {
                locale = new Locale(loc.get(0), loc.get(1));
            }
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }

        // initialize core
        initCore();
    }

    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory()");
    }
  /* LEGACY SUPPORT -- END */

    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "onTerminate()");
    }

    @Override
    public void onTrimMemory(int level) {
        // TODO Auto-generated method stub
        super.onTrimMemory(level);
        Logger.i(TAG, String.format("onTrimMemory(%d)", level));
        try {
            if (Preferences.GLOBAL_SAVEGAME_AUTO
                    && level == android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
                    && WhereYouGoActivity.selectedFile != null && Engine.instance != null) {
                final Activity activity = PreferenceValues.getCurrentActivity();
                if (activity != null) {
                    if (WhereYouGoActivity.wui != null) {
                        WhereYouGoActivity.wui.setOnSavingFinished(() -> {
                            ManagerNotify.toastShortMessage(activity, getString(R.string.save_game_auto));
                            WhereYouGoActivity.wui.setOnSavingFinished(null);
                        });
                    }
                    new SaveGame(activity).execute();
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, String.format("onTrimMemory(%d): savegame failed", level));
        }
    }

    private class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // Logger.v(TAG, "ACTION_SCREEN_OFF");
                mScreenOff = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // Logger.v(TAG, "ACTION_SCREEN_ON");
                LocationState.onScreenOn(false);
                mScreenOff = false;
            }
        }
    }
}
