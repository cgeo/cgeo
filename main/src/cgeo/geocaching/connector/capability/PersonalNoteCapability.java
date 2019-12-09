package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

/**
 * Connector can take a personal note for each cache on its website.
 */
public interface PersonalNoteCapability extends IConnector {

    /**
     * Whether or not the connector supports adding a note to a specific cache. In most cases the argument will not be
     * relevant.
     */
    boolean canAddPersonalNote(@NonNull Geocache cache);

    /**
     * Upload personal note (already stored as member of the cache) to the connector website.
     *
     * @return success
     */
    boolean uploadPersonalNote(@NonNull Geocache cache);

    /**
     * Returns the maximum number of characters allowed in personal notes.
     *
     * @return max number of characters
     */
    int getPersonalNoteMaxChars();

}
