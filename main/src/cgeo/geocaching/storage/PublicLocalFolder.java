package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Instances of this class represent a local public folder to store information in.
 */
public class PublicLocalFolder {

    public static final String EMPTY = "---";

    /** Base directory  */
    public static final PublicLocalFolder BASE_DIR = new PublicLocalFolder(R.string.pref_publicfolder_basedir, "BASE", null);

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    public static final PublicLocalFolder OFFLINE_MAPS = new PublicLocalFolder(R.string.pref_publicfolder_offlinemaps, "OFFLINE_MAPS", "maps");
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    public static final PublicLocalFolder OFFLINE_MAP_THEMES = new PublicLocalFolder(R.string.pref_publicfolder_offlinemapthemes, "OFFLINE_MAPS_THEMES", "themes");
    /** Target folder for written logfiles */
    public static final PublicLocalFolder LOGFILES = new PublicLocalFolder(R.string.pref_publicfolder_logfiles, "LOGFILES", "logfiles");

    public static final PublicLocalFolder[] ALL = new PublicLocalFolder[]{BASE_DIR, OFFLINE_MAPS, OFFLINE_MAP_THEMES, LOGFILES };

    @AnyRes
    private final int prefKeyId;
    private final String title;
    private final String defaultSubfolder;
    private final boolean needsWrite;

    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    private PublicLocalFolder(@AnyRes final int prefKeyId, final String title, final String defaultSubfolder) {
        this.title = title;
        this.defaultSubfolder = defaultSubfolder;
        this.prefKeyId = prefKeyId;
        this.needsWrite = true;
    }

    /** Uri associated with this folder */
    @Nullable
    public Uri getUri() {
        final Uri baseUri = getBaseUri();
        if (baseUri != null && !isUserDefinedLocation()) {
            return Uri.withAppendedPath(baseUri, getDefaultSubfolder());
        }
        return baseUri;
    }

    /** Base Uri associated with this folder. The base uri is the uri where we need to grant permission for */
    public Uri getBaseUri() {
        if (isUserDefinedLocation()) {
            return Settings.getPublicFolderUri(this);
        }
        return Settings.getPublicFolderUri(BASE_DIR);
    }

    /** A folder is user-defined if user has explicitely selected the location. Otherwise it is a default folder (subfolder of BASE directory) */
    public boolean isUserDefinedLocation() {
        return Settings.getPublicFolderUri(this) != null;
    }

    /** True if this instance represents the c:geo BASE directory */
    public boolean isBaseFolder() {
        return BASE_DIR.equals(this);
    }

    /** Returns a representation of this folder's location fit to show to an end user */
    @NonNull
    public String getUserDisplayableName() {
        String result = getUri() == null ? EMPTY : getUri().getPath();
        if (isBaseFolder()) {
            return result;
        }

        result += " (";
        if (CgeoApplication.getInstance() == null) {
            //this codepath is only chosen if no translation is available (e.g. in local unit tests)
            result += isUserDefinedLocation() ? "User-Defined" : "Default";
        } else {
            final Context ctx = CgeoApplication.getInstance().getApplicationContext();
            result += isUserDefinedLocation() ? ctx.getString(R.string.publiclocalstorage_userdefined) : ctx.getString(R.string.publiclocalstorage_default);
        }
        result += ")";
        return result;
    }

    /** Returns the Uri of this folder IF it would be in default-mode */
    @NonNull
    public Uri getDefaultFolderUri() {
        final Uri defaultBaseUri = Settings.getPublicFolderUri(BASE_DIR);
        if (defaultBaseUri == null) {
            return null;
        }
        return Uri.withAppendedPath(defaultBaseUri, getDefaultSubfolder());
    }

    /** Returns a user-showable form of the Uri of this folder IF it would be in default-mode */
    @NonNull
    public String getDefaultFolderUserDisplayableUri() {
        final Uri defaultFolderUri = getDefaultFolderUri();
        return defaultFolderUri == null ? EMPTY : defaultFolderUri.getPath();
    }

    /**
     * sets this folder's uri to a new value. Setting it to null means "use default folder"
     * Note: this method should be called only via {@link cgeo.geocaching.storage.PublicLocalStorage#setFolderUri(cgeo.geocaching.storage.PublicLocalFolder, android.net.Uri)}
     */
    public void setUri(final Uri uri) {
        if (equalsUri(uri, getDefaultFolderUri())) {
            //if user selected the default uri, then we set it to default
            Settings.setPublicFolderUri(this, null);
        } else {
            Settings.setPublicFolderUri(this, uri);
        }
    }

    /** Helper method to compare two Uris and also considering encoded / */
    private static boolean equalsUri(final Uri uri1, final Uri uri2) {
        final boolean uri1IsNull = uri1 == null;
        final boolean uri2IsNull = uri2 == null;
        if (uri1IsNull && uri2IsNull) {
            return true;
        }
        if (uri1IsNull || uri2IsNull) {
            return false;
        }

        return uri1.toString().replaceAll("%2F", "/").equals(uri2.toString().replaceAll("%2F", "/"));
    }

    @Nullable
    public String getDefaultSubfolder() {
        return defaultSubfolder;
    }

    public boolean needsWrite() {
        return needsWrite;
    }

    @Override
    public boolean equals(final Object other) {
        return (other instanceof PublicLocalFolder) && this.prefKeyId == ((PublicLocalFolder) other).prefKeyId;
    }

    @Override
    public int hashCode() {
        return this.prefKeyId;
    }

    @Override
    public String toString() {
        return title + ": " + getUserDisplayableName() + "[" + getUri() + "]";
    }

}
