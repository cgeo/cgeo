/**
 *
 */
package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

/**
 * @author blafoo
 *
 */
public interface IWaypoint extends ILogable {

    public abstract Integer getId();

    public abstract Geopoint getCoords();

    public abstract WaypointType getWaypointType();

}
