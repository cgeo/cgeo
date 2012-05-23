package cgeo.geocaching.compatibility;

import android.app.Activity;

/**
 * dummy class which has no functionality in the level 11 API
 */
public class AndroidLevel11Dummy implements AndroidLevel11Interface {

    @Override
    public void invalidateOptionsMenu(final Activity activity) {
        // do nothing
    }

}
