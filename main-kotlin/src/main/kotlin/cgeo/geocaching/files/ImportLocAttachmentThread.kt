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

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler

import java.io.IOException
import java.io.InputStream
import java.util.Collection

import org.apache.commons.io.IOUtils

class ImportLocAttachmentThread : AbstractImportThread() {
    private final Uri uri
    private final ContentResolver contentResolver

    ImportLocAttachmentThread(final Uri uri, final ContentResolver contentResolver, final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler)
        this.uri = uri
        this.contentResolver = contentResolver
    }

    override     protected Collection<Geocache> doImport() throws IOException, ParserException {
        Log.i("Import LOC from uri: " + uri)
        importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_READ_FILE, R.string.gpx_import_loading_caches_with_filename, -1, getSourceDisplayName()))
        val is: InputStream = contentResolver.openInputStream(uri)
        val parser: LocParser = LocParser(listId)
        try {
            return parser.parse(is, progressHandler)
        } finally {
            IOUtils.closeQuietly(is)
        }
    }

    override     protected String getSourceDisplayName() {
        return uri.getLastPathSegment()
    }
}
