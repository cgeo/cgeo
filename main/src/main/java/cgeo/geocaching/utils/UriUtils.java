package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.webkit.MimeTypeMap;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;


/**
 * Utility class prvoding helper methods when dealing with Uris
 */
public final class UriUtils {

    //constants for a unit test volume entry. VolumeId must be one which does NOT OCCUR in real life!
    //(volumneIds in real life are typically of form xxxx-xxxx where x are hexadecimal digits e.g. 11F2-3A16)
    public static final String UNITTEST_VOLUME_ID = "TEST-1111-2222";
    public static final String UNITTEST_VOLUME_NAME = "TEST-SDCARD";

    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_FILE = "file";

    private static final Map<String, String> VOLUME_MAP = getVolumeMap();

    private UriUtils() {
        //no instance wanted
    }

    @DrawableRes
    public static int getMimeTypeIcon(@Nullable final Uri uri) {
        return getMimeTypeIcon(getMimeType(uri));
    }

    @DrawableRes
    public static int getMimeTypeIcon(@Nullable final String mimeType) {

        if (mimeType == null) {
            return R.drawable.ic_menu_file;
        }
        if (mimeType.contains("spreadsheet")) {
            return R.drawable.ic_menu_file_sheet;
        }
        if (mimeType.contains("pdf")) {
            return R.drawable.ic_menu_file_pdf;
        }
        if (mimeType.startsWith("text/")) {
            return R.drawable.ic_menu_file_doc;
        }
        return R.drawable.ic_menu_file;
    }

