package cgeo.geocaching.utils;

import java.io.Closeable;
import java.io.IOException;

final public class IOUtils {

    private IOUtils() {}

    public static void closeQuietly(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
                Log.w("closeQuietly: unable to close " + closeable, e);
            }
        }
    }

}
