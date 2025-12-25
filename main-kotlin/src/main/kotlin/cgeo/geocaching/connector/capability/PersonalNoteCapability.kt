// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.capability

import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

/**
 * Connector can take a personal note for each cache on its website.
 */
interface PersonalNoteCapability : IConnector() {

    /**
     * Whether or not the connector supports adding a note to a specific cache. In most cases the argument will not be
     * relevant.
     */
    Boolean canAddPersonalNote(Geocache cache)

    /**
     * Upload personal note (already stored as member of the cache) to the connector website.
     *
     * @return success
     */
    Boolean uploadPersonalNote(Geocache cache)

    /**
     * Returns the maximum number of characters allowed in personal notes.
     *
     * @return max number of characters
     */
    Int getPersonalNoteMaxChars()

}
