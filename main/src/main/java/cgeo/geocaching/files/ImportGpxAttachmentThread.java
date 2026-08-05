package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.compress.utils.IOUtils;

public class ImportGpxAttachmentThread extends AbstractImportGpxThread {
    private final Uri uri;
    private final ContentResolver contentResolver;

    public ImportGpxAttachmentThread(final Uri uri, final ContentResolver contentResolver, final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

    @Override
    protected Collection<Geocache> doImport(final GPXParser parser) throws IOException, ParserException {
        Log.i("Import GPX from uri: " + uri);
        final InputStream inputStream = getStream();
        if (inputStream == null) {
            return Collections.emptyList();
        }
        int streamSize = inputStream.available();
        if (streamSize == 0) {
            streamSize = -1;
        }
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, streamSize, getSourceDisplayName()));
        try {
            return parser.parse(inputStream, progressHandler);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Nullable
    @WorkerThread
    private InputStream getStream() {
        try {
            return contentResolver.openInputStream(uri);
        } catch (final FileNotFoundException e) {
            // for http links, we may need to download the content ourselves, if it has no mime type announced by the browser
            if (uri.toString().startsWith("http")) {
                return Network.getResponseStream(Network.getRequest(uri.toString()));
            }
            // only log error for non-http URI
            Log.e("GPX import cannot resolve " + uri);
        }
        return null;
    }

    @Override
    protected String getSourceDisplayName() {
        return uri.getLastPathSegment();
    }
}
