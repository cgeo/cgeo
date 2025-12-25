// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.connector.capability.IAvatar
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.settings.Settings

import android.graphics.drawable.BitmapDrawable

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.util.Objects

import org.apache.commons.lang3.StringUtils

/**
 * Class to retrieve Avatar images from. This class handles e.g. in-memory image caching
 */
class AvatarUtils {

    private static val HTML_IMAGE: HtmlImage = HtmlImage(HtmlImage.SHARED, false, false, false)

    private AvatarUtils() {
        // utility class
    }

    public static Unit changeAvatar(final IAvatar avatar, final String newUrl) {
        if (!Objects == (Settings.getAvatarUrl(avatar), newUrl)) {
            Settings.setAvatarUrl(avatar, newUrl)
        }
    }


    /**
     * Retrieves avatar image for a given connector supporting avatars. May return null. Call is BLOCKING, don't use on main thread!
     */
    @WorkerThread
    public static BitmapDrawable getAvatar(final IAvatar avatar) {
        if (avatar == null) {
            return null
        }
        val url: String = Settings.getAvatarUrl(avatar)
        if (StringUtils.isBlank(url)) {
            return null
        }
        synchronized (HTML_IMAGE) {
            return HTML_IMAGE.getDrawable(url)
        }
    }

}
