package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.downloader.ReceiveDownloadService;
import cgeo.geocaching.files.FileType;
import cgeo.geocaching.files.FileTypeDetector;
import cgeo.geocaching.files.GPXMultiParser;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.wherigo.WherigoActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParserException;

public class HandleLocalFilesActivity extends AbstractActivity {

    private static final String LOGPRAEFIX = "HandleLocalFilesActivity: ";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
        final Uri uri = intent.getData();
        boolean finished = false;

        Log.iForce(LOGPRAEFIX + "called for URI:" + uri);

        final ContentResolver contentResolver = getContentResolver();
        final FileType fileType = new FileTypeDetector(uri, contentResolver).getFileType();
        switch (fileType) {
            case GPX:
                // sample code for GPXMultiParser, which parses geocaches, tracks and routes in parallel
                try (InputStream in = new BufferedInputStream(ContentStorage.get().openForRead(uri))) {
                    final Collection<Object> result = new GPXMultiParser().doParsing(in, StoredList.STANDARD_LIST_ID); // todo: listId depends on context
                    Log.e("returned from parsing, size=" + result.size());
                } catch (IOException | XmlPullParserException e) {
                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    Log.e("parsing exception: " + e.getMessage() + "\n" + sw);
                }
                // depending on result different actions can be done
                // (either automatically or after asking the user):
                // - single cache: import to list + open cache
                // - multiple caches: import to list + open list
                // - track: add to viewed tracks on map
                // - route: overwrite individual route (after confirmation)
                finished = true;
                break;
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
        Log.iForce(LOGPRAEFIX + "continueWith:" + clazz);
        final Intent forwarder = new Intent(intent);
        forwarder.setClass(this, clazz);
        startActivity(forwarder);
        finish();
    }

    private void continueWithForegroundService(@SuppressWarnings("rawtypes") final Class clazz, final Intent intent) {
        Log.iForce(LOGPRAEFIX + "continueWithForegroundService:" + clazz);
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
        String filename = ContentStorage.get().getName(uri);
        Log.iForce(LOGPRAEFIX + "expect Wherigo, raw filename:" + filename);
        if (StringUtils.isBlank(filename)) {
            filename = FileNameCreator.WHERIGO.createName();
        }
        filename = filename.replace("_", "-");
        if (StringUtils.equalsAnyIgnoreCase(filename.substring(filename.length() - 4), ".gwc")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        Log.iForce(LOGPRAEFIX + "Wherigo final filename:" + filename);
        ContentStorage.get().copy(uri, PersistableFolder.WHERIGO.getFolder(), FileNameCreator.forName(filename + ".gwc"), false);
        return filename;
    }
}
