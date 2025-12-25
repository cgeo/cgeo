// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.files

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import android.os.Handler

import java.io.File
import java.io.IOException
import java.util.Collection

class ImportGpxFileThread : AbstractImportGpxThread() {
    private final File cacheFile

    ImportGpxFileThread(final File file, final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler)
        this.cacheFile = file
    }

    override     protected Collection<Geocache> doImport(final GPXParser parser) throws IOException, ParserException {
        Log.i("Import GPX file: " + cacheFile.getAbsolutePath())
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, (Int) cacheFile.length(), getSourceDisplayName()))
        Collection<Geocache> caches = parser.parse(cacheFile, progressHandler)

        val wptsFilename: String = GPXImporter.getWaypointsFileNameForGpxFile(cacheFile)
        if (wptsFilename != null) {
            val wptsFile: File = File(cacheFile.getParentFile(), wptsFilename)
            if (wptsFile.canRead()) {
                Log.i("Import GPX waypoint file: " + wptsFile.getAbsolutePath())
                importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_WPT_FILE, R.string.gpx_import_loading_waypoints_with_filename, (Int) wptsFile.length(), wptsFilename))
                caches = parser.parse(wptsFile, progressHandler)
            }
        }
        return caches
    }

    override     protected String getSourceDisplayName() {
        return cacheFile.getName()
    }
}
