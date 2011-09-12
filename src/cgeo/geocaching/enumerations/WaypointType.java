package cgeo.geocaching.enumerations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing waypoint types
 *  
 * @author koem
 */
public enum WaypointType {
    FLAG      ("flag"),
    OWN       ("own"),
    PKG       ("pkg"),
    PUZZLE    ("puzzle"),
    STAGE     ("stage"),
    TRAILHEAD ("trailhead"),
    WAYPOINT  ("waypoint");

    public final String cgeoId;

    private WaypointType(String cgeoId) {
        this.cgeoId = cgeoId;
    }
    
    public static final Map<String, WaypointType> FIND_BY_CGEOID;
    static {
        final HashMap<String, WaypointType> mapping = new HashMap<String, WaypointType>();
        for (WaypointType wt : values()) {
            mapping.put(wt.cgeoId, wt);
        }
        FIND_BY_CGEOID = Collections.unmodifiableMap(mapping);
    }

}
