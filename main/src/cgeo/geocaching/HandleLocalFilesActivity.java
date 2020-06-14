package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.files.FileType;
import cgeo.geocaching.files.FileTypeDetector;
import cgeo.geocaching.settings.ReceiveMapFileActivity;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import static cgeo.geocaching.files.GPXImporter.COMPRESSED_GPX_FILE_EXTENSION;
import static cgeo.geocaching.files.GPXImporter.GPX_FILE_EXTENSION;
import static cgeo.geocaching.files.GPXImporter.ZIP_FILE_EXTENSION;

public class HandleLocalFilesActivity extends AbstractActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final Uri uri = intent.getData();
        boolean finished = false;

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            final String parsedUri = uri.getPath();
            if (parsedUri != null) {
                final int startExtension = parsedUri.lastIndexOf('.');
                if (startExtension > -1) {
                    final String extension = parsedUri.substring(startExtension).toLowerCase();
                    if (extension.equals(ZIP_FILE_EXTENSION) || extension.equals(GPX_FILE_EXTENSION) || extension.equals(COMPRESSED_GPX_FILE_EXTENSION)) {
                        continueWith(CacheListActivity.class, intent);
                        finished = true;
                    } else if (extension.equals(".map")) {
                        continueWith(ReceiveMapFileActivity.class, intent);
                        finished = true;
                    }
                }
            }
        }
        if (!finished) {
            final ContentResolver contentResolver = getContentResolver();
            final FileType fileType = new FileTypeDetector(uri, contentResolver).getFileType();
            switch (fileType) {
                case GPX:
                case ZIP:
                case LOC:
                    continueWith(CacheListActivity.class, intent);
                    finished = true;
                    break;
                case MAP:
                    continueWith(ReceiveMapFileActivity.class, intent);
                    finished = true;
                    break;
                default:
                    break;
            }
        }
        if (!finished) {
            Dialogs.message(this, R.string.localfile_title, R.string.localfile_cannot_handle, (dialog, button) -> finish());
        }
    }

    private void continueWith(final Class clazz, final Intent intent) {
        final Intent forwarder = new Intent(intent);
        forwarder.setClass(this, clazz);
        startActivity(forwarder);
        finish();
    }

    /*
    private void continueWithExternal(final String component, final String clazz, final Intent intent) {
        final Intent forwarder = new Intent(intent);
        forwarder.setComponent(new ComponentName(component, clazz));
        forwarder.setAction(clazz);
        startActivity(forwarder);
        finish();
    }
    */

}
