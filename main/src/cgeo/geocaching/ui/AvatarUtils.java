package cgeo.geocaching.ui;

import cgeo.geocaching.connector.capability.IAvatar;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.settings.Settings;

import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Class to retrieve Avatar images from. This class handles e.g. in-memory image caching */
public class AvatarUtils {

    private static final HtmlImage HTML_IMAGE = new HtmlImage(HtmlImage.SHARED, false, false, false);

    private AvatarUtils() {
        // utility class
    }

    public static void changeAvatar(@NonNull final IAvatar avatar, final String newUrl) {
        Settings.setAvatarUrl(avatar, newUrl);
    }


    /**
     * Retrieves avatar image for a given connector supporting avatars. May return null. Call is BLOCKING, don't use on main thread!
     */
    public static BitmapDrawable getAvatar(@Nullable final IAvatar avatar) {
        if (avatar == null) {
            return null;
        }
        final String url = Settings.getAvatarUrl(avatar);
        if (url == null) {
            return null;
        }
        synchronized (HTML_IMAGE) {
            return HTML_IMAGE.getDrawable(url);
        }
    }

}
