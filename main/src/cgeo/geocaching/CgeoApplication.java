package cgeo.geocaching;

import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;
import menion.android.whereyougo.MainApplication;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.FileSystem;
import menion.android.whereyougo.utils.Utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CgeoApplication extends Application {

    private static CgeoApplication instance;

    public CgeoApplication() {
        setInstance(this);
    }

    //WhereYouGo TODO
    public boolean isScreenOff() {
        return false;
    }

    //WhereYouGo TODO
    public boolean setRoot(String value) {
        //to nothing for now
        return true;
    }

    //WhereYouGo TODO
    public static void onActivityPause() {

    }

    private static void setInstance(@NonNull final CgeoApplication application) {
        instance = application;
    }

    public static Application getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        OOMDumpingUncaughtExceptionHandler.installUncaughtExceptionHandler();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            fixUserManagerMemoryLeak();
        }

        showOverflowMenu();

        initApplicationLocale();

        // ensure initialization of lists
        DataStore.getLists();

        // Restore cookies
        Cookies.restoreCookies();

        //initialize WhereYouGp
        initializeWhereYouGo();
    }

    /**
     * https://code.google.com/p/android/issues/detail?id=173789
     * introduced with JELLY_BEAN_MR2 / fixed in October 2016
     */
    private void fixUserManagerMemoryLeak() {
        try {
            // invoke UserManager.get() via reflection
            final Method m = UserManager.class.getMethod("get", Context.class);
            m.setAccessible(true);
            m.invoke(null, this);
        } catch (final Throwable e) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("Cannot fix UserManager memory leak", e);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        initApplicationLocale();
    }

    private void showOverflowMenu() {
        try {
            final ViewConfiguration config = ViewConfiguration.get(this);
            final Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            menuKeyField.setAccessible(true);
            menuKeyField.setBoolean(config, false);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException ignored) {
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
    }

    @SuppressLint("NewApi")
    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.i("Cleaning applications cache to trim memory");
            DataStore.removeAllFromCache();
        }
    }

    /**
     * Enforce language to be English if the user decided so.
     */
    private void initApplicationLocale() {
        final Configuration config = new Configuration();
        config.locale = Settings.getApplicationLocale();
        final Resources resources = getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void initializeWhereYouGo() {
        //TODO: WhereYouGo for c:geo
        // set basic settings values
        PreferenceManager.setDefaultValues(this, R.xml.whereyougo_preferences, false);
        Preferences.setContext(this);
        Preferences.init(this);

        // register screen on/off receiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //mScreenReceiver = new MainApplication.ScreenReceiver();
        //registerReceiver(mScreenReceiver, filter);

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

}
