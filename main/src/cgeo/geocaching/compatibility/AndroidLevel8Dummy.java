package cgeo.geocaching.compatibility;

import android.app.Activity;

public class AndroidLevel8Dummy implements AndroidLevel8Interface {

    @Override
    public int getRotation(final Activity activity) {
        return 0;
    }

    @Override
    public void dataChanged(final String name) {
        // do nothing
    }
}
