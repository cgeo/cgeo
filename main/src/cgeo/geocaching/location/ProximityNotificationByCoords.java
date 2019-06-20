package cgeo.geocaching.location;

import cgeo.geocaching.sensors.GeoData;

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
            checkDistance((int) (1000f * referencePoint.distanceTo(geo.getCoords())));
        }
    }

}
