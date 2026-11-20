package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public class FileTypeDetector {

    /**
     * Only the start of a file is used to detect its type. Without a limit, reading the first "line" of a
     * binary file without line breaks (e.g. a large image) reads the whole file into memory.
     */
    @VisibleForTesting
    static final int MAX_HEADER_BYTES = 64 * 1024;

    private final ContentResolver contentResolver;
    private final Uri uri;

    public FileTypeDetector(final Uri uri, final ContentResolver contentResolver) {
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

    @NonNull
    public FileType getFileType() {
        InputStream is = null;
        FileType type = FileType.UNKNOWN;
        try {
            is = contentResolver.openInputStream(uri);
            if (is == null) {
                return FileType.UNKNOWN;
            }
            type = detectFileType(is);
        } catch (final IOException e) {
            if (!uri.toString().startsWith("http")) {
                Log.e("FileTypeDetector", e);
            }
        } catch (RuntimeException re) {
            Log.e("FileTypeDetector, unexpected error", re);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return type;
    }

    @VisibleForTesting
    @NonNull
    static FileType detectFileType(@NonNull final InputStream is) throws IOException {
        final byte[] header = new byte[MAX_HEADER_BYTES];
        final int length = IOUtils.read(is, header);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(header, 0, length), StandardCharsets.UTF_8))) {
            return detectHeader(reader);
        }
    }

    private static FileType detectHeader(final BufferedReader reader)
            throws IOException {
        String line = reader.readLine();
        if (isZip(line)) {
            return FileType.ZIP;
        }
        if (isMap(line)) {
            return FileType.MAP;
        }
        if (isWherigo(reader, line)) {
            return FileType.WHERIGO;
        }
        // scan at most 5 lines of a GPX file
        for (int i = 0; i < 5; i++) {
            line = StringUtils.trim(line);
            if (Strings.CS.contains(line, "<loc")) {
                return FileType.LOC;
            } else if (Strings.CS.contains(line, "<gpx")) {
                return FileType.GPX;
            }
            line = reader.readLine();
        }
        return FileType.UNKNOWN;
    }

    private static boolean isZip(final String line) {
        return StringUtils.length(line) >= 4
                && Strings.CS.startsWith(line, "PK") && line.charAt(2) == 3
                && line.charAt(3) == 4;
    }

    private static boolean isMap(final String line) {
        return StringUtils.length(line) >= 20
                && Strings.CS.startsWith(line, "mapsforge binary OSM");
    }

    private static boolean isWherigo(final BufferedReader reader, final String line) {
        if (line.length() == 1 && line.charAt(0) == 0x02) {
            try {
                final String line2 = reader.readLine();
                return (Strings.CS.equals(line2.substring(0, 4), "CART"));
            } catch (IOException ignore) {
                // ignore
            }
        }
        return false;
    }
}
