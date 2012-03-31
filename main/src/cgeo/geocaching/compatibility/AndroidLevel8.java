package cgeo.geocaching.compatibility;

import cgeo.geocaching.Settings;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.view.Display;

public class AndroidLevel8 implements AndroidLevel8Interface {

    public int getRotation(final Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    public void dataChanged(final String name) {
        Log.i(Settings.tag, "Requesting settings backup with settings manager");
        BackupManager.dataChanged(name);
    }
}
