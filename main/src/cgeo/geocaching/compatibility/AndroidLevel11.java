package cgeo.geocaching.compatibility;

import android.app.Activity;

/**
 * Android level 11 support
 * 
 * @author bananeweizen
 * 
 */
public class AndroidLevel11 implements AndroidLevel11Interface {

    @Override
    public void invalidateOptionsMenu(final Activity activity) {
        activity.invalidateOptionsMenu();
    }

}
