package cgeo.geocaching.speech;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.OnInitListener;

import java.util.Locale;

/**
 * Service to speak the compass directions.
 *
 */
public class SpeechService extends Service implements OnInitListener {

    private static final int SPEECH_MINPAUSE_SECONDS = 5;
    private static final int SPEECH_MAXPAUSE_SECONDS = 30;
    private static Activity startingActivity;
    private static boolean isRunning = false;
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

    final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoDir(final GeoData newGeo, final float newDirection) {
            position = newGeo.getCoords();
            direction = newDirection;
            // avoid any calculation, if the delay since the last output is not long enough
            final long now = System.currentTimeMillis();
            if (now - lastSpeechTime <= SPEECH_MINPAUSE_SECONDS * 1000) {
                return;
            }

            // to speak, we want max pause to have elapsed or distance to geopoint to have changed by a given amount
            final float distance = position.distanceTo(target);
            if (now - lastSpeechTime <= SPEECH_MAXPAUSE_SECONDS * 1000) {
                if (Math.abs(lastSpeechDistance - distance) < getDeltaForDistance(distance)) {
                    return;
                }
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
    private Subscription initSubscription = Subscriptions.empty();

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    /**
     * Return distance required to be moved based on overall distance.<br>
     *
     * @param distance
     *            in km
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
    }

    @Override
    public void onDestroy() {
        initSubscription.unsubscribe();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(final int status) {
        // The text to speech system takes some time to initialize.
        if (status != TextToSpeech.SUCCESS) {
            Log.e("Text to speech cannot be initialized.");
            return;
        }

        Locale locale = Locale.getDefault();
        if (Settings.isUseEnglish()) {
            locale = Locale.ENGLISH;
        }

        final int switchLocale = tts.setLanguage(locale);

        if (switchLocale == TextToSpeech.LANG_MISSING_DATA) {
            startingActivity.startActivity(new Intent(Engine.ACTION_INSTALL_TTS_DATA));
            return;
        }
        if (switchLocale == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("Current languge not supported by text to speech.");
            ActivityMixin.showToast(startingActivity, R.string.err_tts_lang_not_supported);
            return;
        }

        initialized = true;

        initSubscription = geoDirHandler.start(GeoDirHandler.UPDATE_GEODIR);
        ActivityMixin.showShortToast(startingActivity, startingActivity.getResources().getString(R.string.tts_started));
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

    public static void startService(final Activity activity, final Geopoint dstCoords) {
        isRunning = true;
        startingActivity = activity;
        final Intent talkingService = new Intent(activity, SpeechService.class);
        talkingService.putExtra(Intents.EXTRA_COORDS, dstCoords);
        activity.startService(talkingService);
    }

    public static void stopService(final Activity activity) {
        isRunning = false;
        if (activity.stopService(new Intent(activity, SpeechService.class))) {
            ActivityMixin.showShortToast(activity, activity.getResources().getString(R.string.tts_stopped));
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

}
