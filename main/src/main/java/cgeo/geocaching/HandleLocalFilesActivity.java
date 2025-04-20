package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.downloader.ReceiveDownloadService;
import cgeo.geocaching.files.FileType;
import cgeo.geocaching.files.FileTypeDetector;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.wherigo.WherigoActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;

public class HandleLocalFilesActivity extends AbstractActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
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
            case WHERIGO:
                final String guid = copyToWherigoFolder(uri);
                if (guid != null) {
                    WherigoActivity.startForGuid(this, guid, null, false);
                    finish();
                    finished = true;
                }
                break;
            default:
                break;
        }
        if (!finished) {
            SimpleDialog.of(this).setTitle(R.string.localfile_title).setMessage(R.string.localfile_cannot_handle).show(this::finish);
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

    /**
     * copy uri to wherigo folder
     * <br />
     * For "guid calculation" of WherigoActivity,
     * - capitalization of suffix ".gwc" will be normalized to ".gwc"
     * - all "_" will be replaced by "-"
     * <br />
     * returns "guid" (= filename without suffix ".gwc") on success, null otherwise
     */
    @Nullable
    private String copyToWherigoFolder(final Uri uri) {
        String filename = ContentStorage.get().getName(uri).replace("_", "-");
        if (StringUtils.isBlank(filename)) {
            return null;
        }
        if (StringUtils.equalsAnyIgnoreCase(filename.substring(filename.length() - 4), ".gwc")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        ContentStorage.get().copy(uri, PersistableFolder.WHERIGO.getFolder(), FileNameCreator.forName(filename + ".gwc"), false);
        return filename;
    }
}
