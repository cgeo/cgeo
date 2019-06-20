package cgeo.geocaching.location;

import cgeo.geocaching.settings.Settings;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class ProximityNotification {

    // tones
    protected static final int TONE_NONE = 0;
    protected static final int TONE_FAR = ToneGenerator.TONE_PROP_BEEP;
    protected static final int TONE_NEAR = ToneGenerator.TONE_PROP_BEEP2;

    // minimum increase in distance before distance gets reset
    protected static final long MIN_DISTANCE_DELTA = 10;

    // minimum time diff in ms (if repeatedSignal is enabled)
    protected static final long MIN_TIME_DELTA_FAR = 10000;
    protected static final long MIN_TIME_DELTA_NEAR = 5000;

    // config options - get initialized in constructor
    protected int distanceFar;
    protected int distanceNear;
    protected boolean twoDistances;
    protected boolean repeatedSignal;

    // temp values - get initialized in resetValues()
    protected int lastDistance;
    protected long lastTimestamp;
    protected int lastTone;

    public ProximityNotification(final boolean twoDistances, final boolean repeatedSignal) {
        distanceFar = Settings.getProximityNotificationThreshold(true);
        if (distanceFar < 1) {
            distanceFar = Settings.PROXIMITY_NOTIFICATION_DISTANCE_FAR;
        }
        distanceNear = Settings.getProximityNotificationThreshold(false);
        if (distanceNear < 1) {
            distanceNear = Settings.PROXIMITY_NOTIFICATION_DISTANCE_NEAR;
        }
        this.twoDistances = twoDistances;
        this.repeatedSignal = repeatedSignal;
        resetValues();
    }

    private void resetValues() {
        lastDistance = Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE + 1;
        lastTimestamp = System.currentTimeMillis();
        lastTone = TONE_NONE;
    }

    public void checkDistance (final int distance) {
        // no precise distances
        if (distance > Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            return;
        }
        if (lastDistance > Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            lastDistance = distance;
        }

        // are we disapproaching our target?
        if (distance - lastDistance > MIN_DISTANCE_DELTA) {
            resetValues();
            lastDistance = distance;
            return;
        }

        // check if tone needs to be played
        int tone = TONE_NONE;
        final long currentTimestamp = System.currentTimeMillis();
        if (twoDistances && distance <= distanceNear) {
            if ((lastTone != TONE_NEAR) || (repeatedSignal && (currentTimestamp - lastTimestamp) > MIN_TIME_DELTA_NEAR)) {
                tone = TONE_NEAR;
            }
        } else if (distance <= distanceFar && ((lastTone != TONE_FAR) || (repeatedSignal && (currentTimestamp - lastTimestamp) > MIN_TIME_DELTA_FAR))) {
            tone = TONE_FAR;
        }

        // play tone if necessary
        if (tone != TONE_NONE) {
            final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
            toneG.startTone(tone);
            lastTimestamp = currentTimestamp;
            lastDistance = distance;
            lastTone = tone;
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                toneG.release();
            }, 350);
        }
    }
}
