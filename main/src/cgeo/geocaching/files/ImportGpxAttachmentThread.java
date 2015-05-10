package cgeo.geocaching.files;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.eclipse.jdt.annotation.Nullable;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public class ImportGpxAttachmentThread extends AbstractImportGpxThread {
    private final Uri uri;
    private final ContentResolver contentResolver;

    public ImportGpxAttachmentThread(final Uri uri, final ContentResolver contentResolver, final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
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
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, streamSize));
        try {
            return parser.parse(inputStream, progressHandler);
        } finally {
            inputStream.close();
        }
    }

    @Nullable
    private InputStream getStream() {
        try {
            return contentResolver.openInputStream(uri);
        } catch (final FileNotFoundException e) {
            // for http links, we may need to download the content ourself, if it has no mime type announced by the browser
            if (uri.toString().startsWith("http")) {
                return Network.getResponseStream(Network.getRequest(uri.toString()));
            }
            // only log error for non-http URI
            Log.e("GPX import cannot resolve " + uri);
        }
        return null;
    }
}