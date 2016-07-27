package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.UserManager;
import android.support.annotation.NonNull;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.squareup.leakcanary.LeakCanary;

public class CgeoApplication extends Application {

    private static CgeoApplication instance;

    public static void dumpOnOutOfMemory(final boolean enable) {

        if (enable) {

            if (!OOMDumpingUncaughtExceptionHandler.activateHandler()) {
                Log.e("OOM dumping handler not activated (either a problem occurred or it was already active)");
            }
        } else if (!OOMDumpingUncaughtExceptionHandler.resetToDefault()) {
            Log.e("OOM dumping handler not resetted (either a problem occurred or it was not active)");
        }
    }

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
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            fixUserManagerMemoryLeak();
        }

        LeakCanary.install(this);
        showOverflowMenu();

        initApplicationLocale();

        // ensure initialization of lists
        DataStore.getLists();

        final Sensors sensors = Sensors.getInstance();
        sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
        sensors.setupDirectionObservable();

        // Attempt to acquire an initial location before any real activity happens.
        sensors.geoDataObservable(true).subscribeOn(AndroidRxUtils.looperCallbacksScheduler).first().subscribe();
    }

    /**
     * https://code.google.com/p/android/issues/detail?id=173789
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void fixUserManagerMemoryLeak() {
        try {
            final Method m = UserManager.class.getMethod("get", Context.class);
            m.setAccessible(true);
            m.invoke(null, this);

            //above is reflection for below...
            //UserManager.get();
        } catch (final Throwable e) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException(e);
            }
        }
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
        onTrimMemory(Compatibility.TRIM_MEMORY_COMPLETE);
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

}
