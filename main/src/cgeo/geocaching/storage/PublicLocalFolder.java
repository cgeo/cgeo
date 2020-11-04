package cgeo.geocaching.storage;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CalendarUtils;

import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.util.Supplier;

import java.util.concurrent.atomic.AtomicInteger;

public class PublicLocalFolder {

    /** Offline Maps: default folder (also the one where c:geo downloads its own offline maps) */
    public static final PublicLocalFolder OFFLINE_MAP_DEFAULT = new PublicLocalFolder("maps", "map", "map");
    /** Offline Maps: optional additional folder (configured in settings) with user-supplied maps acquired outside of c:geo */
    public static final PublicLocalFolder OFFLINE_MAP_CONFIGURED = new PublicLocalFolder("maps", "map", "map");
    /** Offline Maps: optional folder for map themes (configured in settings) with user-supplied theme data */
    public static final PublicLocalFolder OFFLINE_MAP_THEMES = new PublicLocalFolder("maps", "map", "map");

    /** Target folder for written logfiles */
    public static final PublicLocalFolder LOGFILES = new PublicLocalFolder("logfiles", "logcat", "txt");

    public static final PublicLocalFolder BACKUP = new PublicLocalFolder("backup", "", "sqlite");
    public static final PublicLocalFolder IMAGES = new PublicLocalFolder("cgeo", "img", "jpg", () -> MediaStore.Images.Media.INTERNAL_CONTENT_URI);
    public static final PublicLocalFolder GPX = new PublicLocalFolder("gpx", "gpx", "jpg", () -> MediaStore.Images.Media.INTERNAL_CONTENT_URI);


    private final AtomicInteger fileNameCounter = new AtomicInteger(1);

    private final String folderName;
    private final String defaultFilePrefix;
    private final String defaultFileSuffix;
    private final Supplier<Uri> baseUriSupplier;
    private final boolean needsWrite;

    private PublicLocalFolder(final String folderName, final String defaultFilePrefix, final String defaultFileSuffix) {
        this(folderName, defaultFilePrefix, defaultFileSuffix, null);
    }

    private PublicLocalFolder(final String folderName, final String defaultFilePrefix, final String defaultFileSuffix, final Supplier<Uri> baseUriSupplier) {
        this.baseUriSupplier = baseUriSupplier;
        this.folderName = folderName;
        this.defaultFilePrefix = defaultFilePrefix;
        this.defaultFileSuffix = defaultFileSuffix;
        this.needsWrite = true; //TODO
    }

    public Uri getBaseUri() {
        if (baseUriSupplier == null) {
            return Settings.getBaseDir();
        }
        return this.baseUriSupplier.get();
    }

    public void setBaseUri(final Uri uri) {
        if (baseUriSupplier == null) {
            Settings.setBaseDir(uri);
        }
        //else? //TODO
    }

    public String getFolderName() {
        return folderName;
    }

    public boolean needsWrite() {
        return needsWrite || getFolderName() != null;  //if there's a subfolder, we need to create it and need write permission
    }

    public String createNewFilename() {
        return defaultFilePrefix + "_" + CalendarUtils.formatDateTime("yyyy-MM-dd_HH-mm-ss") + "-" + (fileNameCounter.addAndGet(1))
            + "." + defaultFileSuffix;
    }

    public String getDefaultMimeType() {
        //TODO make configurable
        return "text/plain";
    }

}
