package cgeo.geocaching.apps.cache.navi;

import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.AbstractLocusApp;

class LocusApp extends AbstractLocusApp implements NavigationApp {

	LocusApp(Resources res) {
		super(res);
	}

	/**
	 * Show a single cache with waypoints or a single waypoint in Locus.
	 * This method constructs a list of cache and waypoints only. 
	 * 
	 * @see AbstractLocusApp#showInLocus
	 * @author koem
	 */
	@Override
	public boolean invoke(cgGeo geo, Activity activity, Resources res, cgCache cache,
			final UUID searchId, cgWaypoint waypoint, Double latitude, Double longitude) {
		
		if (cache == null && waypoint == null && latitude == null && longitude == null) {
			return false;
		}

		if (isInstalled(activity)) { // TODO: is this if-statement really necessary?
			final ArrayList<Object> points = new ArrayList<Object>();

			// add cache if present
			if (cache != null) {
				if (cache.longitude == null || cache.latitude == null) {
					cache.longitude = longitude;
					cache.latitude = latitude;
				}
				if (cache.longitude != null && cache.latitude != null) {
					points.add(cache);
				}

//				// use only waypoints with coordinates
//				if (cache.waypoints != null) {
//					for (cgWaypoint wp : cache.waypoints) {
//						if (wp.latitude != null && wp.longitude != null) points.add(wp);
//					}
//				}
			}

			// add waypoint if present
			if (waypoint != null) {
				if (waypoint.longitude == null || waypoint.latitude == null) {
					waypoint.longitude = longitude;
					waypoint.latitude = latitude;
				}
				if (waypoint.longitude != null && waypoint.latitude != null) {
					points.add(waypoint);
				}
			}

			this.showInLocus(points, activity);
			
			return true;
		}

		return false;
	}

}
