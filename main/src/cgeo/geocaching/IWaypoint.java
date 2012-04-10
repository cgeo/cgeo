/**
 *
 */
package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;

/**
 * @author blafoo
 *
 */
public interface IWaypoint extends ILogable, ICoordinates {

    public abstract int getId();

    public abstract WaypointType getWaypointType();

    public abstract String getCoordType();

}
