package cgeo.geocaching.maps.google;

import android.content.Context;
import android.graphics.drawable.Drawable;
import cgeo.geocaching.R;
import cgeo.geocaching.cgUser;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class googleUsersOverlayItem extends OverlayItem implements OtherCachersOverlayItemImpl {
	private Context context = null;
	private cgUser user = null;

	public googleUsersOverlayItem(Context contextIn, cgUser userIn) {
		super(new GeoPoint((int)(userIn.latitude * 1e6), (int)(userIn.longitude * 1e6)), userIn.username, "");

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
