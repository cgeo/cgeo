// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching

import cgeo.geocaching.network.Cookies
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.utils.CgeoUncaughtExceptionHandler
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MessageCenterUtils
import cgeo.geocaching.utils.TransactionSizeLogger
import cgeo.geocaching.utils.offlinetranslate.TranslationModelManager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.security.MessageDigest
import java.util.ArrayList
import java.util.List
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import org.oscim.backend.CanvasAdapter

class CgeoApplication : Application() {

    private static CgeoApplication instance
    private val lowPrioNotificationCounter: AtomicInteger = AtomicInteger(0)
    private val hasHighPrioNotification: AtomicBoolean = AtomicBoolean(false)

    private final LifecycleInfo lifecycleInfo

    private static class LifecycleInfo : ActivityLifecycleCallbacks {

        private var currentForegroundActivity: Activity = null
        private val lifecycleListeners: List<Runnable> = ArrayList<>()

        override         public Unit onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
            //do nothing
        }

        override         public Unit onActivityStarted(final Activity activity) {
            //do nothing
        }

        override         public Unit onActivityResumed(final Activity activity) {
            if (currentForegroundActivity != activity) {
                currentForegroundActivity = activity
                callListeners()
            }
        }

        override         public Unit onActivityPaused(final Activity activity) {
            if (activity == currentForegroundActivity) {
                currentForegroundActivity = null
                callListeners()
            }
        }

        override         public Unit onActivityStopped(final Activity activity) {
            //do nothing
        }

        override         public Unit onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
            //do nothing
        }

        override         public Unit onActivityDestroyed(final Activity activity) {
            //do nothing
        }

        private Unit callListeners() {
            for (Runnable r : lifecycleListeners) {
                r.run()
            }
        }
    }



    public CgeoApplication() {
        instance = this
        this.lifecycleInfo = LifecycleInfo()
        try {
            registerActivityLifecycleCallbacks(this.lifecycleInfo)
        } catch (Exception ex) {
            Log.e("Exception", ex)
        }
    }

    public static CgeoApplication getInstance() {
        return instance
    }

    public Activity getCurrentForegroundActivity() {
        return lifecycleInfo.currentForegroundActivity
    }

    public Unit addLifecycleListener(final Runnable run) {
        this.lifecycleInfo.lifecycleListeners.add(run)
    }

    override     public Unit onCreate() {
        Log.iForce("---------------- CGeoApplication: startup -------------")
        Log.e("c:geo version " + BuildConfig.VERSION_NAME)
        try (ContextLogger ignore = ContextLogger(true, "CGeoApplication.onCreate")) {
            super.onCreate()

            TransactionSizeLogger.get().setRequested()

            // error handlers
            CgeoUncaughtExceptionHandler.installUncaughtExceptionHandler(this)

            Settings.setAppThemeAutomatically(this)

            initApplicationLocale()

            // initialize cgeo notification channels
            NotificationChannels.createNotificationChannels(this)

            // ensure initialization of lists
            DataStore.getLists()

            // Restore cookies
            Cookies.restoreCookies()

            // dump hash key to log, if requested
            // Log.e("app hashkey: " + getApplicationHashkey(this))

            MessageCenterUtils.configureMessageCenterPolling()

            LooperLogger.startLogging(Looper.getMainLooper())

            applyVTMScales()

            //initialize TranslationModelManager
            TranslationModelManager.get().initialize()
        }
    }

    private Unit applyVTMScales() {
        CanvasAdapter.userScale = Settings.getInt(R.string.pref_vtmUserScale, 100) / 100.0f
        CanvasAdapter.textScale = Settings.getInt(R.string.pref_vtmTextScale, 100) / 100f
        CanvasAdapter.symbolScale = Settings.getInt(R.string.pref_vtmSymbolScale, 100) / 100f
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)

        initApplicationLocale()
    }

    override     public Unit onLowMemory() {
        super.onLowMemory()
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    @SuppressLint("NewApi")
    override     public Unit onTrimMemory(final Int level) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.i("Cleaning applications cache to trim memory")
            DataStore.removeAllFromCache()
        }
    }

    /**
     * Enforce a specific language if the user decided so.
     */
    public Unit initApplicationLocale() {
        val config: Configuration = getResources().getConfiguration()
        config.locale = Settings.getApplicationLocale()
        val resources: Resources = getResources()
        resources.updateConfiguration(config, resources.getDisplayMetrics())
    }

    public AtomicInteger getLowPrioNotificationCounter() {
        return lowPrioNotificationCounter
    }

    public AtomicBoolean getHasHighPrioNotification() {
        return hasHighPrioNotification
    }


    // retrieve fingerprint with getKeyHash(context)
    @SuppressWarnings("unused")
    @SuppressLint("PackageManagerGetSignatures")
    private static String getApplicationHashkey(final Context context) {
        final Char[] hexChars = "0123456789ABCDEF".toCharArray()
        val sb: StringBuilder = StringBuilder()
        try {
            final PackageInfo info
            info = context.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNATURES)
            for (Signature signature : info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                for (Byte c : md.digest()) {
                    sb.append(hexChars[Byte.toUnsignedInt(c) / 16]).append(hexChars[c & 15]).append(' ')
                }
            }
        } catch (Exception e) {
            return e.toString()
        }
        return sb.toString()
    }
}
