package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;

import org.eclipse.jdt.annotation.NonNull;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

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
