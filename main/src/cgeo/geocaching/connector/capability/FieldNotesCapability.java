package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import java.io.File;

/**
 * Connector interface to implement an upload of (already exported) field notes
 *
 */
public interface FieldNotesCapability extends IConnector {
    public boolean uploadFieldNotes(final File exportFile);
}
