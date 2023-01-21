package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.os.Handler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

class ImportGpxFileThread extends AbstractImportGpxThread {
    private final File cacheFile;

    ImportGpxFileThread(final File file, final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.cacheFile = file;
    }

    @Override
    protected Collection<Geocache> doImport(final GPXParser parser) throws IOException, ParserException {
        Log.i("Import GPX file: " + cacheFile.getAbsolutePath());
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, (int) cacheFile.length(), getSourceDisplayName()));
        Collection<Geocache> caches = parser.parse(cacheFile, progressHandler);

        final String wptsFilename = GPXImporter.getWaypointsFileNameForGpxFile(cacheFile);
        if (wptsFilename != null) {
            final File wptsFile = new File(cacheFile.getParentFile(), wptsFilename);
            if (wptsFile.canRead()) {
                Log.i("Import GPX waypoint file: " + wptsFile.getAbsolutePath());
                importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_WPT_FILE, R.string.gpx_import_loading_waypoints_with_filename, (int) wptsFile.length(), wptsFilename));
                caches = parser.parse(wptsFile, progressHandler);
            }
        }
        return caches;
    }

    @Override
    protected String getSourceDisplayName() {
        return cacheFile.getName();
    }
}
