package cgeo.geocaching.maps.mapsforge.v024;

import cgeo.geocaching.go4cache.Go4CacheUser;
import cgeo.geocaching.maps.OtherCachersOverlayItem;
import cgeo.geocaching.maps.interfaces.OtherCachersOverlayItemImpl;

import org.mapsforge.android.mapsold.GeoPoint;
import org.mapsforge.android.mapsold.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class MapsforgeOtherCachersOverlayItem extends OverlayItem implements OtherCachersOverlayItemImpl {
    private final OtherCachersOverlayItem item;

    public MapsforgeOtherCachersOverlayItem(Context context, Go4CacheUser user) {
        super(new GeoPoint(user.getCoords().getLatitudeE6(), user.getCoords().getLongitudeE6()), user.getUsername(), "");
        item = new OtherCachersOverlayItem(context, user);
    }

    @Override
    public Drawable getMarker(int state) {
        Drawable marker = item.getMarker();
        setMarker(marker);
        return marker;
    }

    @Override
    public Go4CacheUser getUser() {
        return item.getUser();
    }
}
