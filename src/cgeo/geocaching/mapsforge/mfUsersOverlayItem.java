package cgeo.geocaching.mapsforge;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;
import cgeo.geocaching.R;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.mapinterfaces.UserOverlayItemImpl;

public class mfUsersOverlayItem extends OverlayItem implements UserOverlayItemImpl {
	private Context context = null;
	private cgUser user = null;

	public mfUsersOverlayItem(Context contextIn, cgUser userIn) {
		super(new GeoPoint((int)(userIn.coords.getLatitudeE6()), userIn.coords.getLongitudeE6()), userIn.username, "");

		context = contextIn;
		user = userIn;
	}

	@Override
	public Drawable getMarker(int state) {
		Drawable marker = null;
		
		if (user != null && user.located != null && user.located.getTime() >= (System.currentTimeMillis() - (20 * 60 * 1000))) {
			marker = context.getResources().getDrawable(R.drawable.user_location_active);
		} else {
			marker = context.getResources().getDrawable(R.drawable.user_location);
		}

		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		marker.setAlpha(190);
		setMarker(marker);

		return marker;
	}

	public cgUser getUser() {
		return user;
	}
}
