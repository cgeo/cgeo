package cgeo.geocaching.location;

import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;

public class ProximityNotification implements Parcelable {

    // tones
    protected static final int TONE_NONE = 0;
    protected static final int TONE_FAR = ToneGenerator.TONE_PROP_BEEP;
    protected static final int TONE_NEAR = ToneGenerator.TONE_PROP_BEEP2;

    // minimum increase in distance before distance gets reset
    protected static final long MIN_DISTANCE_DELTA = 10;

    // minimum time diff in ms (if repeatedSignal is enabled)
    protected static final long MIN_TIME_DELTA_FAR = 10000;
    protected static final long MIN_TIME_DELTA_NEAR = 5000;

    // how many consecutive distance peaks to ignore?
    protected  static final int IGNORE_PEAKS = 3;

    // context this is running in
    protected Context context = null;

    // config options - get initialized in constructor
    protected int distanceFar;
    protected int distanceNear;
    protected boolean twoDistances;
    protected boolean repeatedSignal;

    // temp values - get initialized in resetValues()
    protected int lastDistance;
    protected long lastTimestamp;
    protected int lastTone;
    protected int ignorePeaks;

    // parcelable functions
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeInt(distanceFar);
        out.writeInt(distanceNear);
        out.writeByte((byte) (twoDistances ? 1 : 0));
        out.writeByte((byte) (repeatedSignal ? 1 : 0));
        out.writeInt(lastDistance);
        out.writeLong(lastTimestamp);
        out.writeInt(lastTone);
        out.writeInt(ignorePeaks);
    }

    public static final Parcelable.Creator<ProximityNotification> CREATOR
            = new Parcelable.Creator<ProximityNotification>() {
        public ProximityNotification createFromParcel(final Parcel in) {
            return new ProximityNotification(in);
        }

        public ProximityNotification[] newArray(final int size) {
            return new ProximityNotification[size];
        }
    };

    protected ProximityNotification(final Parcel in) {
        distanceFar = in.readInt();
        distanceNear = in.readInt();
        twoDistances = in.readByte() == 1;
        repeatedSignal = in.readByte() == 1;
        lastDistance = in.readInt();
        lastTimestamp = in.readLong();
        lastTone = in.readInt();
        ignorePeaks = in.readInt();
    }

    // regular constructor

    public ProximityNotification(final Context context, final boolean twoDistances, final boolean repeatedSignal) {
        this.context = context;

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
        ignorePeaks = IGNORE_PEAKS;
    }

    public void checkDistance (final int distance) {
        // no precise distances
        if (distance > Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            return;
        }
        if (lastDistance > Settings.PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            lastDistance = distance;
        }
        if (distance > lastDistance && ignorePeaks > 0) {
            // after opening cache popup we sometimes have an irregular one time peak => ignore
            ignorePeaks--;
            return;
        }
        ignorePeaks = IGNORE_PEAKS;

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
            lastTimestamp = currentTimestamp;
            lastDistance = distance;
            lastTone = tone;
            final Handler handler = new Handler(Looper.getMainLooper());

            final int ringerMode = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
            switch (ringerMode) {
                case AudioManager.RINGER_MODE_NORMAL:
                    final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                    toneG.startTone(tone);
                    if (tone == TONE_NEAR) {
                        handler.postDelayed(() -> {
                            toneG.startTone(TONE_NEAR);
                            handler.postDelayed(() -> {
                                toneG.release();
                            }, 350);
                        }, 350);
                    } else {
                        handler.postDelayed(() -> {
                            toneG.release();
                        }, 350);
                    }
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    final Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    // @todo: when compiling with targetSDK 28 or higher use case by Build.VERSION.SDK_VERSION and for >= 26 use
                    // v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                    if (tone == TONE_NEAR) {
                        v.vibrate(300);
                        handler.postDelayed(() -> {
                            v.vibrate(300);
                        }, 500);
                    } else {
                        v.vibrate(300);
                    }
                    break;
                // otherwise: stay silent
            }
        }
    }
}
