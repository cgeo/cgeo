package cgeo.geocaching.files;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.DisposableHandler;

import android.os.Handler;

import java.io.IOException;
import java.util.Collection;

abstract class AbstractImportGpxThread extends AbstractImportThread {

    protected AbstractImportGpxThread(final int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler);
    }

    @Override
    protected Collection<Geocache> doImport() throws IOException, ParserException {
        try {
            // try to parse cache file as GPX 10
            return doImport(new GPX10Parser(listId));
        } catch (final ParserException ignored) {
            // didn't work -> lets try GPX11
            return doImport(new GPX11Parser(listId));
        }
    }

    protected abstract Collection<Geocache> doImport(GPXParser parser) throws IOException, ParserException;
}
