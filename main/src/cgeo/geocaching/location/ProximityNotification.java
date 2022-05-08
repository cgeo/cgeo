package cgeo.geocaching.location;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.speech.TextFactory;
import cgeo.geocaching.ui.notifications.NotificationChannels;
import cgeo.geocaching.ui.notifications.Notifications;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.core.app.NotificationCompat;

public class ProximityNotification implements Parcelable {

    protected static final int PROXIMITY_NOTIFICATION_MAX_DISTANCE = Settings.getKeyInt(R.integer.proximitynotification_distance_max);

    // notification types - correspond to @string/pref_value_pn_xxx
    public static final String NOTIFICATION_TYPE_TONE_ONLY = "1";
    public static final String NOTIFICATION_TYPE_TEXT_ONLY = "2";
    public static final String NOTIFICATION_TYPE_TONE_AND_TEXT = "3";

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
    protected static final int IGNORE_PEAKS = 3;

    // config options - get initialized in constructor
    protected int distanceFar;
    protected int distanceNear;
    protected final boolean twoDistances;
    protected final boolean repeatedSignal;

    // config options for types of notification - see resetValues() and setTextNotifications()
    protected boolean useToneNotifications = false;
    protected boolean useTextNotifications = false;
    protected Context context = null;
    protected boolean useImperialUnits = false;

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
        out.writeByte((byte) (useToneNotifications ? 1 : 0));
        out.writeByte((byte) (useTextNotifications ? 1 : 0));
        out.writeByte((byte) (useImperialUnits ? 1 : 0));
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
        useToneNotifications = in.readByte() == 1;
        useTextNotifications = in.readByte() == 1;
        useImperialUnits = in.readByte() == 1;
    }

    // regular constructor
    public ProximityNotification(final boolean twoDistances, final boolean repeatedSignal) {
        distanceFar = Settings.getProximityNotificationThreshold(true);
        if (distanceFar < 1) {
            distanceFar = Settings.getKeyInt(R.integer.proximitynotification_far_default);
        }
        distanceNear = Settings.getProximityNotificationThreshold(false);
        if (distanceNear < 1) {
            distanceNear = Settings.getKeyInt(R.integer.proximitynotification_near_default);
        }
        this.twoDistances = twoDistances;
        this.repeatedSignal = repeatedSignal;
        resetValues();
    }

    // activate text notification and save context
    public void setTextNotifications(final Context context) {
        useTextNotifications = context != null && Settings.isProximityNotificationTypeText();
        this.context = context;
    }

    // show a single text notification
    protected void showNotification(final boolean near, final String notification) {
        if (useTextNotifications) {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.PROXIMITY_NOTIFICATION.name())
                    .setSmallIcon(near ? R.drawable.proximity_notification_near : R.drawable.proximity_notification_far)
                    .setContentTitle(context.getString(R.string.notification_proximity_title))
                    .setContentText(notification)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notification))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            Notifications.getNotificationManager(context).notify(Notifications.ID_PROXIMITY_NOTIFICATION, builder.build());
        }
    }

    // cancel proximity notification notification
    private void clearNotification() {
        Notifications.getNotificationManager(context).cancel(Notifications.ID_PROXIMITY_NOTIFICATION);
    }

    private void resetValues() {
        lastDistance = PROXIMITY_NOTIFICATION_MAX_DISTANCE + 1;
        lastTimestamp = System.currentTimeMillis();
        lastTone = TONE_NONE;
        ignorePeaks = IGNORE_PEAKS;
        useToneNotifications = Settings.isProximityNotificationTypeTone();
        useTextNotifications = context != null && Settings.isProximityNotificationTypeText();
        useImperialUnits = Settings.useImperialUnits();
    }

    public void checkDistance(final Geopoint position, final Geopoint target, final float direction) {
        final int tone = checkDistanceInternal((int) (1000f * position.distanceTo(target)));
        if (useTextNotifications && tone != TONE_NONE) {
            showNotification(tone == TONE_NEAR, TextFactory.getText(position, target, direction));
        }
    }

    public void checkDistance(final int meters) {
        final int tone = checkDistanceInternal(meters);
        if (useTextNotifications && tone != TONE_NONE) {
            showNotification(tone == TONE_NEAR, TextFactory.getText(meters / 1000.0f));
        }
    }

    public void checkDistance(final WaypointDistanceInfo info) {
        final int tone = checkDistanceInternal(info.meters);
        if (useTextNotifications && tone != TONE_NONE) {
            showNotification(tone == TONE_NEAR, TextFactory.getText(info.meters / 1000.0f) + ": " + info.name);
        }
    }

    private int checkDistanceInternal(final int meters) {
        // no precise distances
        if (meters > PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            return TONE_NONE;
        }
        if (lastDistance > PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            lastDistance = meters;
        }
        if (meters > lastDistance && ignorePeaks > 0) {
            // after opening cache popup we sometimes have an irregular one time peak => ignore
            ignorePeaks--;
            return TONE_NONE;
        }
        ignorePeaks = IGNORE_PEAKS;

        // clear old text notification, if too far away
        if (useTextNotifications && (meters > distanceFar)) {
            clearNotification();
        }

        // are we disapproaching our target?
        if (meters - lastDistance > MIN_DISTANCE_DELTA) {
            resetValues();
            lastDistance = meters;
            return TONE_NONE;
        }

        // check if tone needs to be played
        int tone = TONE_NONE;
        final long currentTimestamp = System.currentTimeMillis();
        if (twoDistances && meters <= distanceNear) {
            if ((lastTone != TONE_NEAR) || (repeatedSignal && (currentTimestamp - lastTimestamp) > MIN_TIME_DELTA_NEAR)) {
                tone = TONE_NEAR;
            }
        } else if (meters <= distanceFar && ((lastTone != TONE_FAR) || (repeatedSignal && (currentTimestamp - lastTimestamp) > MIN_TIME_DELTA_FAR))) {
            tone = TONE_FAR;
        }

        // play/show notification if necessary
        if (tone != TONE_NONE) {
            lastTimestamp = currentTimestamp;
            lastDistance = meters;
            lastTone = tone;

            if (useToneNotifications) {
                final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
                toneG.startTone(tone);
                final Handler handler = new Handler(Looper.getMainLooper());
                if (tone == TONE_NEAR) {
                    handler.postDelayed(() -> {
                        toneG.startTone(TONE_NEAR);
                        handler.postDelayed(toneG::release, 350);
                    }, 350);
                } else {
                    handler.postDelayed(toneG::release, 350);
                }
            }
        }
        return tone;
    }

}
