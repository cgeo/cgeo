package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

class ImportLocFileThread extends AbstractImportThread {
    private final File file;

    ImportLocFileThread(final File file, final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.file = file;
    }

    @Override
    protected Collection<Geocache> doImport() throws IOException, ParserException {
        Log.i("Import LOC file: " + file.getAbsolutePath());
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, (int) file.length(), getSourceDisplayName()));
        final LocParser parser = new LocParser(listId);
        return parser.parse(file, progressHandler);
    }

    @Override
    protected String getSourceDisplayName() {
        return file.getName();
    }
}
