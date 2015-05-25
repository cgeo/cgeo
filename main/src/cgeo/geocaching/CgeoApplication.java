package cgeo.geocaching;

import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;
import cgeo.geocaching.utils.RxUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.eclipse.jdt.annotation.NonNull;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;
import java.util.Locale;

public class CgeoApplication extends Application {

    private boolean forceRelog = false; // c:geo needs to log into cache providers
    public boolean showLoginToast = true; //login toast shown just once.
    private boolean liveMapHintShownInThisSession = false; // livemap hint has been shown
    private static CgeoApplication instance;
    private boolean isGooglePlayServicesAvailable = false;
    private Locale applicationLocale;


    public static void dumpOnOutOfMemory(final boolean enable) {

        if (enable) {

            if (!OOMDumpingUncaughtExceptionHandler.activateHandler()) {
                Log.e("OOM dumping handler not activated (either a problem occured or it was already active)");
            }
        } else {
            if (!OOMDumpingUncaughtExceptionHandler.resetToDefault()) {
                Log.e("OOM dumping handler not resetted (either a problem occured or it was not active)");
            }
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

    @Override
    public void onCreate() {
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
        Log.i("Google Play services are " + (isGooglePlayServicesAvailable ? "" : "not ") + "available");
        final Sensors sensors = Sensors.getInstance();
        sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
        sensors.setupDirectionObservable(Settings.useLowPowerMode());

        // Attempt to acquire an initial location before any real activity happens.
        sensors.geoDataObservable(true).subscribeOn(RxUtils.looperCallbacksScheduler).first().subscribe();
    }


    @Override
    public void onLowMemory() {
        onTrimMemory(TRIM_MEMORY_COMPLETE);
    }

    @Override
    public void onTrimMemory(final int level) {
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
     * @return <code>true</code> if it is necessary to relog
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
    public void restartApplication() {
        final Intent launchIntent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent intent= PendingIntent.getActivity(this, 0, launchIntent, 0);
        final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, intent);
        System.exit(0);
    }


}
