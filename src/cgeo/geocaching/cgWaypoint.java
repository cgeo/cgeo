package cgeo.geocaching;

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
}
