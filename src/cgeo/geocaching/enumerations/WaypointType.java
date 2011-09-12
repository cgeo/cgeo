package cgeo.geocaching.enumerations;

import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;

/**
 * Enum listing waypoint types
 *  
 * @author koem
 */
public enum WaypointType {
    FLAG      ("flag",      PointGeocachingData.CACHE_WAYPOINT_TYPE_FINAL),
    OWN       ("own",       PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES),
    PKG       ("pkg",       PointGeocachingData.CACHE_WAYPOINT_TYPE_PARKING),
    PUZZLE    ("puzzle",    PointGeocachingData.CACHE_WAYPOINT_TYPE_QUESTION),
    STAGE     ("stage",     PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES),
    TRAILHEAD ("trailhead", PointGeocachingData.CACHE_WAYPOINT_TYPE_TRAILHEAD),
    WAYPOINT  ("waypoint",  PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES);
    
    public final String cgeoId;
    public final String locusId;

    private WaypointType(String cgeoId, String locusId) {
        this.cgeoId = cgeoId;
        this.locusId = locusId;
    }
    
    public static WaypointType findByCgeoId(String cgeoId) {
        if (cgeoId == null) return null;
        for (WaypointType wt : WaypointType.values()) {
            if (wt.cgeoId.equals(cgeoId)) return wt;
        }
        return null;
    }

}
