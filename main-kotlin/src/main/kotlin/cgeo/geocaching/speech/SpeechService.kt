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

package cgeo.geocaching.speech

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.GeoDirHandler
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.utils.Log
import cgeo.geocaching.ui.notifications.NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION
import cgeo.geocaching.ui.notifications.Notifications.ID_FOREGROUND_NOTIFICATION_SPEECH_SERVICE

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import android.speech.tts.TextToSpeech.OnInitListener

import androidx.activity.ComponentActivity
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.apache.commons.lang3.StringUtils

/**
 * Service to speak the compass directions.
 */
class SpeechService : Service() : OnInitListener {

    private static val SPEECH_MINPAUSE_SECONDS: Int = 5
    private static val SPEECH_MAXPAUSE_SECONDS: Int = 30
    private static Activity startingActivity
    private static val startingActivityLock: Object = Object()
    /**
     * Text to speech API of Android
     */
    private TextToSpeech tts
    /**
     * TTS has been initialized and we can speak.
     */
    private var initialized: Boolean = false
    protected Float direction
    protected Geopoint position

    private val geoDirHandler: GeoDirHandler = GeoDirHandler() {

        override         public Unit updateGeoDir(final GeoData newGeo, final Float newDirection) {
            // We might receive a location update before the target has been set. In this case, do nothing.
            if (target == null) {
                return
            }

            position = newGeo.getCoords()
            direction = newDirection
            // avoid any calculation, if the delay since the last output is not Long enough
            val now: Long = System.currentTimeMillis()
            if (now - lastSpeechTime <= SPEECH_MINPAUSE_SECONDS * 1000) {
                return
            }

            // to speak, we want max pause to have elapsed or distance to geopoint to have changed by a given amount
            val distance: Float = position.distanceTo(target)
            if (now - lastSpeechTime <= SPEECH_MAXPAUSE_SECONDS * 1000 && Math.abs(lastSpeechDistance - distance) < getDeltaForDistance(distance)) {
                return
            }

            val text: String = TextFactory.getText(position, target, direction)
            if (StringUtils.isNotEmpty(text)) {
                lastSpeechTime = System.currentTimeMillis()
                lastSpeechDistance = distance
                speak(text)
            }
        }
    }
    /**
     * remember when we talked the last time
     */
    private var lastSpeechTime: Long = 0
    private var lastSpeechDistance: Float = 0.0f
    private Geopoint target
    private val initDisposable: CompositeDisposable = CompositeDisposable()
    private PowerManager.WakeLock wakeLock

    override     public IBinder onBind(final Intent intent) {
        return null
    }

    /**
     * Return distance required to be moved based on overall distance.<br>
     *
     * @param distance in km
     * @return delta in km
     */
    private static Float getDeltaForDistance(final Float distance) {
        if (distance > 1.0) {
            return 0.2f
        }
        if (distance > 0.05) {
            return distance / 5.0f
        }
        return 0f
    }

    override     public Unit onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)

        val powerManager: PowerManager = (PowerManager) getSystemService(POWER_SERVICE)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cgeo:SpeechService")
        // Set timeout to protect battery life. It's acceptable that the service can get killed after one hour.
        wakeLock.acquire(60 * 60 * 1000)
        Log.w("SpeechService - WakeLock acquired")


        startForeground(ID_FOREGROUND_NOTIFICATION_SPEECH_SERVICE, Notifications
                .createNotification(this, FOREGROUND_SERVICE_NOTIFICATION, R.string.tts_service)
                .setContentText(getString(R.string.tts_running))
                .build())
    }

    override     public Unit onDestroy() {
        initDisposable.clear()
        if (tts != null) {
            tts.stop()
            tts.shutdown()
        }
        wakeLock.release()
        Log.w("SpeechService - WakeLock released")
        super.onDestroy()
    }

    override     public Unit onInit(final Int status) {
        // The text to speech system takes some time to initialize.
        if (status != TextToSpeech.SUCCESS) {
            Log.e("Text to speech cannot be initialized.")
            return
        }

        val switchLocale: Int = tts.setLanguage(Settings.getApplicationLocale())

        if (switchLocale == TextToSpeech.LANG_MISSING_DATA) {
            synchronized (startingActivityLock) {
                if (startingActivity != null) {
                    startingActivity.startActivity(Intent(Engine.ACTION_INSTALL_TTS_DATA))
                }
            }
            return
        }
        if (switchLocale == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("Current language not supported by text to speech.")
            synchronized (startingActivityLock) {
                if (startingActivity != null) {
                    ActivityMixin.showToast(startingActivity, R.string.err_tts_lang_not_supported)
                }
            }
            return
        }

        initialized = true

        synchronized (startingActivityLock) {
            val startingActivityChecked: Activity = startingActivity
            if (startingActivityChecked != null) {
                initDisposable.add(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR))
                ActivityMixin.showShortToast(startingActivity, startingActivityChecked.getString(R.string.tts_started))
            }
        }
    }

    override     public Int onStartCommand(final Intent intent, final Int flags, final Int startId) {
        if (intent != null) {
            target = intent.getParcelableExtra(Intents.EXTRA_COORDS)
        }
        return START_NOT_STICKY; // service can be stopped by system, if under memory pressure
    }

    @SuppressWarnings("deprecation")
    private Unit speak(final String text) {
        if (!initialized) {
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
    }

    private static Unit startService(final Activity activity, final Geopoint dstCoords) {
        synchronized (startingActivityLock) {
            startingActivity = activity
        }
        val talkingService: Intent = Intent(activity, SpeechService.class)
        talkingService.putExtra(Intents.EXTRA_COORDS, dstCoords)
        activity.startService(talkingService)
    }

    private static Unit stopService(final Activity activity) {
        synchronized (startingActivityLock) {
            if (activity.stopService(Intent(activity, SpeechService.class))) {
                ActivityMixin.showShortToast(activity, activity.getString(R.string.tts_stopped))
            }
            startingActivity = null
        }
    }

    public static Unit toggleService(final ComponentActivity activity, final Geopoint dstCoords) {
        if (isRunning()) {
            stopService(activity)
        } else {
            startService(activity, dstCoords)
            activity.getLifecycle().addObserver(DefaultLifecycleObserver() {
                override                 public Unit onDestroy(final LifecycleOwner owner) {
                    stopService(activity)
                }
            })
        }
    }

    public static Boolean isRunning() {
        return startingActivity != null
    }
}
