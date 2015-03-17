package cgeo.geocaching.files;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Filter input stream that doesn't forward close() calls. Needed to parse multiple XML documents by SAX XML parser from
 * one input stream (e.g. ZipInputStream) because SAX parser closes stream.
 */
public class NoCloseInputStream extends FilterInputStream {
    private static final ClosedInputStream closedInputStream = new ClosedInputStream();

    public NoCloseInputStream(final InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        in = closedInputStream;
    }


    private static class ClosedInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("Stream already closed.");
        }
    }
}
