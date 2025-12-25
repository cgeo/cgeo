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

import cgeo.geocaching.network.Network
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.Log

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler

import java.io.FileNotFoundException
import java.io.InputStream

class ImportGpxZipAttachmentThread : AbstractImportGpxZipThread() {
    private final Uri uri
    private final ContentResolver contentResolver

    ImportGpxZipAttachmentThread(final Uri uri, final ContentResolver contentResolver, final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler)
        this.uri = uri
        this.contentResolver = contentResolver
        Log.i("Import zipped GPX from uri: " + uri)
    }

    override     protected InputStream getInputStream() {
        try {
            return contentResolver.openInputStream(uri)
        } catch (final FileNotFoundException e) {
            // for http links, we may need to download the content ourselves, if it has no mime type announced by the browser
            if (uri.toString().startsWith("http")) {
                return Network.getResponseStream(Network.getRequest(uri.toString()))
            }
        }
        Log.e("GpxZip import cannot resolve " + uri)
        return null
    }

}
