package cgeo.geocaching.compatibility;

import cgeo.geocaching.utils.Log;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.os.Environment;
import android.view.Surface;

import java.io.File;

@TargetApi(8)
public class AndroidLevel8 implements AndroidLevel8Interface {

    @Override
    public void dataChanged(final String name) {
        Log.i("Requesting settings backup with settings manager");
        BackupManager.dataChanged(name);
    }

    @Override
    public int getRotationOffset(final Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    @Override
    public File getExternalPictureDir() {
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
    }
}
