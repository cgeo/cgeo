package cgeo.geocaching.compatibility;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public final class Compatibility {

    private Compatibility() {
        // utility class
    }

    @SuppressWarnings("deprecation")
    // the non replacement method is only available on level 21, therefore we ignore this deprecation
    public static Drawable getDrawable(final Resources resources, final int markerId) {
        return resources.getDrawable(markerId);
    }
}
