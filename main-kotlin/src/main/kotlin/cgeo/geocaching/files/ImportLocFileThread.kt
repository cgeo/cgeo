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

class ImportLocFileThread : AbstractImportThread() {
    private final File file

    ImportLocFileThread(final File file, final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler)
        this.file = file
    }

    override     protected Collection<Geocache> doImport() throws IOException, ParserException {
        Log.i("Import LOC file: " + file.getAbsolutePath())
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, (Int) file.length(), getSourceDisplayName()))
        val parser: LocParser = LocParser(listId)
        return parser.parse(file, progressHandler)
    }

    override     protected String getSourceDisplayName() {
        return file.getName()
    }
}
