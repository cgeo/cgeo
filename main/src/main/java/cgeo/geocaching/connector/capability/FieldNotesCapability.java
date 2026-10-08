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
     * Upload field notes. All upload targets receive the same, hybrid field notes (all platforms):
     * <ul>
     * <li>{@code exportFile}: the export file, written once by the export (not by the connector).
     * For connectors which upload the file itself; the site ignores records of other platforms.</li>
     * <li>{@code fieldNotes}: the same notes as an object. For connectors which upload through an
     * API; they take their own records with their own log type names from it.</li>
     * </ul>
     * A connector uses whichever of the two fits its site. Never split the field notes per platform.
     *
     * return {@code true} if uploaded successfully
     */
    @WorkerThread
    boolean uploadFieldNotes(@NonNull File exportFile, @NonNull FieldNotes fieldNotes);
}
