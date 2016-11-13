package cgeo.geocaching.files;

import cgeo.geocaching.network.Network;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class ImportGpxZipAttachmentThread extends AbstractImportGpxZipThread {
    private final Uri uri;
    private final ContentResolver contentResolver;

    ImportGpxZipAttachmentThread(final Uri uri, final ContentResolver contentResolver, final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.uri = uri;
        this.contentResolver = contentResolver;
        Log.i("Import zipped GPX from uri: " + uri);
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        try {
            return contentResolver.openInputStream(uri);
        } catch (final FileNotFoundException e) {
            // for http links, we may need to download the content ourselves, if it has no mime type announced by the browser
            if (uri.toString().startsWith("http")) {
                return Network.getResponseStream(Network.getRequest(uri.toString()));
            }
        }
        Log.e("GpxZip import cannot resolve " + uri);
        return null;
    }

}
