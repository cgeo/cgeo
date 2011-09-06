package cgeo.geocaching.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import menion.android.locus.addon.publiclib.DisplayData;
import menion.android.locus.addon.publiclib.geoData.Point;
import menion.android.locus.addon.publiclib.geoData.PointGeocachingData;
import menion.android.locus.addon.publiclib.geoData.PointsData;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import cgeo.geocaching.R;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgWaypoint;

public abstract class AbstractLocusApp extends AbstractApp {
	private static final String INTENT = Intent.ACTION_VIEW;
	private final Resources res;
	
	protected AbstractLocusApp(final Resources res) {
		super(res.getString(R.string.caches_map_locus), INTENT);
		this.res = res;
	}

	@Override
	public boolean isInstalled(final Context context) {
		final Intent intentTest = new Intent(INTENT);
		intentTest.setData(Uri.parse("menion.points:x"));
		return isIntentAvailable(context, intentTest);
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

        // An icon can only be set for a group of Locus-points. For grouping we use a Map.
        // As a key we use an IconType object which holds all information to construct and
        // distinguish between different icons.
//        Map<IconType, PointsData> pointsGroups = new HashMap<IconType, PointsData>();

//        int gc = 0; // counter for groups of points
        int pc = 0; // counter for points
PointsData pd = new PointsData("c:geo");
        for (Object o : objectsToShow) {
            // get icon and Point
//            IconType it = null;
            Point p = null;
PointGeocachingData pg = new PointGeocachingData();
            if (o instanceof cgCache) {
                cgCache c = (cgCache) o;
//                it = new IconType(true, c.type, c.own, c.found, c.disabled);
                p = this.getPoint(c);
pg.name=c.name;
pg.available=!c.disabled;
pg.cacheID=c.geocode;
pg.type = (int) (Math.random() * 13);
p.setGeocachingData(pg);
            } else if (o instanceof cgWaypoint) {
                cgWaypoint w = (cgWaypoint) o;
//                it = new IconType(false, w.type, false, false, false);
//                p = this.getPoint(w);
            } else {
                continue; // no cache, no waypoint => ignore
            }
            if (p == null) continue;

//            // get / create corresponding group of points 
//            PointsData pd = pointsGroups.get(it);
//            if (pd == null) {
//                pd = new PointsData("c:geo objects " + ++gc); // every pd must have a unique name
//                pd.setBitmap(getIcon(it));
//                pointsGroups.put(it, pd);
//            }
            pd.addPoint(p);
            ++pc;
        }

        if (pc < 1000) {
DisplayData.sendData(activity, pd, false);
//            DisplayData.sendData(activity, new ArrayList<PointsData>(pointsGroups.values()), false);
        } else {
        	ArrayList<PointsData> data = new ArrayList<PointsData>();
        	data.add(pd);
DisplayData.sendDataCursor(activity, data,
        "content://" + LocusDataStorageProvider.class.getCanonicalName().toLowerCase(), 
        false);
//			DisplayData.sendDataCursor(activity, new ArrayList<PointsData>(pointsGroups.values()),
//			        "content://" + LocusDataStorageProvider.class.getCanonicalName().toLowerCase(), 
//			        false);
        }
	}

	/**
	 * This method constructs a <code>Point</code> for displaying in Locus
	 * 
	 * @param cache
	 * @return  null, when the <code>Point</code> could not be constructed
	 * @author koem
	 */
	private Point getPoint(cgCache cache) {
		if (cache == null) return null;

		// create one simple point with location
		Location loc = new Location(cgSettings.tag);
		loc.setLatitude(cache.latitude);
		loc.setLongitude(cache.longitude);

		Point p = new Point(cache.name, loc);
//		p.setDescription("<a href=\"http://coord.info/" +cache.geocode + "\">" 
//					+ cache.geocode + "</a>");
    	
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
	
	/**
	 * caching mechanism for icons
     * 
     * @author koem
	 */
	private Bitmap getIcon(IconType it) {
		Bitmap icon = iconCache.get(it);
		if (icon != null) return icon;

		synchronized(this) {
			// load icon
			final int iconId = cgBase.getMarkerIcon(it.isCache, it.type, it.own, it.found, 
			        it.disabled);
			if (iconId > 0) icon = BitmapFactory.decodeResource(res, iconId);
			iconCache.put(it, icon);
			return icon;
		}
	}
	
	private static Map<IconType, Bitmap> iconCache = new HashMap<IconType, Bitmap>();
	
	/**
	 * class representing a key in the iconCache
	 * 
	 * @author koem
	 */
	private static class IconType {
		private final boolean isCache;
		private final String type;
		private final boolean own, found, disabled;
		private int hashCode = 0;
		public IconType(boolean isCache, String type, boolean own, boolean found, 
				boolean disabled) {
			this.isCache = isCache;
			this.type = type;
			this.own = own;
			this.found = found;
			this.disabled = disabled;
		}
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof IconType)) return false;
			return this.hashCode() == ((IconType) o).hashCode();
		}
		@Override
		public int hashCode() {
			if (hashCode == 0) hashCode = (isCache + type + own + found + disabled).hashCode();
			return hashCode;
		}
	}
}
