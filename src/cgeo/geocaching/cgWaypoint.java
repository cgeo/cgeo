package cgeo.geocaching;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

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
		if (StringUtils.isBlank(prefix)) {
			prefix = old.prefix;
		}
		if (StringUtils.isBlank(lookup)) {
			lookup = old.lookup;
		}
		if (StringUtils.isBlank(name)) {
			name = old.name;
		}
		if (StringUtils.isBlank(latlon)) {
			latlon = old.latlon;
		}
		if (StringUtils.isBlank(latitudeString)) {
			latitudeString = old.latitudeString;
		}
		if (StringUtils.isBlank(longitudeString)) {
			longitudeString = old.longitudeString;
		}
		if (latitude == null) {
			latitude = old.latitude;
		}
		if (longitude == null) {
			longitude = old.longitude;
		}
		if (StringUtils.isBlank(note)) {
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