package cgeo.geocaching.utils;

import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

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
        String uriString = uri.getPath();
        final int idx = uriString.lastIndexOf(":");
        if (idx >= 0) {
            uriString = "…/" + uriString.substring(idx + 1);
        }
        while (uriString.endsWith("/")) {
            uriString = uriString.substring(0, uriString.length() - 1);
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

    /** Returns whether this Uri is a content Uri */
    public static boolean isContentUri(final Uri uri) {
        if (uri == null) {
            return false;
        }
        return "content".equals(uri.getScheme());
    }

    /** if given Uri is a file uri, then the corresponding file is returned. Otherwise null is returned */
    public static File toFile(final Uri uri) {
        if (isFileUri(uri)) {
            return new File(uri.getPath());
        }
        return null;
    }

    public static Uri appendPath(final Uri uri, final String path) {
        if (uri == null || StringUtils.isBlank(path)) {
            return uri;
        }
        String pathToAppend = path.trim();
        while (pathToAppend.startsWith("/")) {
            pathToAppend = pathToAppend.substring(1);
        }
        while (pathToAppend.endsWith("/")) {
            pathToAppend = pathToAppend.substring(0, pathToAppend.length() - 1);
        }
        if (StringUtils.isBlank(pathToAppend)) {
            return uri;
        }
        return Uri.withAppendedPath(uri, pathToAppend);
    }

    /**
     * Returns a string reporesentation of Uri fit for comparison with other Uris (e.g. to heck for equality).
     *
     * This method tweaks the Uri string such that Uris different in string representation but pointing
     * to same physical folder have a higher chance to match. It does not, however, guarantee that
     * two Uris pointing to the same physical folder will get same string rep (this is simply not possible to achieve)
     *
     * Returned strings may NOT be used to reconstruct an Uri using Uri.parse()!

     */
    public static String toCompareString(final Uri uri) {
        if (uri == null) {
            return null;
        }
        //replace encoded characters
        String uriString = uri.toString().replaceAll("%2F", "/").replaceAll("%3A", ":").trim();
        // remove trailing /
        // This is important because: folders returned by Document Intents may have trailing / while persisted Uris for same folder may not!
        while (uriString.endsWith("/")) {
            uriString = uriString.substring(0, uriString.length() - 1);
        }
        return uriString;
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
