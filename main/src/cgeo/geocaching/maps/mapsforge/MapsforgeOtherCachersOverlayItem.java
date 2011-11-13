package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.R;
import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class MapsforgeOtherCachersOverlayItem extends OverlayItem implements OtherCachersOverlayItemImpl {
    private Context context = null;
    private Go4CacheUser user = null;

    public MapsforgeOtherCachersOverlayItem(Context contextIn, Go4CacheUser userIn) {
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
