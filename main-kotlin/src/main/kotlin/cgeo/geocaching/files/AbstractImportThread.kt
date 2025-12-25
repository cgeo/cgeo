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
import cgeo.geocaching.SearchResult
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import android.os.Handler

import java.io.IOException
import java.util.Collection
import java.util.concurrent.CancellationException

abstract class AbstractImportThread : Thread() {
    final Int listId
    final Handler importStepHandler
    final DisposableHandler progressHandler

    protected AbstractImportThread(final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        this.listId = listId
        this.importStepHandler = importStepHandler
        this.progressHandler = progressHandler
    }

    override     public Unit run() {
        try {
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_START, getSourceDisplayName()))
            val caches: Collection<Geocache> = doImport()
            Log.i("Imported successfully " + caches.size() + " caches.")

            val search: SearchResult = SearchResult(caches)
            // Do not put imported caches into the cachecache. That would consume lots of memory for no benefit.

            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED, search.getCount(), 0, getSourceDisplayName()))
        } catch (final IOException e) {
            Log.i("Importing caches failed - error reading data: ", e)
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_io, 0, e.getLocalizedMessage()))
        } catch (final ParserException e) {
            Log.i("Importing caches failed - data format error", e)
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_parser, 0, e.getLocalizedMessage()))
        } catch (final CancellationException ignored) {
            Log.i("Importing caches canceled")
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_CANCELED, getSourceDisplayName()))
        } catch (final Exception e) {
            Log.e("Importing caches failed - unknown error: ", e)
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_unexpected, 0, e.getLocalizedMessage()))
        }
    }

    protected abstract Collection<Geocache> doImport() throws IOException, ParserException

    /**
     * Return a user presentable name of the imported source
     *
     * @return The import source display name
     */
    protected abstract String getSourceDisplayName()

}
