package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.export.FieldNotes;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.io.File;

/**
 * Connector interface to implement an upload of (already exported) field notes
 */
public interface FieldNotesCapability extends IConnector {

    /**
     * return {@code true} if field notes can be uploaded at the moment (e.g. supported by the site)
     */
    default boolean canUploadFieldNotes() {
        return true;
    }

    /**
     * Upload field notes. The export file holds the notes of all platforms with the geocaching.com
     * log type names; connectors which need another selection or other log type names build their
     * own content from {@code fieldNotes}.
     *
     * return {@code true} if uploaded successfully
     */
    @WorkerThread
    boolean uploadFieldNotes(@NonNull File exportFile, @NonNull FieldNotes fieldNotes);
}
