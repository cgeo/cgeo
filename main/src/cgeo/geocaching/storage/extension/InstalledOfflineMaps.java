package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public static void add(@NonNull final String remoteUrl, @NonNull final String localUri, @NonNull final String filename, final long date) {
        removeAll(type, remoteUrl);
        add(type, remoteUrl, date, 0, 0, 0, filename, localUri, "", "");
    }

    public static void remove(@NonNull final String uri) {
        removeAll(type, uri);
    }

}
