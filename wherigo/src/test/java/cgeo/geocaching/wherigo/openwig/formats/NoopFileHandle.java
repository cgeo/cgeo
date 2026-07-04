package cgeo.geocaching.wherigo.openwig.formats;

import cgeo.geocaching.wherigo.openwig.platform.FileHandle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Stand-in {@link FileHandle} for tests: {@link CartridgeFile#read} only passes it to
 * {@code Savegame}'s constructor, which merely stores the reference without touching it, so
 * nothing here needs to actually work.
 */
final class NoopFileHandle implements FileHandle {

    public DataInputStream openDataInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean exists() {
        return false;
    }

    public void create() {
        // no-op
    }

    public void delete() {
        // no-op
    }

    public void truncate(final long len) {
        // no-op
    }
}
