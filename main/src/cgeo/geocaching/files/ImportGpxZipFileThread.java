package cgeo.geocaching.files;

import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class ImportGpxZipFileThread extends AbstractImportGpxZipThread {
    private final File cacheFile;

    public ImportGpxZipFileThread(final File file, final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
        this.cacheFile = file;
        Log.i("Import zipped GPX: " + file);
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return new FileInputStream(cacheFile);
    }
}