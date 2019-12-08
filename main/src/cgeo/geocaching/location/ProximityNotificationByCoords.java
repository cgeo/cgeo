package cgeo.geocaching.location;

import cgeo.geocaching.sensors.GeoData;

import android.os.Parcel;
import android.os.Parcelable;

public class ProximityNotificationByCoords extends ProximityNotification {

    // reference point to calculate distance to
    // needs to be set by calling setReferencePoint()
    private Geopoint referencePoint = null;

    public ProximityNotificationByCoords() {
        super(true, true);
    }

    public void setReferencePoint(final Geopoint referencePoint) {
        this.referencePoint = referencePoint;
    }

    public void onUpdateGeoData(final GeoData geo) {
        if (null != referencePoint) {
            checkDistance(geo.getCoords(), referencePoint, geo.getBearing());
        }
    }

    // extend parcelable functions

    public void writeToParcel(final Parcel out, final int flags) {
        super.writeToParcel(out, flags);
        referencePoint.writeToParcel(out, flags);
    }

    public static final Parcelable.Creator<ProximityNotificationByCoords> CREATOR
            = new Parcelable.Creator<ProximityNotificationByCoords>() {
        public ProximityNotificationByCoords createFromParcel(final Parcel in) {
            return new ProximityNotificationByCoords(in);
        }

        public ProximityNotificationByCoords[] newArray(final int size) {
            return new ProximityNotificationByCoords[size];
        }
    };

    private ProximityNotificationByCoords(final Parcel in) {
        super(in);
        referencePoint = new Geopoint(in);
    }

}
