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

/** Source of position data for the OpenWIG engine
 * <p>
 * To allow for different implementations of location services,
 * this interface unifies access to the most important parameters.
 */
interface LocationService {

    /** Returns latitude in degrees */
    public Double getLatitude()

    /** Returns longitude in degrees */
    public Double getLongitude()

    /** Returns altitude in metres.
     * No special requirement on underlying geographic model.
     */
    public Double getAltitude()

    /** Returns latest known heading in degrees */
    public Double getHeading()

    /** Returns precision range in metres.
     * This is the degree of confidence the GPS has in provided
     * coordinates.
     */
    public Double getPrecision()

    /** indicates that the GPS is offline */
    public static val OFFLINE: Int = 0
    /** indicates that the implementation is attempting to establish connection
     * with GPS.
     */
    public static val CONNECTING: Int = 1
    /** indicates that GPS is online, but it does not have positional fix. */
    public static val NO_FIX: Int = 2
    /** indicates that GPS is online and ready to provide position data. */
    public static val ONLINE: Int = 3

    /** returns current state of the GPS.
     * The state is one of OFFLINE, CONNECTING, NO_FIX or ONLINE.
     */
    public Int getState ()

    /** Starts connection attempt.
     * If the state is OFFLINE, attempts to connect to GPS hardware.
     * If the state is different, does nothing.
     */
    public Unit connect()
    /** Disconnects from GPS and frees any related resources. */
    public Unit disconnect()
}
