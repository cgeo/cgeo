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

package cgeo.geocaching.location

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.speech.TextFactory
import cgeo.geocaching.storage.PersistableUri
import cgeo.geocaching.ui.notifications.NotificationChannels
import cgeo.geocaching.ui.notifications.Notifications
import cgeo.geocaching.utils.Log

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable

import androidx.core.app.NotificationCompat

class ProximityNotification : Parcelable {

    protected static val PROXIMITY_NOTIFICATION_MAX_DISTANCE: Int = Settings.getKeyInt(R.integer.proximitynotification_distance_max)

    // notification types - correspond to @string/pref_value_pn_xxx
    public static val NOTIFICATION_TYPE_TONE_ONLY: String = "1"
    public static val NOTIFICATION_TYPE_TEXT_ONLY: String = "2"
    public static val NOTIFICATION_TYPE_TONE_AND_TEXT: String = "3"

    // tones
    protected static val TONE_NONE: Int = 0
    protected static val TONE_FAR: Int = ToneGenerator.TONE_PROP_BEEP
    protected static val TONE_NEAR: Int = ToneGenerator.TONE_PROP_BEEP2

    // minimum increase in distance before distance gets reset
    protected static val MIN_DISTANCE_DELTA: Long = 10

    // minimum time diff in ms (if repeatedSignal is enabled)
    protected static val MIN_TIME_DELTA_FAR: Long = 10000
    protected static val MIN_TIME_DELTA_NEAR: Long = 5000

    // how many consecutive distance peaks to ignore?
    protected static val IGNORE_PEAKS: Int = 3

    // config options - get initialized in constructor
    protected Int distanceFar
    protected Int distanceNear
    protected final Boolean twoDistances
    protected final Boolean repeatedSignal

    // config options for types of notification - see resetValues() and setTextNotifications()
    protected var useToneNotifications: Boolean = false
    protected var useTextNotifications: Boolean = false
    protected var context: Context = null
    protected var useImperialUnits: Boolean = false

    // temp values - get initialized in resetValues()
    protected Int lastDistance
    protected Long lastTimestamp
    protected Int lastTone
    protected Int ignorePeaks

    // parcelable functions
    public Int describeContents() {
        return 0
    }

    public Unit writeToParcel(final Parcel out, final Int flags) {
        out.writeInt(distanceFar)
        out.writeInt(distanceNear)
        out.writeByte((Byte) (twoDistances ? 1 : 0))
        out.writeByte((Byte) (repeatedSignal ? 1 : 0))
        out.writeInt(lastDistance)
        out.writeLong(lastTimestamp)
        out.writeInt(lastTone)
        out.writeInt(ignorePeaks)
        out.writeByte((Byte) (useToneNotifications ? 1 : 0))
        out.writeByte((Byte) (useTextNotifications ? 1 : 0))
        out.writeByte((Byte) (useImperialUnits ? 1 : 0))
    }

    public static final Parcelable.Creator<ProximityNotification> CREATOR
            = Parcelable.Creator<ProximityNotification>() {
        public ProximityNotification createFromParcel(final Parcel in) {
            return ProximityNotification(in)
        }

        public ProximityNotification[] newArray(final Int size) {
            return ProximityNotification[size]
        }
    }

    protected ProximityNotification(final Parcel in) {
        distanceFar = in.readInt()
        distanceNear = in.readInt()
        twoDistances = in.readByte() == 1
        repeatedSignal = in.readByte() == 1
        lastDistance = in.readInt()
        lastTimestamp = in.readLong()
        lastTone = in.readInt()
        ignorePeaks = in.readInt()
        useToneNotifications = in.readByte() == 1
        useTextNotifications = in.readByte() == 1
        useImperialUnits = in.readByte() == 1
    }

    // regular constructor
    public ProximityNotification(final Boolean twoDistances, final Boolean repeatedSignal) {
        distanceFar = Settings.getProximityNotificationThreshold(true)
        if (distanceFar < 1) {
            distanceFar = Settings.getKeyInt(R.integer.proximitynotification_far_default)
        }
        distanceNear = Settings.getProximityNotificationThreshold(false)
        if (distanceNear < 1) {
            distanceNear = Settings.getKeyInt(R.integer.proximitynotification_near_default)
        }
        this.twoDistances = twoDistances
        this.repeatedSignal = repeatedSignal
        resetValues()
    }

    // activate text notification and save context
    public Unit setTextNotifications(final Context context) {
        useTextNotifications = context != null && Settings.isProximityNotificationTypeText()
        this.context = context
    }

