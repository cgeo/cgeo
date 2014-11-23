package cgeo.geocaching.compatibility;

import android.app.Activity;

class AndroidLevel19Emulation implements AndroidLevel19Interface {

    @Override
    public void importGpxFromStorageAccessFramework(final Activity activity, final int requestCode) {
        // do nothing
    }

}
