package cgeo.geocaching;

import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Looper;
import android.os.UserManager;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CgeoApplication extends Application {

    private static CgeoApplication instance;
    private static final AtomicInteger lowPrioNotificationCounter = new AtomicInteger(0);
    private static final AtomicBoolean hasHighPrioNotification = new AtomicBoolean(false);

    public CgeoApplication() {
        setInstance(this);
    }

    private static void setInstance(@NonNull final CgeoApplication application) {
        instance = application;
    }

    public static Application getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        Log.iForce("---------------- CGeoApplication: startup -------------");
        try (ContextLogger ignore = new ContextLogger(true, "CGeoApplication.onCreate")) {
            super.onCreate();

            OOMDumpingUncaughtExceptionHandler.installUncaughtExceptionHandler();

            Settings.setAppThemeAutomatically(this);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                fixUserManagerMemoryLeak();
            }

            initApplicationLocale();

            // initialize cgeo notification channels
            NotificationChannels.createNotificationChannels(this);

            // ensure initialization of lists
            DataStore.getLists();

            // Restore cookies
            Cookies.restoreCookies();

            LooperLogger.startLogging(Looper.getMainLooper());
        }
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
     * Enforce a specific language if the user decided so.
     */
    private void initApplicationLocale() {
        final Configuration config = getResources().getConfiguration();
        config.locale = Settings.getApplicationLocale();
        final Resources resources = getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public AtomicInteger getLowPrioNotificationCounter() {
        return lowPrioNotificationCounter;
    }

    public AtomicBoolean getHasHighPrioNotification() {
        return hasHighPrioNotification;
    }

}