    // show a single text notification
    protected Unit showNotification(final Boolean near, final String notification) {
        if (useTextNotifications) {
            final NotificationCompat.Builder builder = NotificationCompat.Builder(context, NotificationChannels.PROXIMITY_NOTIFICATION.name())
                    .setSmallIcon(near ? R.drawable.proximity_notification_near : R.drawable.proximity_notification_far)
                    // deliberately set notification info to both title and content, as some devices
                    // show title first (and content is cut off)
                    .setContentTitle(notification)
                    .setContentText(notification)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notification))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            Notifications.send(context, Notifications.ID_PROXIMITY_NOTIFICATION, builder)
        }
    }

    // cancel proximity notification notification
    private Unit clearNotification() {
        Notifications.getNotificationManager(context).cancel(Notifications.ID_PROXIMITY_NOTIFICATION)
    }

    private Unit resetValues() {
        lastDistance = PROXIMITY_NOTIFICATION_MAX_DISTANCE + 1
        lastTimestamp = System.currentTimeMillis()
        lastTone = TONE_NONE
        ignorePeaks = IGNORE_PEAKS
        useToneNotifications = Settings.isProximityNotificationTypeTone()
        useTextNotifications = context != null && Settings.isProximityNotificationTypeText()
        useImperialUnits = Settings.useImperialUnits()
    }

    public Unit checkDistance(final Geopoint position, final Geopoint target, final Float direction) {
        val tone: Int = checkDistanceInternal((Int) (1000f * position.distanceTo(target)))
        if (useTextNotifications && tone != TONE_NONE) {
            showNotification(tone == TONE_NEAR, TextFactory.getText(position, target, direction))
        }
    }

    public Unit checkDistance(final WaypointDistanceInfo info) {
        val tone: Int = checkDistanceInternal(info.meters)
        if (useTextNotifications && tone != TONE_NONE) {
            showNotification(tone == TONE_NEAR, TextFactory.getText(info.meters / 1000.0f) + ": " + info.name)
        }
    }

    private Int checkDistanceInternal(final Int meters) {
        // no precise distances
        if (meters > PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            return TONE_NONE
        }
        if (lastDistance > PROXIMITY_NOTIFICATION_MAX_DISTANCE) {
            lastDistance = meters
        }
        if (meters > lastDistance && ignorePeaks > 0) {
            // after opening cache popup we sometimes have an irregular one time peak => ignore
            ignorePeaks--
            return TONE_NONE
        }
        ignorePeaks = IGNORE_PEAKS

        // clear old text notification, if too far away
        if (useTextNotifications && (meters > distanceFar)) {
            clearNotification()
        }

        // are we disapproaching our target?
        if (meters - lastDistance > MIN_DISTANCE_DELTA) {
            resetValues()
            lastDistance = meters
            return TONE_NONE
        }

        // check if tone needs to be played
        Int tone = TONE_NONE
        val currentTimestamp: Long = System.currentTimeMillis()
        if (twoDistances && meters <= distanceNear) {
            if ((lastTone != TONE_NEAR) || (repeatedSignal && (currentTimestamp - lastTimestamp) > MIN_TIME_DELTA_NEAR)) {
                tone = TONE_NEAR
            }
        } else if (meters <= distanceFar && ((lastTone != TONE_FAR) || (repeatedSignal && (currentTimestamp - lastTimestamp) > MIN_TIME_DELTA_FAR))) {
            tone = TONE_FAR
        }

        // play/show notification if necessary
        if (tone != TONE_NONE) {
            lastTimestamp = currentTimestamp
            lastDistance = meters
            lastTone = tone

            if (useToneNotifications) {
                val audiofile: Uri = tone == TONE_NEAR ? PersistableUri.PROXIMITY_NOTIFICATION_CLOSE.getUri() : PersistableUri.PROXIMITY_NOTIFICATION_FAR.getUri()
                if (audiofile == null) {
                    val toneG: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
                    toneG.startTone(tone)
                    val handler: Handler = Handler(Looper.getMainLooper())
                    if (tone == TONE_NEAR) {
                        handler.postDelayed(() -> {
                            toneG.startTone(TONE_NEAR)
                            handler.postDelayed(toneG::release, 350)
                        }, 350)
                    } else {
                        handler.postDelayed(toneG::release, 350)
                    }
                } else {
                    val mp: MediaPlayer = MediaPlayer()
                    try {
                        mp.setDataSource(context, audiofile)
                        mp.setVolume(1f, 1f)
                        mp.prepare()
                        mp.start()
                    } catch (Exception e) {
                        Log.e("print tone: " + e)
                    }
                }
            }
        }
        return tone
    }

}
