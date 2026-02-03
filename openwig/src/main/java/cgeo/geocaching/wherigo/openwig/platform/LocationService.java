/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.platform
 */
package cgeo.geocaching.wherigo.openwig.platform;

/** Source of position data for the OpenWIG engine
 * <p>
 * To allow for different implementations of location services,
 * this interface unifies access to the most important parameters.
 */
public interface LocationService {

    /** Returns latitude in degrees */
    public double getLatitude();

    /** Returns longitude in degrees */
    public double getLongitude();

    /** Returns altitude in metres.
     * No special requirement on underlying geographic model.
     */
    public double getAltitude();

    /** Returns latest known heading in degrees */
    public double getHeading();

    /** Returns precision range in metres.
     * This is the degree of confidence the GPS has in provided
     * coordinates.
     */
    public double getPrecision();

    /** indicates that the GPS is offline */
    public static final int OFFLINE = 0;
    /** indicates that the implementation is attempting to establish connection
     * with GPS.
     */
    public static final int CONNECTING = 1;
    /** indicates that GPS is online, but it does not have positional fix. */
    public static final int NO_FIX = 2;
    /** indicates that GPS is online and ready to provide position data. */
    public static final int ONLINE = 3;

    /** returns current state of the GPS.
     * The state is one of OFFLINE, CONNECTING, NO_FIX or ONLINE.
     */
    public int getState ();

    /** Starts connection attempt.
     * If the state is OFFLINE, attempts to connect to GPS hardware.
     * If the state is different, does nothing.
     */
    public void connect();
    /** Disconnects from GPS and frees any related resources. */
    public void disconnect();
}
