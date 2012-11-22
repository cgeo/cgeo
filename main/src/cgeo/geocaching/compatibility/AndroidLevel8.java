package cgeo.geocaching.compatibility;

import cgeo.geocaching.utils.Log;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.view.Display;
import android.view.Surface;

@TargetApi(8)
public class AndroidLevel8 implements AndroidLevel8Interface {

    @Override
    public int getRotation(final Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    @Override
    public void dataChanged(final String name) {
        Log.i("Requesting settings backup with settings manager");
        BackupManager.dataChanged(name);
    }

    @Override
    public int getRotationOffset(final Activity activity) {
        try {
            final int rotation = getRotation(activity);
            if (rotation == Surface.ROTATION_90) {
                return 90;
            } else if (rotation == Surface.ROTATION_180) {
                return 180;
            } else if (rotation == Surface.ROTATION_270) {
                return 270;
            }
        } catch (final Exception e) {
            // This should never happen: IllegalArgumentException, IllegalAccessException or InvocationTargetException
            Log.e("Cannot call getRotation()", e);
        }

        return 0;
    }
}
