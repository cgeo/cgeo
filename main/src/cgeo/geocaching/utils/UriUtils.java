package cgeo.geocaching.utils;

import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * Utility class prvoding helper methods when dealing with Uris
 */
public final class UriUtils {

    private UriUtils() {
        //no instance wanted
    }

    /** Tries to format the given Uri in a user-displayable way */
    @NonNull
    public static String toUserDisplayableString(final Uri uri) {
        if (uri == null) {
            return "";
        }
        final String uriString = uri.getPath();
        final int idx = uriString.lastIndexOf(":");
        if (idx >= 0) {
            return uriString.substring(idx + 1);
        }
        return uriString;
    }

    /** Tries to extract the (file) name of a given Uri */
    @NonNull
    public static String getFileName(final Uri uri) {
        if (uri == null) {
            return "";
        }
        final String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            return "";
        }
        final String[] tokens = lastPathSegment.split("/");
        if (tokens == null) {
            return lastPathSegment;
        }
        return tokens[tokens.length - 1];
    }

    /** Returns whether this Uri is a file Uri */
    public static boolean isFileUri(final Uri uri) {
        if (uri == null) {
            return false;
        }
        return "file".equals(uri.getScheme());
    }

    /** if given Uri is a file uri, then the corresponding file is returned. Otherwise null is returned */
    public static File toFile(final Uri uri) {
        if (isFileUri(uri)) {
            return new File(uri.getPath());
        }
        return null;
    }

    /** Returns string reporesentation of Uri where encoded characters are decoded. Useful e.g. for comparison of Uris */
    public static String toStringDecoded(final Uri uri) {
        if (uri == null) {
            return null;
        }
        return uri.toString().replaceAll("%2F", "/").replaceAll("%3A", ":");
    }

    /** toString()-method for {@link UriPermission} */
    public static String uriPermissionToString(final UriPermission uriPerm) {
        if (uriPerm == null) {
            return "---";
        }
        return uriPerm.getUri() + " (" + Formatter.formatShortDateTime(uriPerm.getPersistedTime()) +
            "):" + (uriPerm.isReadPermission() ? "R" : "-") + (uriPerm.isWritePermission() ? "W" : "-");
    }
}
