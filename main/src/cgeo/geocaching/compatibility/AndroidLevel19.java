package cgeo.geocaching.compatibility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;

@TargetApi(19)
class AndroidLevel19 implements AndroidLevel19Interface {

    @Override
    public void importGpxFromStorageAccessFramework(final Activity activity, final int requestCode) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a file (as opposed to a list
        // of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Mime type based filter, we use "*/*" as GPX does not have a good mime type anyway
        intent.setType("*/*");

        activity.startActivityForResult(intent, requestCode);
    }

}
