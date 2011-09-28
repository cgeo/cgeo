package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.view.Display;

public class AndroidLevel8 {

    @SuppressWarnings("static-method")
    public int getRotation(final Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        return display.getRotation();
    }

    @SuppressWarnings("static-method")
    public void dataChanged(final String name) {
        BackupManager.dataChanged(name);
    }
}
