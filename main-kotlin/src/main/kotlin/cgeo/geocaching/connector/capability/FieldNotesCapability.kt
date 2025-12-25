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

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.io.File

/**
 * Connector interface to implement an upload of (already exported) field notes
 */
interface FieldNotesCapability : IConnector() {
    /**
     * return {@code true} if uploaded successfully
     */
    @WorkerThread
    Boolean uploadFieldNotes(File exportFile)
}
