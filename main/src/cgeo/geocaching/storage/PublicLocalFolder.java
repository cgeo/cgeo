package cgeo.geocaching.storage;

import cgeo.geocaching.settings.Settings;

import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.util.Supplier;

public enum PublicLocalFolder {

    LOGFILES("logfiles"),
    BACKUP("backup"),
    IMAGES("cgeo", () -> MediaStore.Images.Media.INTERNAL_CONTENT_URI),
    OFFLINE_MAP("maps");

    private final String folderName;
    private final Supplier<Uri> baseUriSupplier;
    private final boolean needsWrite;

    PublicLocalFolder(final String folderName) {
        this(folderName, null);
    }

    PublicLocalFolder(final String folderName, final Supplier<Uri> baseUriSupplier) {
        this.baseUriSupplier = baseUriSupplier;
        this.folderName = folderName;
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

    public String getDefaultMimeType() {
        //TODO make configurable
        return "text/plain";
    }

}
