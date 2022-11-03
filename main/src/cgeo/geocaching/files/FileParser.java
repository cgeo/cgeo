package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.DisposableHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.commons.io.IOUtils;

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
    @NonNull
    public abstract Collection<Geocache> parse(@NonNull InputStream stream, @Nullable DisposableHandler progressHandler) throws IOException, ParserException;

    /**
     * Convenience method for parsing a file.
     */
    @NonNull
    public Collection<Geocache> parse(final File file, final DisposableHandler progressHandler) throws IOException, ParserException {
        final BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
        try {
            return parse(stream, progressHandler);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @NonNull
    protected static StringBuilder readStream(@NonNull final InputStream is, @Nullable final DisposableHandler progressHandler) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        final ProgressInputStream progressInputStream = new ProgressInputStream(is);
        final BufferedReader input = new BufferedReader(new InputStreamReader(progressInputStream, StandardCharsets.UTF_8));

        try {
            String line;
            while ((line = input.readLine()) != null) {
                buffer.append(line);
                showProgressMessage(progressHandler, progressInputStream.getProgress());
            }
            return buffer;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    protected static void showProgressMessage(@Nullable final DisposableHandler handler, final int bytesRead) {
        if (handler != null) {
            if (handler.isDisposed()) {
                throw new CancellationException();
            }
            handler.sendMessage(handler.obtainMessage(0, bytesRead, 0));
        }
    }

    protected static void fixCache(final Geocache cache) {
        final List<Trackable> inventory = cache.getInventory();
        cache.setInventoryItems(inventory.size());
        final long time = System.currentTimeMillis();
        cache.setUpdated(time);
        cache.setDetailedUpdate(time);

        // fix potentially bad cache id
        if (GCConnector.getInstance().equals(ConnectorFactory.getConnector(cache))) {
            cache.setCacheId(String.valueOf(GCUtils.gcLikeCodeToGcLikeId(cache.getGeocode())));
        }
    }
}