    @Nullable
    public static String getMimeFileExtension(@Nullable final Uri uri) {
        if (uri == null) {
            return null;
        }
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension == null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getMimeType(uri));
        }
        return extension;
    }

    @Nullable
    public static String getMimeType(@Nullable final Uri uri) {
        if (uri == null) {
            return null;
        }
        String mimeType = null;
        final Context context = CgeoApplication.getInstance();
        if (context != null && context.getContentResolver() != null) {
            mimeType = context.getContentResolver().getType(uri);
        }
        if (StringUtils.isBlank(mimeType)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
        }
        if (StringUtils.isBlank(mimeType)) {
            final String uriString = uri.toString();
            final int lidx = uriString.lastIndexOf(".");
            if (lidx >= 0) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(uriString.substring(lidx + 1));
            }
        }
        return StringUtils.isBlank(mimeType) ? null : mimeType;
    }

    /**
     * Tries to format the given Uri in a user-displayable way
     */
    @NonNull
    public static String toUserDisplayableString(final Uri uri, final List<String> subfolders) {
        String uriString = toUserDisplayableString(uri);
        if (subfolders != null) {
            for (String subfolder : subfolders) {
                uriString += "/" + subfolder;
            }
        }
        return uriString;
    }

    /**
     * Tries to format the given Uri in a user-displayable way
     */
    @NonNull
    public static String toUserDisplayableString(final Uri uri) {

        if (uri == null) {
            return "---";
        }
        //Handle special File case
        if (isFileUri(uri)) {
            return uri.getPath();
        }

        String uriString = uri.getLastPathSegment();
        if (uriString == null) {
            return "---";
        }

        //handle the non-content-case (e.g. web Uris)
        if (!isContentUri(uri)) {
            return uri.toString();
        }

        String volumeId = null;
        //check if there'sa volume
        final int idx = uriString.indexOf(":");
        if (idx >= 0) {
            volumeId = uriString.substring(0, idx);
            uriString = uriString.substring(idx + 1);
        }

        //construct base Uri
        while (uriString.startsWith("/")) {
            uriString = uriString.substring(1);
        }
        uriString = "/" + uriString;

        //add volumne name if available/feasible
        if (volumeId != null) {
            final String volumeName = VOLUME_MAP.get(volumeId);
            if (volumeName != null) {
                uriString = volumeName + uriString;
            } else if (!"primary".equals(volumeId)) {
                uriString = volumeId + uriString;
            }
        }

        //add provider info if available/feasible
        final String providerId = uri.getAuthority();
        //default provider would be "com.android.externalstorage.documents", we don't add anything for this one
        if ("com.android.providers.downloads.documents".equals(providerId)) {
            uriString = "Downloads:" + uriString;
        }

        while (uriString.endsWith("/")) {
            uriString = uriString.substring(0, uriString.length() - 1);
        }

        return uriString;
    }

    /**
     * Get a mapping for all current volumes (UUIDs) to their display name.
     * This might work better or worse depending on Android version and context state. In any case a map object is returned
     *
     * Output (if context is available) is most likely a map with
     * * a null-key for internal storage description
     * * additional keys for each external storage with its Id as used in 'tree' expression and its name
     *
     * Real-world example: SDK29 emulator returns maps with at least the following entries:
     * * null -> "internal shared storage" (this one is returned by Android Volume Manager. It will not be used for display)
     * * "16EA-2F02" -> "SDCARD" (that's a typical entry for an SD card as returned by Android Volume Manager)
     * * "primary" -> null (used for internal storage in Uris. We add the entry to document that "primary" exists but should not be displayed)
     * * some special volume mappings e.g. for "home" (points to "Document" directory)
     * * A dummy entry for unit-tests
     *
     * In an Uri, the "volume id" is typically written directly behind "/tree". In the following uri
     * the "volumeId" is "primary" (the most common one, refering to internal storage):
     * content://com.android.externalstorage.documents/tree/primary%3Acgeo
     */
    @NonNull
    private static Map<String, String> getVolumeMap() {
        final Map<String, String> volumeMap = new HashMap<>();

        //add a fake volume entry for unit tests. This entry has an uuid which will not be used in real-life
        volumeMap.put(UNITTEST_VOLUME_ID, UNITTEST_VOLUME_NAME);

        //add special volume ids that we know of
        volumeMap.put("primary", null); //the most common one where we will NOT put text for
        volumeMap.put("home", "[Documents]"); //example Uri pointing to /Documents/cgeo: content://com.android.externalstorage.documents/tree/home%3Acgeo

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && CgeoApplication.getInstance() != null) {
            final Context context = CgeoApplication.getInstance().getApplicationContext();
            final StorageManager storageManager = ContextCompat.getSystemService(context, StorageManager.class);
            final List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
            for (StorageVolume sv : storageVolumes) {
                volumeMap.put(sv.getUuid(), sv.getDescription(context));
            }
        } else {
            //at least we create a default entry for the primary drive
            volumeMap.put(null, "internal shared storage");
        }
        return volumeMap;
    }

    /**
     * Tries to extract the last path segment name of a given Uri, removing "/" and such
     */
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

    /**
     * Returns whether this Uri is a file Uri
     */
    public static boolean isFileUri(final Uri uri) {
        if (uri == null) {
            return false;
        }
        return SCHEME_FILE.equals(uri.getScheme());
    }

    /**
     * Returns whether this Uri is a content Uri
     */
    public static boolean isContentUri(final Uri uri) {
        if (uri == null) {
            return false;
        }
        return SCHEME_CONTENT.equals(uri.getScheme());
    }

    /**
     * if given Uri is a file uri, then the corresponding file is returned. Otherwise null is returned
     */
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
     * @param base    baseUri
     * @param subdirs (sub)directories or file
     * @param max     if >=0 then only up to max subdirs are considered. if < 0 then all subdirs are
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

    /**
     * toString()-method for {@link UriPermission}
     */
    public static String uriPermissionToString(final UriPermission uriPerm) {
        if (uriPerm == null) {
            return "---";
        }
        return uriPerm.getUri() + " (" + Formatter.formatShortDateTime(uriPerm.getPersistedTime()) +
                "):" + (uriPerm.isReadPermission() ? "R" : "-") + (uriPerm.isWritePermission() ? "W" : "-");
    }


    /**
     * Returns a PSEUDO-Tree-Uri based on the given path. This URI can be used e.g. as EXTRA_INITIAL_URI when requesting access to a folder
     * The returned URI might be OK but also could simply be bullshit, so non't use this function when the URI must always be valid!
     *
     * Note that the returned URI CAN'T and SHOULDN'T be used to access the directory.
     *
     * @param legacyDirectory file uri which points to the directory
     */
    public static Uri getPseudoTreeUriForFileUri(final Uri legacyDirectory) {

        //works only for File Uris!
        if (!(UriUtils.isFileUri(legacyDirectory))) {
            return legacyDirectory;
        }

        // Separate each element of the File path
        // File format: "/storage/XXXX-XXXX/sub-folder1/sub-folder2..../folder"
        //  ele[0] = (empty)
        //  ele[1] = not used (storage name)
        //  ele[2] = storage number ("XXXX-XXXX" for external removable or "primary" for internal)
        //  ele[3 to n] = folders
        final String[] ele = legacyDirectory.getPath().replace("/emulated/0/", "/primary/").split("/");
        if (ele.length < 3) {
            //something seems not right. Log for analysis and continue
            Log.v("[getPseudoTreeUriForFileUri] uri could not be parsed to pseudo tree: " + legacyDirectory);
            return legacyDirectory;
        }

        // Construct folders strings using SAF format
        final StringBuilder folders = new StringBuilder();
        if (ele.length > 3) {
            folders.append(ele[3]);
            for (int i = 4; i < ele.length; ++i) {
                folders.append("%2F").append(ele[i]);
            }
        }
        final String common = ele[2] + "%3A" + folders;

        // Construct TREE Uri
        return new Uri.Builder()
                .scheme(SCHEME_CONTENT)
                .authority("com.android.externalstorage.documents")
                .encodedPath("/tree/" + common + "/document/" + common)
                .build();
    }

}
