package cgeo.geocaching.compatibility;

import android.app.Activity;

public class AndroidLevel19Emulation implements AndroidLevel19Interface {

    @Override
    public void importGpxFromStorageAccessFramework(Activity activity, int requestCode) {
        // do nothing
    }

}
