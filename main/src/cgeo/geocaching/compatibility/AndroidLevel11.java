package cgeo.geocaching.compatibility;

import android.annotation.TargetApi;
import android.app.Activity;

/**
 * Android level 11 support
 */
@TargetApi(11)
public class AndroidLevel11 implements AndroidLevel11Interface {

    @Override
    public void invalidateOptionsMenu(final Activity activity) {
        activity.invalidateOptionsMenu();
    }

}
