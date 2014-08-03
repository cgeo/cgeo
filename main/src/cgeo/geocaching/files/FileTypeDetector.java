package cgeo.geocaching.files;

import cgeo.geocaching.utils.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileTypeDetector {

    private final ContentResolver contentResolver;
    private final Uri uri;

    public FileTypeDetector(Uri uri, ContentResolver contentResolver) {
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

	public @NonNull FileType getFileType() {
        InputStream is = null;
        BufferedReader reader = null;
		FileType type = FileType.UNKNOWN;
		try {
            is = contentResolver.openInputStream(uri);
            if (is == null) {
                return FileType.UNKNOWN;
            }
            reader = new BufferedReader(new InputStreamReader(is));
			type = detectHeader(reader);
            reader.close();
        } catch (FileNotFoundException e) {
			Log.e("FileTypeDetector", e);
        } catch (IOException e) {
			Log.e("FileTypeDetector", e);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(is);
        }
		return type;
    }

	private static FileType detectHeader(BufferedReader reader)
			throws IOException {
		String line = reader.readLine();
		if (isZip(line)) {
			return FileType.ZIP;
		}
		// scan at most 5 lines of a GPX file
		for (int i = 0; i < 5; i++) {
			line = StringUtils.trim(line);
			if (StringUtils.contains(line, "<loc")) {
				return FileType.LOC;
			}
			if (StringUtils.contains(line, "<gpx")) {
				return FileType.GPX;
			}
			line = reader.readLine();
		}
		return FileType.UNKNOWN;
	}

	private static boolean isZip(String line) {
		return StringUtils.length(line) >= 4
				&& StringUtils.startsWith(line, "PK") && line.charAt(2) == 3
				&& line.charAt(3) == 4;
	}

}
