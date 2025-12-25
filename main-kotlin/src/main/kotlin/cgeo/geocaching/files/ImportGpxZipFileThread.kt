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

import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import android.os.Handler

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class ImportGpxZipFileThread : AbstractImportGpxZipThread() {
    private final File cacheFile

    ImportGpxZipFileThread(final File file, final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler)
        this.cacheFile = file
        Log.i("Import zipped GPX: " + file)
    }

    override     protected InputStream getInputStream() throws IOException {
        return FileInputStream(cacheFile)
    }

}
