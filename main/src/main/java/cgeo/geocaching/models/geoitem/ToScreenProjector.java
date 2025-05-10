package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;

/** Projects lat/lon-coordinates to screen coordinates for a concrete map */
public interface ToScreenProjector {

    /**
     * for a given geopoint, returns an int array of size 2 (x, y) containing the
     * screen coordinate for the given lat/lon for a currently displayed map. Any visualization
     * details of the map (e.g. rotating, tilting, zooming, ...) should be accomodated for by the
     * implementor.
     */
    int[] project(Geopoint t);
}
