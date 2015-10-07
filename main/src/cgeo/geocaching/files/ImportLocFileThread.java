package cgeo.geocaching.files;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

class ImportLocFileThread extends AbstractImportThread {
    private final File file;

    public ImportLocFileThread(final File file, final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.file = file;
    }

    @Override
    protected Collection<Geocache> doImport() throws IOException, ParserException {
        Log.i("Import LOC file: " + file.getAbsolutePath());
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches, (int) file.length()));
        final LocParser parser = new LocParser(listId);
        return parser.parse(file, progressHandler);
    }
}