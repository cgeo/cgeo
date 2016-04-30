package cgeo.geocaching;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.ViewConfiguration;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.eclipse.jdt.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Locale;

import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private boolean isGooglePlayServicesAvailable = false;
    private Locale applicationLocale;
    private ConnectivityManager connectivityManager = null;

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

    public static CgeoApplication getInstance() {
        return instance;
    }

    /**
     * Checks if the device has network connection.
     *
     * @return {@code true} if the device is connected to the network.
     */
    public boolean isNetworkConnected() {
        if (connectivityManager == null) {
            // Concurrent assignment would not hurt as this request is idempotent
            connectivityManager = (ConnectivityManager) getInstance().getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        }
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            final ViewConfiguration config = ViewConfiguration.get(this);
            final Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            menuKeyField.setAccessible(true);
            menuKeyField.setBoolean(config, false);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException ignored) {
        }

        // Set language to English if the user decided so.
        initApplicationLocale(Settings.useEnglish());

        // ensure initialization of lists
        DataStore.getLists();

        // Check if Google Play services is available
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            isGooglePlayServicesAvailable = true;
        }
        final Sensors sensors = Sensors.getInstance();
        sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
        sensors.setupDirectionObservable();

        // Attempt to acquire an initial location before any real activity happens.
        sensors.geoDataObservable(true).subscribeOn(AndroidRxUtils.looperCallbacksScheduler).first().subscribe();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        onTrimMemory(Compatibility.TRIM_MEMORY_COMPLETE);
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.i("Cleaning applications cache to trim memory");
            DataStore.removeAllFromCache();
        }
    }

    public boolean isLiveMapHintShownInThisSession() {
        return liveMapHintShownInThisSession;
    }

    public void setLiveMapHintShownInThisSession() {
        liveMapHintShownInThisSession = true;
    }

    /**
     * Check if cgeo must relog even if already logged in.
     *
     * @return {@code true} if it is necessary to relog
     */
    public boolean mustRelog() {
        final boolean mustLogin = forceRelog;
        forceRelog = false;
        return mustLogin;
    }

    /**
     * Force cgeo to relog when reaching the main activity.
     */
    public void forceRelog() {
        forceRelog = true;
    }

    public boolean isGooglePlayServicesAvailable() {
        return isGooglePlayServicesAvailable;
    }

    /**
     * Set the current application language.
     *
     * @param useEnglish {@code true} if English should be used, {@code false} to use the systems settings
     */
    private void initApplicationLocale(final boolean useEnglish) {
        applicationLocale = useEnglish ? Locale.ENGLISH : Locale.getDefault();
        final Configuration config = new Configuration();
        config.locale = applicationLocale;
        final Resources resources = getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    /**
     * Return the locale that should be used to display information to the user.
     *
     * @return either the system locale or an English one, depending on the settings
     */
    public Locale getApplicationLocale() {
        return applicationLocale;
    }

    /**
     * Kill and restart the application.
     */
    @SuppressFBWarnings("DM_EXIT")
    public void restartApplication() {
        final Intent launchIntent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent intent= PendingIntent.getActivity(this, 0, launchIntent, 0);
        final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, intent);
        System.exit(0);
    }


}
