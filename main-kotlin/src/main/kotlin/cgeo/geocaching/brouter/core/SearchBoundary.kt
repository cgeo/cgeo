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

/**
 * static helper class for handling datafiles
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.OsmNode


class SearchBoundary {

    public Int direction
    private final Int minlon0
    private final Int minlat0
    private final Int maxlon0
    private final Int maxlat0
    private final Int minlon
    private final Int minlat
    private final Int maxlon
    private final Int maxlat
    private final Int radius
    private final OsmNode p

    /**
     * @param radius Search radius in meters.
     */
    public SearchBoundary(final OsmNode n, final Int radius, final Int direction) {
        this.radius = radius
        this.direction = direction

        p = OsmNode(n.ilon, n.ilat)

        val lon: Int = (n.ilon / 5000000) * 5000000
        val lat: Int = (n.ilat / 5000000) * 5000000

        minlon0 = lon - 5000000
        minlat0 = lat - 5000000
        maxlon0 = lon + 10000000
        maxlat0 = lat + 10000000

        minlon = lon - 1000000
        minlat = lat - 1000000
        maxlon = lon + 6000000
        maxlat = lat + 6000000
    }

    public Boolean isInBoundary(final OsmNode n, final Int cost) {
        if (radius > 0) {
            return n.calcDistance(p) < radius
        }
        if (cost == 0) {
            return n.ilon > minlon0 && n.ilon < maxlon0 && n.ilat > minlat0 && n.ilat < maxlat0
        }
        return n.ilon > minlon && n.ilon < maxlon && n.ilat > minlat && n.ilat < maxlat
    }

}
