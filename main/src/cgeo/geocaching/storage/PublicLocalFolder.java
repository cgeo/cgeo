package cgeo.geocaching.storage;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CalendarUtils;

import android.net.Uri;

import androidx.annotation.AnyRes;

import java.util.concurrent.atomic.AtomicInteger;

public class PublicLocalFolder {

    /** Base directory  */
    public static final PublicLocalFolder BASE_DIR = new PublicLocalFolder(R.string.pref_publicfolder_basedir, null, "map", "map");

    /** Offline Maps folder where cgeo looks for offline map files (also the one where c:geo downloads its own offline maps) */
    public static final PublicLocalFolder OFFLINE_MAPS = new PublicLocalFolder(R.string.pref_publicfolder_offlinemaps, "maps", "map", "map");
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    public static final PublicLocalFolder OFFLINE_MAP_THEMES = new PublicLocalFolder(R.string.pref_publicfolder_offlinemapthemes, "maps", "map", "map");

    /** Target folder for written logfiles */
    public static final PublicLocalFolder LOGFILES = new PublicLocalFolder(R.string.pref_publicfolder_logfiles, "logfiles", "logcat", "txt");

    //public static final PublicLocalFolder BACKUP = new PublicLocalFolder("backup", "", "sqlite");
    //public static final PublicLocalFolder GPX = new PublicLocalFolder("gpx", "gpx", "jpg", () -> MediaStore.Images.Media.INTERNAL_CONTENT_URI);

    private final AtomicInteger fileNameCounter = new AtomicInteger(1);

    @AnyRes
    private final int prefKeyId;
    private final String defaultSubfolder;
    private final String defaultFilePrefix;
    private final String defaultFileSuffix;
    private final boolean needsWrite;

    @AnyRes
    public int getPrefKeyId() {
        return prefKeyId;
    }

    private PublicLocalFolder(@AnyRes final int prefKeyId, final String defaultSubfolder, final String defaultFilePrefix, final String defaultFileSuffix) {
        this.defaultSubfolder = defaultSubfolder;
        this.defaultFilePrefix = defaultFilePrefix;
        this.defaultFileSuffix = defaultFileSuffix;
        this.prefKeyId = prefKeyId;
        this.needsWrite = true; //TODO
    }

    public Uri getBaseUri() {
        final Uri uri = Settings.getPublicFolderUri(this);
        return uri == null ? Settings.getPublicFolderUri(BASE_DIR) : uri;
    }

    public boolean isUserDefinedLocation() {
        return Settings.getPublicFolderUri(this) != null;
    }

    public String getUserDisplayableUri() {
        if (!isUserDefinedLocation()) {
            return getBaseUri().toString() + "/" + getSubfolder();
        }
        return getBaseUri().toString();
    }

    public void setUri(final Uri uri) {
        Settings.setPublicFolderUri(this, uri);
    }

    public String getSubfolder() {
        return isUserDefinedLocation() ? null : defaultSubfolder;
    }

    public boolean needsWrite() {
        return needsWrite || getSubfolder() != null;  //if there's a subfolder, we need to create it and need write permission
    }

    public String createNewFilename() {
        return defaultFilePrefix + "_" + CalendarUtils.formatDateTime("yyyy-MM-dd_HH-mm-ss") + "-" + (fileNameCounter.addAndGet(1))
            + "." + defaultFileSuffix;
    }

    public String getDefaultMimeType() {
        //TODO make configurable
        return "text/plain";
    }

    @Override
    public boolean equals(final Object other) {
        return (other instanceof PublicLocalFolder) && this.prefKeyId == ((PublicLocalFolder) other).prefKeyId;
    }

    @Override
    public int hashCode() {
        return this.prefKeyId;
    }

    public String toString() {
        return getUserDisplayableUri();
    }

}
