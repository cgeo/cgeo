package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import org.eclipse.jdt.annotation.NonNull;

import java.io.File;

/**
 * Connector interface to implement an upload of (already exported) field notes
 *
 */
public interface FieldNotesCapability extends IConnector {
    public boolean uploadFieldNotes(@NonNull final File exportFile);
}
