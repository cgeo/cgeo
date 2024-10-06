package cgeo.geocaching.wherigo;

import cgeo.geocaching.storage.ContentStorage;

import androidx.annotation.NonNull;

import java.util.Date;

public class WherigoSavegameInfo {

    public final ContentStorage.FileInformation fileInfo;
    public final String name;
    public final Date saveDate;

    public WherigoSavegameInfo(final ContentStorage.FileInformation fileInfo, final String name, final Date saveDate) {
        this.fileInfo = fileInfo;
        this.name = name;
        this.saveDate = saveDate;
    }

    @NonNull
    @Override
    public String toString() {
        return "Savegame: " + name + "/File:" + fileInfo;
    }
}
