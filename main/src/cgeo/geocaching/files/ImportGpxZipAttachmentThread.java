package cgeo.geocaching.files;

import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;

class ImportGpxZipAttachmentThread extends AbstractImportGpxZipThread {
    private final Uri uri;
    private final ContentResolver contentResolver;

    public ImportGpxZipAttachmentThread(final Uri uri, final ContentResolver contentResolver, final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.uri = uri;
        this.contentResolver = contentResolver;
        Log.i("Import zipped GPX from uri: " + uri);
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return contentResolver.openInputStream(uri);
    }
}