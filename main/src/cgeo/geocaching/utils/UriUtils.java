package cgeo.geocaching.utils;

import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

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
            final String uriFirstPart = uriString.substring(0, idx);
            //"firstpart" this might be something like /tree/primary (points to root) or /tree/home (points to documents)
            String prepend = "…/";
            if ("/tree/home".equals(uriFirstPart)) {
                prepend = prepend + "Documents/";
            }
            uriString = prepend + uriString.substring(idx + 1);
        }
        while (uriString.endsWith("/")) {
            uriString = uriString.substring(0, uriString.length() - 1);
        }
        return uriString;
    }

    /** Tries to extract the last path segment name of a given Uri, removing "/" and such */
    @Nullable
    public static String getLastPathSegment(final Uri uri) {
        if (uri == null) {
            return null;
        }
        final String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null) {
            return null;
        }
        final String[] tokens = lastPathSegment.split("/");
        if (tokens == null) {
            return lastPathSegment.trim();
        }
        return tokens[tokens.length - 1] == null ? null : tokens[tokens.length - 1].trim();
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
     * Returns a PSEUDO-Uri based on given Root and appended paths. This can be used e.g. as cache key or for logging.
     * The returned string is guaranteed to be unique fpr given root uri and paths.
     *
     * Note that the returned Uri String CAN'T and SHOULDN'T be used to construct an Uri to retrieve documents etc since it is NOT constructred according to its type.
     * For example, content:-Uris can't be constructed at all and must be queries via ContentResolver
     *
     * @param base baseUri
     * @param subdirs (sub)directories or file
     * @param max if >=0 then only up to max subdirs are considered. if < 0 then all subdirs are
     * @return pseudo-uri-string
     */
    @NonNull
    public static String getPseudoUriString(@Nullable final Uri base, @Nullable final List<String> subdirs, final int max) {
        final StringBuilder key = new StringBuilder("p-").append(base).append("::");
        if (subdirs != null) {
            int cnt = 0;
            for (String subdir : subdirs) {
                key.append("/").append(subdir);
                if (max >= 0 && ++cnt >= max) {
                    break;
                }
            }
        }
        return key.toString();
    }

    @Nullable
    public static Uri parseUri(final String uri) {
        if (StringUtils.isBlank(uri)) {
            return null;
        }
        final String uriString = uri.trim();

        //legacy case: file names
        if (uriString.indexOf(":") < 0 && uriString.length() >= 2 && uriString.startsWith("/") && uriString.charAt(1) != '/') {
            return Uri.fromFile(new File(uriString));
        }

        return Uri.parse(uriString);
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
