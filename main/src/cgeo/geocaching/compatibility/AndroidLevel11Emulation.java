package cgeo.geocaching.compatibility;

import android.app.Activity;

/**
 * implement level 11 API using older methods
 */
public class AndroidLevel11Emulation implements AndroidLevel11Interface {

    @Override
    public void invalidateOptionsMenu(final Activity activity) {
        // do nothing
    }

}
