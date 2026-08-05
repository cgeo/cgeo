package cgeo.geocaching.speech;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.notifications.Notifications;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.ui.notifications.NotificationChannels.FOREGROUND_SERVICE_NOTIFICATION;
import static cgeo.geocaching.ui.notifications.Notifications.ID_FOREGROUND_NOTIFICATION_SPEECH_SERVICE;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.OnInitListener;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.apache.commons.lang3.StringUtils;

/**
 * Service to speak the compass directions.
 */
public class SpeechService extends Service implements OnInitListener {

    private static final int SPEECH_MINPAUSE_SECONDS = 5;
    private static final int SPEECH_MAXPAUSE_SECONDS = 30;
    @Nullable
    private static Activity startingActivity;
    private static final Object startingActivityLock = new Object();
    /**
     * Text to speech API of Android
     */
    private TextToSpeech tts;
    /**
     * TTS has been initialized and we can speak.
     */
    private boolean initialized = false;
    protected float direction;
    protected Geopoint position;

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {

        @Override
        public void updateGeoDir(@NonNull final GeoData newGeo, final float newDirection) {
            // We might receive a location update before the target has been set. In this case, do nothing.
            if (target == null) {
                return;
            }

            position = newGeo.getCoords();
            direction = newDirection;
            // avoid any calculation, if the delay since the last output is not long enough
            final long now = System.currentTimeMillis();
            if (now - lastSpeechTime <= SPEECH_MINPAUSE_SECONDS * 1000) {
                return;
            }

            // to speak, we want max pause to have elapsed or distance to geopoint to have changed by a given amount
            final float distance = position.distanceTo(target);
            if (now - lastSpeechTime <= SPEECH_MAXPAUSE_SECONDS * 1000 && Math.abs(lastSpeechDistance - distance) < getDeltaForDistance(distance)) {
                return;
            }

            final String text = TextFactory.getText(position, target, direction);
            if (StringUtils.isNotEmpty(text)) {
                lastSpeechTime = System.currentTimeMillis();
                lastSpeechDistance = distance;
                speak(text);
            }
        }
    };
    /**
     * remember when we talked the last time
     */
    private long lastSpeechTime = 0;
    private float lastSpeechDistance = 0.0f;
    private Geopoint target;
    private final CompositeDisposable initDisposable = new CompositeDisposable();
    private PowerManager.WakeLock wakeLock;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    /**
     * Return distance required to be moved based on overall distance.<br>
     *
     * @param distance in km
     * @return delta in km
     */
    private static float getDeltaForDistance(final float distance) {
        if (distance > 1.0) {
            return 0.2f;
        }
        if (distance > 0.05) {
            return distance / 5.0f;
        }
        return 0f;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, this);

        final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cgeo:SpeechService");
        // Set timeout to protect battery life. It's acceptable that the service can get killed after one hour.
        wakeLock.acquire(60 * 60 * 1000);
        Log.w("SpeechService - WakeLock acquired");


        startForeground(ID_FOREGROUND_NOTIFICATION_SPEECH_SERVICE, Notifications
                .createNotification(this, FOREGROUND_SERVICE_NOTIFICATION, R.string.tts_service)
                .setContentText(getString(R.string.tts_running))
                .build());
    }

    @Override
    public void onDestroy() {
        initDisposable.clear();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        wakeLock.release();
        Log.w("SpeechService - WakeLock released");
        super.onDestroy();
    }

    @Override
    public void onInit(final int status) {
        // The text to speech system takes some time to initialize.
        if (status != TextToSpeech.SUCCESS) {
            Log.e("Text to speech cannot be initialized.");
            return;
        }

        final int switchLocale = tts.setLanguage(Settings.getApplicationLocale());

        if (switchLocale == TextToSpeech.LANG_MISSING_DATA) {
            synchronized (startingActivityLock) {
                if (startingActivity != null) {
                    startingActivity.startActivity(new Intent(Engine.ACTION_INSTALL_TTS_DATA));
                }
            }
            return;
        }
        if (switchLocale == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("Current language not supported by text to speech.");
            synchronized (startingActivityLock) {
                if (startingActivity != null) {
                    ActivityMixin.showToast(startingActivity, R.string.err_tts_lang_not_supported);
                }
            }
            return;
        }

        initialized = true;

        synchronized (startingActivityLock) {
            final Activity startingActivityChecked = startingActivity;
            if (startingActivityChecked != null) {
                initDisposable.add(geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR));
                ActivityMixin.showShortToast(startingActivity, startingActivityChecked.getString(R.string.tts_started));
            }
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            target = intent.getParcelableExtra(Intents.EXTRA_COORDS);
        }
        return START_NOT_STICKY; // service can be stopped by system, if under memory pressure
    }

    @SuppressWarnings("deprecation")
    private void speak(final String text) {
        if (!initialized) {
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private static void startService(final Activity activity, final Geopoint dstCoords) {
        synchronized (startingActivityLock) {
            startingActivity = activity;
        }
        final Intent talkingService = new Intent(activity, SpeechService.class);
        talkingService.putExtra(Intents.EXTRA_COORDS, dstCoords);
        activity.startService(talkingService);
    }

    private static void stopService(final Activity activity) {
        synchronized (startingActivityLock) {
            if (activity.stopService(new Intent(activity, SpeechService.class))) {
                ActivityMixin.showShortToast(activity, activity.getString(R.string.tts_stopped));
            }
            startingActivity = null;
        }
    }

    public static void toggleService(final ComponentActivity activity, final Geopoint dstCoords) {
        if (isRunning()) {
            stopService(activity);
        } else {
            startService(activity, dstCoords);
            activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull final LifecycleOwner owner) {
                    stopService(activity);
                }
            });
        }
    }

    public static boolean isRunning() {
        return startingActivity != null;
    }
}
