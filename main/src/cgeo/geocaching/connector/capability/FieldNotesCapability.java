package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * Connector interface to implement an upload of (already exported) field notes
 *
 */
public interface FieldNotesCapability extends IConnector {
    /**
     * return {@code true} if uploaded successfully
     */
    boolean uploadFieldNotes(@NonNull File exportFile);
}
