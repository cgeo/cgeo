package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.downloader.ReceiveDownloadService;
import cgeo.geocaching.files.FileType;
import cgeo.geocaching.files.FileTypeDetector;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

public class HandleLocalFilesActivity extends AbstractActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final Uri uri = intent.getData();
        boolean finished = false;

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
                continueWithForegroundService(ReceiveDownloadService.class, intent);
                finished = true;
                break;
            default:
                break;
        }
        if (!finished) {
            SimpleDialog.of(this).setTitle(R.string.localfile_title).setMessage(R.string.localfile_cannot_handle).show((dialog, button) -> finish());
        }
    }

    private void continueWith(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        final Intent forwarder = new Intent(intent);
        forwarder.setClass(this, clazz);
        startActivity(forwarder);
        finish();
    }

    private void continueWithForegroundService(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        final Intent forwarder = new Intent(intent);
        forwarder.setClass(this, clazz);
        ContextCompat.startForegroundService(this, forwarder);
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
