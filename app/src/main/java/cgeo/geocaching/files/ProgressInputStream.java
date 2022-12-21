package cgeo.geocaching.files;

import androidx.annotation.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stream to measure progress of reading automatically.
 * <p>
 * The method @link ProgressInputStream#read(byte[]) does not need to be overridden as it delegates to @link
 * ProgressInputStream#read(byte[], int, int) anyway.
 * </p>
 */
public class ProgressInputStream extends FilterInputStream {

    private int progress = 0;

    protected ProgressInputStream(final InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException { // NO_UCD This method is called from the framework
        final int read = super.read();
        if (read >= 0) {
            progress++;
        }
        return read;
    }

    @Override
    public int read(@NonNull final byte[] buffer, final int offset, final int count) throws IOException {
        final int read = super.read(buffer, offset, count);
        progress += read;
        return read;
    }

    int getProgress() {
        return progress;
    }

}
