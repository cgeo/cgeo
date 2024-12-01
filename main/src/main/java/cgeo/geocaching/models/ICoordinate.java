package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;

public interface ICoordinate {

    Geopoint getCoords();

    /** instances of ICoordinates may optionally provide elevation information */
    default float getElevation() {
        return 0f;
    }

}
