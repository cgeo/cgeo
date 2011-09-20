package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.view.Display;

public class AndroidLevel8 {

    public int getRotation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    public void dataChanged(final String name) {
        BackupManager.dataChanged(name);
    }
}
