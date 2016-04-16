package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.WaypointType;

public interface IWaypoint extends ILogable, ICoordinates {

    /**
     * Return an unique waypoint id.
     *
     * @return a non-negative id if set, -1 if unset
     */
    int getId();

    WaypointType getWaypointType();

    CoordinatesType getCoordType();

}
