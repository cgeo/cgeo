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

/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.platform
 */
package cgeo.geocaching.wherigo.openwig.platform

import java.io.*

/** Platform-independent interface to file handles.
 * Provides ability to check whether a file exists, create or delete it
 * and open data streams for reading or writing.
 * Implementation must ensure that the underlying file is accessible for
 * both reading and writing.
 * <p>
 * FileHandle is used by Savegame to create save files only when needed
 * and to read and write game state data.
 */
interface FileHandle {
    /** Opens a DataInputStream for reading */
    DataInputStream openDataInputStream () throws IOException

    /** Opens a DataOutputStream for writing */
    DataOutputStream openDataOutputStream () throws IOException

    /** Checks whether the underlying file exists */
    Boolean exists () throws IOException

    /** Creates the underlying file.
     * This may fail if the file already exists - caller must ensure
     * that the file is not present by calling exists().
     */
    Unit create () throws IOException

    /** Deletes the underlying file.
     * This may fail if the file does not exist.
     */
    Unit delete () throws IOException

    /** Truncates the underlying file to a given length.
     * This will reduce size of the file to a given length.
     * @param len desired length of file.
     */
    Unit truncate (Long len) throws IOException
}
