package cgeo.geocaching;

import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MessageCenterUtils;
import cgeo.geocaching.utils.OOMDumpingUncaughtExceptionHandler;
import cgeo.geocaching.utils.TransactionSizeLogger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.oscim.backend.CanvasAdapter;

public class CgeoApplication extends Application {

    private static CgeoApplication instance;
    private final AtomicInteger lowPrioNotificationCounter = new AtomicInteger(0);
    private final AtomicBoolean hasHighPrioNotification = new AtomicBoolean(false);

    private final LifecycleInfo lifecycleInfo;

    private static class LifecycleInfo implements ActivityLifecycleCallbacks {

        private Activity currentForegroundActivity = null;
        private final List<Runnable> lifecycleListeners = new ArrayList<>();

        @Override
        public void onActivityCreated(@NonNull final Activity activity, @Nullable final Bundle savedInstanceState) {
            //do nothing
        }

        @Override
        public void onActivityStarted(@NonNull final Activity activity) {
            //do nothing
        }

        @Override
        public void onActivityResumed(@NonNull final Activity activity) {
            if (currentForegroundActivity != activity) {
                currentForegroundActivity = activity;
                callListeners();
            }
        }

        @Override
        public void onActivityPaused(@NonNull final Activity activity) {
            if (activity == currentForegroundActivity) {
                currentForegroundActivity = null;
                callListeners();
            }
        }

        @Override
        public void onActivityStopped(@NonNull final Activity activity) {
            //do nothing
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle outState) {
            //do nothing
        }

        @Override
        public void onActivityDestroyed(@NonNull final Activity activity) {
            //do nothing
        }

        private void callListeners() {
            for (Runnable r : lifecycleListeners) {
                r.run();
            }
        }
    }



    public CgeoApplication() {
        instance = this;
        this.lifecycleInfo = new LifecycleInfo();
        try {
            registerActivityLifecycleCallbacks(this.lifecycleInfo);
        } catch (Exception ex) {
            Log.e("Exception", ex);
        }
    }

    public static CgeoApplication getInstance() {
        return instance;
    }

    public Activity getCurrentForegroundActivity() {
        return lifecycleInfo.currentForegroundActivity;
    }

    public void addLifecycleListener(final Runnable run) {
        this.lifecycleInfo.lifecycleListeners.add(run);
    }

    @Override
    public void onCreate() {
        Log.iForce("---------------- CGeoApplication: startup -------------");
        Log.e("c:geo version " + BuildConfig.VERSION_NAME);
        try (ContextLogger ignore = new ContextLogger(true, "CGeoApplication.onCreate")) {
            super.onCreate();

            TransactionSizeLogger.get().setRequested();

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

            // dump hash key to log, if requested
            // Log.e("app hashkey: " + getApplicationHashkey(this));

            MessageCenterUtils.configureMessageCenterPolling();

            LooperLogger.startLogging(Looper.getMainLooper());

            applyVTMScales();
        }
    }

    private void applyVTMScales() {
        CanvasAdapter.userScale = Settings.getInt(R.string.pref_vtmUserScale, 100) / 100.0f;
        CanvasAdapter.textScale = Settings.getInt(R.string.pref_vtmTextScale, 100) / 100f;
        CanvasAdapter.symbolScale = Settings.getInt(R.string.pref_vtmSymbolScale, 100) / 100f;
    }

    /**
     * <a href="https://code.google.com/p/android/issues/detail?id=173789">...</a>
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
    public void initApplicationLocale() {
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


    // retrieve fingerprint with getKeyHash(context)
    @SuppressWarnings("unused")
    @SuppressLint("PackageManagerGetSignatures")
    private static String getApplicationHashkey(final Context context) {
        final char[] hexChars = "0123456789ABCDEF".toCharArray();
        final StringBuilder sb = new StringBuilder();
        try {
            final PackageInfo info;
            info = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                final MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                for (byte c : md.digest()) {
                    sb.append(hexChars[Byte.toUnsignedInt(c) / 16]).append(hexChars[c & 15]).append(' ');
                }
            }
        } catch (Exception e) {
            return e.toString();
        }
        return sb.toString();
    }
}
