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

import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.DisposableHandler

import android.os.Handler

import java.io.IOException
import java.util.Collection

abstract class AbstractImportGpxThread : AbstractImportThread() {

    protected AbstractImportGpxThread(final Int listId, final Handler importStepHandler, final DisposableHandler progressHandler) {
        super(listId, importStepHandler, progressHandler)
    }

    override     protected Collection<Geocache> doImport() throws IOException, ParserException {
        try {
            // try to parse cache file as GPX 10
            return doImport(GPX10Parser(listId))
        } catch (final ParserException ignored) {
            // didn't work -> lets try GPX11
            return doImport(GPX11Parser(listId))
        }
    }

    protected abstract Collection<Geocache> doImport(GPXParser parser) throws IOException, ParserException
}
