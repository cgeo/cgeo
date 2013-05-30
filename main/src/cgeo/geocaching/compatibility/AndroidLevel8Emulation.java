package cgeo.geocaching.compatibility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Environment;
import android.view.Display;

import java.io.File;

@TargetApi(value = 7)
public class AndroidLevel8Emulation implements AndroidLevel8Interface {

    @Override
    public void dataChanged(final String name) {
        // do nothing
    }

    @Override
    public int getRotationOffset(Activity activity) {
        final Display display = activity.getWindowManager().getDefaultDisplay();

        // the non deprecated method is available in API 8+ only, so we cannot deal better with this
        @SuppressWarnings("deprecation")
        final int rotation = display.getOrientation();

        if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
            return 90;
        }
        return 0;
    }

    @Override
    public File getExternalPictureDir() {
        // Use externalStorage/Pictures as default
        return new File(Environment.getExternalStorageDirectory(), "Pictures");
    }
}
