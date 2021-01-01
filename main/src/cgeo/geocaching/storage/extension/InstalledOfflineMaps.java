package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;

public class InstalledOfflineMaps extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_INSTALLED_OFFLINEMAPS;

    private InstalledOfflineMaps(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    @Nullable
    public static InstalledOfflineMaps load(@NonNull final String uri) {
        final DataStore.DBExtension temp = load(type, uri);
        return null == temp ? null : new InstalledOfflineMaps(temp);
    }

    /**
     * returns a list of downloaded offline maps which are still available in the local filesystem
     */
    public static ArrayList<InstalledOfflineMaps> availableOfflineMaps() {
        final ArrayList<InstalledOfflineMaps> result = new ArrayList<>();
        final ArrayList<DataStore.DBExtension> candidates = getAll(type, null);
        for (DataStore.DBExtension candidate : candidates) {
            final File file = new File(candidate.getString2());
            if (file.exists()) {
                result.add(new InstalledOfflineMaps(candidate));
            }
        }
        return result;
    }

    public long getDate() {
        return long1;
    }

    public String getFilename() {
        return string1;
    }

    public String getLocalUri() {
        return string2;
    }

    public String getRemoteUrl() {
        return key;
    }

    public static void add(@NonNull final String remoteUrl, @NonNull final String localUri, @NonNull final String filename, final long date) {
        removeAll(type, remoteUrl);
        add(type, remoteUrl, date, 0, 0, 0, filename, localUri, "", "");
    }

    public static void remove(@NonNull final String uri) {
        removeAll(type, uri);
    }

}
