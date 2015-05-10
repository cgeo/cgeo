package cgeo.geocaching.files;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

class ImportLocAttachmentThread extends AbstractImportThread {
    private final Uri uri;
    private final ContentResolver contentResolver;

    public ImportLocAttachmentThread(final Uri uri, final ContentResolver contentResolver, final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

    @Override
    protected Collection<Geocache> doImport() throws IOException, ParserException {
        Log.i("Import LOC from uri: " + uri);
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, -1));
        final InputStream is = contentResolver.openInputStream(uri);
        final LocParser parser = new LocParser(listId);
        try {
            return parser.parse(is, progressHandler);
        } finally {
            is.close();
        }
    }
}