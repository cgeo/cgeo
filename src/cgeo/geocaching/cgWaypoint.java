package cgeo.geocaching;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

public class cgWaypoint {
    public Integer id = 0;
	public String geocode = "geocode";
	public String type = "waypoint";
	public String prefix = "";
	public String lookup = "";
	public String name = "";
	public String latlon = "";
	public String latitudeString = "";
	public String longitudeString = "";
	public Double latitude = null;
	public Double longitude = null;
	public String note = "";

	public void setIcon(Resources res, cgBase base, TextView nameView) {
		int iconId = R.drawable.waypoint_waypoint;
		if (type != null) {
			int specialId = res.getIdentifier("waypoint_" + type, "drawable", base.context.getPackageName());
			if (specialId > 0) {
				iconId = specialId;
			}
		}
		nameView.setCompoundDrawablesWithIntrinsicBounds((Drawable) res.getDrawable(iconId), null, null, null);
	}

	public void merge(final cgWaypoint old) {
		if (prefix == null || prefix.length() == 0) {
			prefix = old.prefix;
		}
		if (lookup == null || lookup.length() == 0) {
			lookup = old.lookup;
		}
		if (name == null || name.length() == 0) {
			name = old.name;
		}
		if (latlon == null || latlon.length() == 0) {
			latlon = old.latlon;
		}
		if (latitudeString == null || latitudeString.length() == 0) {
			latitudeString = old.latitudeString;
		}
		if (longitudeString == null || longitudeString.length() == 0) {
			longitudeString = old.longitudeString;
		}
		if (latitude == null) {
			latitude = old.latitude;
		}
		if (longitude == null) {
			longitude = old.longitude;
		}
		if (note == null || note.length() == 0) {
			note = old.note;
		}
		if (note != null && old.note != null) {
			if (old.note.length() > note.length()) {
				note = old.note;
			}
		}
	}

	public static void mergeWayPoints(ArrayList<cgWaypoint> newPoints,
			ArrayList<cgWaypoint> oldPoints) {
		// copy user modified details of the waypoints
		if (newPoints != null && oldPoints != null) {
			for (cgWaypoint old : oldPoints) {
				boolean merged = false;
				if (old != null && old.name != null && old.name.length() > 0) {
					for (cgWaypoint waypoint : newPoints) {
						if (waypoint != null && waypoint.name != null) {
							if (old.name.equalsIgnoreCase(waypoint.name)) {
								waypoint.merge(old);
								merged = true;
								break;
							}
						}
					}
				}
				// user added waypoints should also be in the new list
				if (!merged) {
					newPoints.add(old);
				}
			}
		}
	}

	public boolean isUserDefined() {
		return type != null && type.equalsIgnoreCase("own");
	}
}