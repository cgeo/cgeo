package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.go4cache.Go4CacheUser;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class OtherCachersOverlayItem {
    private final Context context;
    private final Go4CacheUser user;

    public OtherCachersOverlayItem(Context contextIn, Go4CacheUser userIn) {
        context = contextIn;
        user = userIn;
    }

    public Drawable getMarker() {
        Drawable marker;

        if (user != null && user.getDate() != null && user.getDate().getTime() >= (System.currentTimeMillis() - (20 * 60 * 1000))) {
            marker = context.getResources().getDrawable(R.drawable.user_location_active);
        } else {
            marker = context.getResources().getDrawable(R.drawable.user_location);
        }

        marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        marker.setAlpha(190);

        return marker;
    }

    public Go4CacheUser getUser() {
        return user;
    }
}