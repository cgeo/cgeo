package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

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
     * Upload the export file as it is: the one hybrid field notes file with the records of all
     * platforms. Do not split or filter it; the site ignores the records of other platforms itself.
     *
     * return {@code true} if uploaded successfully
     */
    @WorkerThread
    boolean uploadFieldNotes(@NonNull File exportFile);
}
