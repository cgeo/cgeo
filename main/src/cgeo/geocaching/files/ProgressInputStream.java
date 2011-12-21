package cgeo.geocaching.files;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends FilterInputStream {

    private int progress = 0;

    protected ProgressInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        final int read = super.read();
        progress += read;
        return read;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return super.read(buffer);
        // don't increment here, this calls another read implementation which we already measure
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        final int read = super.read(buffer, offset, count);
        progress += read;
        return read;
    }

    int getProgress() {
        return progress;
    }

}
