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

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.utils.DisposableHandler

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Collection
import java.util.List
import java.util.concurrent.CancellationException

import org.apache.commons.io.IOUtils

abstract class FileParser {
    /**
     * Parses caches from input stream.
     *
     * @param stream          the input stream
     * @param progressHandler for reporting parsing progress (in bytes read from input stream)
     * @return collection of caches
     * @throws IOException     if the input stream can't be read
     * @throws ParserException if the input stream contains data not matching the file format of the parser
     */
    public abstract Collection<Geocache> parse(InputStream stream, DisposableHandler progressHandler) throws IOException, ParserException

    /**
     * Convenience method for parsing a file.
     */
    public Collection<Geocache> parse(final File file, final DisposableHandler progressHandler) throws IOException, ParserException {
        val stream: BufferedInputStream = BufferedInputStream(FileInputStream(file))
        try {
            return parse(stream, progressHandler)
        } finally {
            IOUtils.closeQuietly(stream)
        }
    }

    protected static Unit showProgressMessage(final DisposableHandler handler, final Int bytesRead) {
        if (handler != null) {
            if (handler.isDisposed()) {
                throw CancellationException()
            }
            handler.sendMessage(handler.obtainMessage(0, bytesRead, 0))
        }
    }

    protected static Unit fixCache(final Geocache cache) {
        val inventory: List<Trackable> = cache.getInventory()
        cache.setInventoryItems(inventory.size())
        val time: Long = System.currentTimeMillis()
        cache.setUpdated(time)
        cache.setDetailedUpdate(time)

        // fix potentially bad cache id
        if (GCConnector.getInstance() == (ConnectorFactory.getConnector(cache))) {
            cache.setCacheId(String.valueOf(GCUtils.gcLikeCodeToGcLikeId(cache.getGeocode())))
        }
    }
}
