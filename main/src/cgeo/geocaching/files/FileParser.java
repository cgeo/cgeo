package cgeo.geocaching.files;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CancellationException;

public abstract class FileParser {
    /**
     * Parses caches from input stream.
     *
     * @param stream
     * @param progressHandler
     *            for reporting parsing progress (in bytes read from input stream)
     * @return collection of caches
     * @throws IOException
     *             if the input stream can't be read
     * @throws ParserException
     *             if the input stream contains data not matching the file format of the parser
     */
    public abstract Collection<Geocache> parse(final InputStream stream, final CancellableHandler progressHandler) throws IOException, ParserException;

    /**
     * Convenience method for parsing a file.
     *
     * @param file
     * @param progressHandler
     * @return
     * @throws IOException
     * @throws ParserException
     */
    public Collection<Geocache> parse(final File file, final CancellableHandler progressHandler) throws IOException, ParserException {
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
        try {
            return parse(stream, progressHandler);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    protected static StringBuilder readStream(InputStream is, CancellableHandler progressHandler) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        ProgressInputStream progressInputStream = new ProgressInputStream(is);
        final BufferedReader input = new BufferedReader(new InputStreamReader(progressInputStream, "UTF-8"));

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

    protected static void showProgressMessage(final CancellableHandler handler, final int bytesRead) {
        if (handler != null) {
            if (handler.isCancelled()) {
                throw new CancellationException();
            }
            handler.sendMessage(handler.obtainMessage(0, bytesRead, 0));
        }
    }

    protected static void fixCache(Geocache cache) {
        if (cache.getInventory() != null) {
            cache.setInventoryItems(cache.getInventory().size());
        } else {
            cache.setInventoryItems(0);
        }
        final long time = new Date().getTime();
        cache.setUpdated(time);
        cache.setDetailedUpdate(time);
    }
}
