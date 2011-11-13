package cgeo.geocaching.maps.google;

import cgeo.geocaching.R;
import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class GoogleOtherCachersOverlayItem extends OverlayItem implements OtherCachersOverlayItemImpl {
    private Context context = null;
    private Go4CacheUser user = null;

    public GoogleOtherCachersOverlayItem(Context contextIn, Go4CacheUser userIn) {
        super(new GeoPoint(userIn.getCoords().getLatitudeE6(), userIn.getCoords().getLongitudeE6()), userIn.getUsername(), "");

        context = contextIn;
        user = userIn;
    }

    @Override
    public Drawable getMarker(int state) {
        Drawable marker = null;

        if (user != null && user.getDate() != null && user.getDate().getTime() >= (System.currentTimeMillis() - (20 * 60 * 1000))) {
            marker = context.getResources().getDrawable(R.drawable.user_location_active);
        } else {
            marker = context.getResources().getDrawable(R.drawable.user_location);
        }

        marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        marker.setAlpha(190);
        setMarker(marker);

        return marker;
    }

    public Go4CacheUser getUser() {
        return user;
    }
}
