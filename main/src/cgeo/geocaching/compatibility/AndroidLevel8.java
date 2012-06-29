package cgeo.geocaching.compatibility;

import cgeo.geocaching.utils.Log;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.backup.BackupManager;
import android.view.Display;

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
}
