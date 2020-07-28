package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class FileTypeDetector {

    private final ContentResolver contentResolver;
    private final Uri uri;

    public FileTypeDetector(final Uri uri, final ContentResolver contentResolver) {
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

    @NonNull
    public FileType getFileType() {
        InputStream is = null;
        BufferedReader reader = null;
        FileType type = FileType.UNKNOWN;
        try {
            is = contentResolver.openInputStream(uri);
            if (is == null) {
                return FileType.UNKNOWN;
            }
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            type = detectHeader(reader);
        } catch (final IOException e) {
            if (!uri.toString().startsWith("http")) {
                Log.e("FileTypeDetector", e);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(is);
        }
        return type;
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
        // scan at most 5 lines of a GPX file
        for (int i = 0; i < 5; i++) {
            line = StringUtils.trim(line);
            if (StringUtils.contains(line, "<loc")) {
                return FileType.LOC;
            } else if (StringUtils.contains(line, "<gpx")) {
                return FileType.GPX;
            }
            line = reader.readLine();
        }
        return FileType.UNKNOWN;
    }

    private static boolean isZip(final String line) {
        return StringUtils.length(line) >= 4
                && StringUtils.startsWith(line, "PK") && line.charAt(2) == 3
                && line.charAt(3) == 4;
    }

    private static boolean isMap(final String line) {
        return StringUtils.length(line) >= 20
                && StringUtils.startsWith(line, "mapsforge binary OSM");
    }
}
