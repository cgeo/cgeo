package cgeo.geocaching.apps;

import java.util.ArrayList;
import java.util.List;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.LocusUtils;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingDataWaypoint;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import cgeo.geocaching.R;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;

public abstract class AbstractLocusApp extends AbstractApp {
	private static final String INTENT = Intent.ACTION_VIEW;
	
	protected AbstractLocusApp(final Resources res) {
		super(res.getString(R.string.caches_map_locus), INTENT);
	}

	@Override
	public boolean isInstalled(Context context) {
		return LocusUtils.isLocusAvailable(context);
	}

	/**
	 * Display a list of caches / waypoints in Locus
	 * 
	 * @param objectsToShow
	 * @param activity
	 * @author koem
	 */
	protected void showInLocus(List<? extends Object> objectsToShow, Activity activity) {
        if (objectsToShow == null || activity == null) return;

        int pc = 0; // counter for points
        PointsData pd = new PointsData("c:geo");
        for (Object o : objectsToShow) {
            // get icon and Point
            Point p = null;
            if (o instanceof cgCache) {
                cgCache c = (cgCache) o;
                p = this.getPoint(c, activity);
            } else if (o instanceof cgWaypoint) {
                cgWaypoint w = (cgWaypoint) o;
                p = this.getPoint(w);
            } else {
                continue; // no cache, no waypoint => ignore
            }
            if (p == null) continue;

            pd.addPoint(p);
            ++pc;
        }

        if (pc <= 1000) {
        	DisplayData.sendData(activity, pd, false);
        } else {
        	ArrayList<PointsData> data = new ArrayList<PointsData>();
        	data.add(pd);
			DisplayData.sendDataCursor(activity, data,
			        "content://" + LocusDataStorageProvider.class.getCanonicalName().toLowerCase(), 
			        false);
        }
	}

	/**
	 * This method constructs a <code>Point</code> for displaying in Locus
	 * 
	 * @param cache
	 * @return  null, when the <code>Point</code> could not be constructed
	 * @author koem
	 */
	private Point getPoint(cgCache cache, Context context) {
		if (cache == null) return null;

		// create one simple point with location
		Location loc = new Location(cgSettings.tag);
		loc.setLatitude(cache.latitude);
		loc.setLongitude(cache.longitude);

		Point p = new Point(cache.name, loc);
		PointGeocachingData pg = new PointGeocachingData();
		p.setGeocachingData(pg);
		
		pg.cacheID = cache.geocode;
		pg.available = ! cache.disabled;
		pg.archived = cache.archived;
		pg.premiumOnly = cache.members;
		pg.name = cache.name;
		pg.placedBy = cache.owner;
		if (cache.hidden != null) pg.hidden = cgBase.formatFullDate(context, cache.hidden.getTime());
		for (CacheType ct : CacheType.values()) {
		    if (ct.cgeoId.equals(cache.type)) {
		        if (ct.locusId != CacheType.NO_LOCUS_ID) pg.type = ct.locusId;
		        break;
		    }
		}
		for (CacheSize cs : CacheSize.values()) {
		    if (cs.cgeoId.equals(cache.size)) {
		        pg.container = cs.locusId;
		        break;
		    }
		}
		if (cache.difficulty != null) pg.difficulty = cache.difficulty;
		if (cache.terrain != null) pg.terrain = cache.terrain;
		pg.shortDescription = cache.shortdesc;
		pg.longDescription = cache.description;
		pg.encodedHints = cache.hint;
		if (cache.waypoints != null) {
			pg.waypoints = new ArrayList<PointGeocachingDataWaypoint>();
			for (cgWaypoint waypoint : cache.waypoints) {
				if (waypoint == null) continue;
				PointGeocachingDataWaypoint w = new PointGeocachingDataWaypoint();
				w.code = waypoint.geocode;
				w.name = waypoint.name;
				w.type = PointGeocachingData.CACHE_WAYPOINT_TYPE_STAGES;
				w.lat = waypoint.latitude;
				w.lon = waypoint.longitude;
				pg.waypoints.add(w);
			}
		}
		pg.found = cache.found;

    	return p;
	}

	/**
     * This method constructs a <code>Point</code> for displaying in Locus
     * 
     * @param waypoint
     * @return  null, when the <code>Point</code> could not be constructed
     * @author koem
     */
    private Point getPoint(cgWaypoint waypoint) {
        if (waypoint == null) return null;

        // create one simple point with location
        Location loc = new Location(cgSettings.tag);
        loc.setLatitude(waypoint.latitude);
        loc.setLongitude(waypoint.longitude);

        Point p = new Point(waypoint.name, loc);
        p.setDescription("<a href=\"http://coord.info/" + waypoint.geocode + "\">" 
                    + waypoint.geocode + "</a>");

        return p;
    }
}
