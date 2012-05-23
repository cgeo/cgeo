package cgeo.geocaching.maps.google;

import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.OtherCachersOverlayItem;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class GoogleOtherCachersOverlayItem extends OverlayItem implements OtherCachersOverlayItemImpl {
    final private OtherCachersOverlayItem item;

    public GoogleOtherCachersOverlayItem(Context context, Go4CacheUser user) {
        super(new GeoPoint(user.getCoords().getLatitudeE6(), user.getCoords().getLongitudeE6()), user.getUsername(), "");
        item = new OtherCachersOverlayItem(context, user);
    }

    @Override
    public Drawable getMarker(int state) {
        final Drawable marker = item.getMarker();
        setMarker(marker);
        return marker;
    }

    @Override
    public Go4CacheUser getUser() {
        return item.getUser();
    }
}
