package cgeo.geocaching.compatibility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.view.Display;

@TargetApi(value = 7)
public class AndroidLevel8Emulation implements AndroidLevel8Interface {

    @Override
    public int getRotation(final Activity activity) {
        return 0;
    }

    @Override
    public void dataChanged(final String name) {
        // do nothing
    }

    @Override
    public int getRotationOffset(Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();
        final int rotation = display.getOrientation();
        if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
            return 90;
        }
        return 0;
    }
}
