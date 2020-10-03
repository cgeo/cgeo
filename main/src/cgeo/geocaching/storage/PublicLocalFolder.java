package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PublicLocalFolder {

    public static final String EMPTY = "---";

    /** Base directory  */
    public static final PublicLocalFolder BASE_DIR = new PublicLocalFolder(R.string.pref_publicfolder_basedir, null, false);

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    public static final PublicLocalFolder OFFLINE_MAPS = new PublicLocalFolder(R.string.pref_publicfolder_offlinemaps, "maps");
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    public static final PublicLocalFolder OFFLINE_MAP_THEMES = new PublicLocalFolder(R.string.pref_publicfolder_offlinemapthemes, "themes");
    /** Target folder for written logfiles */
    public static final PublicLocalFolder LOGFILES = new PublicLocalFolder(R.string.pref_publicfolder_logfiles, "logfiles");

    @AnyRes
    private final int prefKeyId;
    private final String defaultSubfolder;
    private final boolean needsWrite;
    private final boolean canUseDefault;

    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    private PublicLocalFolder(@AnyRes final int prefKeyId, final String defaultSubfolder) {
        this(prefKeyId, defaultSubfolder, true);
    }

    private PublicLocalFolder(@AnyRes final int prefKeyId, final String defaultSubfolder, final boolean canUseDefault) {
        this.defaultSubfolder = defaultSubfolder;
        this.prefKeyId = prefKeyId;
        this.needsWrite = true;
        this.canUseDefault = canUseDefault;
    }

    @Nullable
    public Uri getUri() {
        final Uri baseUri = getBaseUri();
        if (baseUri != null && !isUserDefinedLocation()) {
            return Uri.withAppendedPath(baseUri, getDefaultSubfolder());
        }
        return baseUri;
    }

    public Uri getBaseUri() {
        if (isUserDefinedLocation()) {
            return Settings.getPublicFolderUri(this);
        }
        return Settings.getPublicFolderUri(BASE_DIR);
    }

    public boolean isUserDefinedLocation() {
        return Settings.getPublicFolderUri(this) != null;
    }

    public boolean canUseDefault() {
        return canUseDefault;
    }

    @NonNull
    public String getUserDisplayableName() {
        String result = getUri() == null ? EMPTY : getUri().getPath();
        if (!canUseDefault()) {
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

    @NonNull
    public Uri getDefaultFolderUri() {
        final Uri defaultBaseUri = Settings.getPublicFolderUri(BASE_DIR);
        if (defaultBaseUri == null) {
            return null;
        }
        return Uri.withAppendedPath(defaultBaseUri, getDefaultSubfolder());
    }

    @NonNull
    public String getDefaultFolderUserDisplayableUri() {
        final Uri defaultFolderUri = getDefaultFolderUri();
        return defaultFolderUri == null ? EMPTY : defaultFolderUri.getPath();
    }

    public void setUri(final Uri uri) {
        if (equalsUri(uri, getDefaultFolderUri())) {
            //if user selected the default uri, then we set it to default
            Settings.setPublicFolderUri(this, null);
        } else {
            Settings.setPublicFolderUri(this, uri);
        }
    }

    public static boolean equalsUri(final Uri uri1, final Uri uri2) {
        final boolean uri1IsNull = uri1 == null;
        final boolean uri2IsNull = uri1 == null;
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
        return getUserDisplayableName() + "[" + getUri() + "]";
    }

}
