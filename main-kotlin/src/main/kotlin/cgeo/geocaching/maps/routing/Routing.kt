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

package cgeo.geocaching.maps.routing

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.brouter.core.RoutingEngine
import cgeo.geocaching.downloader.DownloadConfirmationActivity
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ProcessUtils
import cgeo.geocaching.brouter.BRouterConstants.PROFILE_PARAMTERKEY

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.sax.Element
import android.sax.RootElement
import android.util.Xml

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedList
import java.util.Map

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

class Routing {
    public static val NO_ELEVATION_AVAILABLE: Float = Float.NaN; // check with Float.isNaN(...)

    private static val UPDATE_MIN_DISTANCE_KILOMETERS: Double = 0.005
    private static val MIN_ROUTING_DISTANCE_KILOMETERS: Double = 0.04
    private static val UPDATE_MIN_DELAY_SECONDS: Int = 3
    private static AbstractServiceConnection routingServiceConnection
    private static Geopoint lastDirectionUpdatePoint
    private static Geopoint[] lastRoutingPoints = null
    private static Geopoint lastDestination
    private static Long timeLastUpdate
    private static Int connectCount = 0

    private static val REGISTERED_CALLBACKS: Map<String, Runnable> = HashMap<>()
    private static val requestedTileFiles: ArrayList<String> = ArrayList<>()

    private static val SERVICE_CONNECTED_CALLBACK: Runnable = () -> {
        synchronized (Routing.class) {
            for (Runnable r : REGISTERED_CALLBACKS.values()) {
                r.run()
            }
        }
    }

    private Routing() {
        // utility class
    }

    public static synchronized Unit connect() {
        connect(null, null)
    }

    public static synchronized Unit connect(final LifecycleOwner owner) {
        connect(null, null, owner)
    }

    public static synchronized Unit connect(final String callbackKey, final Runnable onServiceConnectedCallback) {
        connect(callbackKey, onServiceConnectedCallback, null)
    }

    public static synchronized Unit connect(
        final String callbackKey,
        final Runnable onServiceConnectedCallback,
        final LifecycleOwner owner
    ) {

        connectCount++

        if (callbackKey != null && onServiceConnectedCallback != null) {
            REGISTERED_CALLBACKS.put(callbackKey, onServiceConnectedCallback)
        }

        if (owner != null) {
            owner.getLifecycle().addObserver(RoutingObserver(callbackKey))
        }

        if (isConnected()) {
            //already connected
            return
        }

        val intent: Intent = Intent()
        if (Settings.useInternalRouting()) {
            routingServiceConnection = InternalServiceConnection(SERVICE_CONNECTED_CALLBACK)
            intent.setClassName(CgeoApplication.getInstance().getPackageName(), "cgeo.geocaching.brouter.InternalRoutingService")
        } else {
            routingServiceConnection = BRouterServiceConnection(SERVICE_CONNECTED_CALLBACK)
            intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService")
        }

        if (!getContext().bindService(intent, routingServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.d("Connecting brouter: bindService failed or delayed")
        }
    }

    private static ContextWrapper getContext() {
        return CgeoApplication.getInstance()
    }

    public static synchronized Unit disconnect(final String callbackKey) {

        if (callbackKey != null) {
            REGISTERED_CALLBACKS.remove(callbackKey)
        }

        connectCount--

        if (connectCount <= 0) {
            connectCount = 0
            if (isConnected()) {
                getContext().unbindService(routingServiceConnection)
                routingServiceConnection = null

                Log.d("brouter disconnected")
            }
        }
    }

    /**
     * Return a valid track (with at least two points, including the start and destination).
     * In some cases (e.g., destination is too close or too far, path could not be found),
     * a straight line will be returned.
     *
     * @param start       the starting point
     * @param destination the destination point
     * @return a track with at least two points including the start and destination points
     */
    public static Geopoint[] getTrack(final Geopoint start, final Geopoint destination, final ArrayList<Float> elevation) {
        if (routingServiceConnection == null || Settings.getRoutingMode() == RoutingMode.STRAIGHT) {
            return defaultTrack(start, destination)
        }

        // avoid updating to frequently
        val timeNow: Long = System.currentTimeMillis()
        if ((timeNow - timeLastUpdate) < 1000 * UPDATE_MIN_DELAY_SECONDS) {
            return ensureTrack(lastRoutingPoints, start, destination)
        }

        // Disable routing for huge distances
        val maxThresholdKm: Int = Settings.getBrouterThreshold()
        val targetDistance: Float = start.distanceTo(destination)
        if (targetDistance > maxThresholdKm) {
            return defaultTrack(start, destination)
        }

        // disable routing when near the target
        if (targetDistance < MIN_ROUTING_DISTANCE_KILOMETERS) {
            return defaultTrack(start, destination)
        }

        // Use cached route if current position has not changed more than 5m and we had a route
        // TODO: Maybe adjust this to current zoomlevel
        if (lastDirectionUpdatePoint != null && destination == lastDestination && start.distanceTo(lastDirectionUpdatePoint) < UPDATE_MIN_DISTANCE_KILOMETERS && lastRoutingPoints != null) {
            return lastRoutingPoints
        }

        // now really calculate a route
        lastDestination = destination
        lastRoutingPoints = calculateRouting(start, destination, elevation)
        lastDirectionUpdatePoint = start
        timeLastUpdate = timeNow
        return ensureTrack(lastRoutingPoints, start, destination)
    }

    public static Float getElevation(final Geopoint current) {
        if (routingServiceConnection == null || current == null || !routingServiceConnection.isConnected()) {
            return NO_ELEVATION_AVAILABLE
        }
        val params: Bundle = Bundle()
        params.putInt("engineMode", RoutingEngine.BROUTER_ENGINEMODE_GETELEV)
        params.putDoubleArray("lats", Double[]{current.getLatitude(), current.getLatitude()})
        params.putDoubleArray("lons", Double[]{current.getLongitude(), current.getLongitude()})
        params.putString("v", RoutingMode.STRAIGHT.parameterValue)
        val gpx: String = routingServiceConnection.getTrackFromParams(params)

        // parse result
        final Boolean[] inElevationElement = Boolean[1]
        final Float[] result = Float[1]
        result[0] = NO_ELEVATION_AVAILABLE
        try {
            Xml.parse(gpx, DefaultHandler() {
                override                 public Unit startElement(final String uri, final String localName, final String qName, final Attributes atts) {
                    if (qName.equalsIgnoreCase("ele")) {
                        inElevationElement[0] = true
                    }
                }

                override                 public Unit endElement(final String uri, final String localName, final String qName) {
                    inElevationElement[0] = false
                }

                override                 public Unit characters(final Char[] ch, final Int start, final Int length) {
                    if (inElevationElement[0]) {
                        result[0] = Float.parseFloat(String.valueOf(ch))
                    }
                }
            })
        } catch (SAXException | NullPointerException ignore) {
            return NO_ELEVATION_AVAILABLE
        }
        return result[0]
    }

    /**
     * Return a valid track (with at least two points, including the start and destination).
     * no caching
     *
     * @param start       the starting point
     * @param destination the destination point
     * @param elevation   the variable to write elevation info to (null, if no elevation info to be returned)
     * @return a track with at least two points including the start and destination points
     */
    public static Geopoint[] getTrackNoCaching(final Geopoint start, final Geopoint destination, final ArrayList<Float> elevation) {
        if (routingServiceConnection == null || Settings.getRoutingMode() == RoutingMode.STRAIGHT) {
            return defaultTrack(start, destination)
        }

        // Disable routing for huge distances
        val maxThresholdKm: Int = Settings.getBrouterThreshold()
        val targetDistance: Float = start.distanceTo(destination)
        if (targetDistance > maxThresholdKm) {
            return defaultTrack(start, destination)
        }

        // disable routing when near the target
        if (targetDistance < MIN_ROUTING_DISTANCE_KILOMETERS) {
            return defaultTrack(start, destination)
        }

        // now calculate a route
        final Geopoint[] track = calculateRouting(start, destination, elevation)
        return ensureTrack(track, start, destination)
    }

    private static Geopoint[] ensureTrack(final Geopoint[] routingPoints, final Geopoint start, final Geopoint destination) {
        return routingPoints != null ? routingPoints : defaultTrack(start, destination)
    }

    private static Geopoint[] defaultTrack(final Geopoint start, final Geopoint destination) {
        return Geopoint[]{start, destination}
    }

    @SuppressWarnings({"PMD.NPathComplexity"}) // splitting up would not improve readability
    private static Geopoint[] calculateRouting(final Geopoint start, final Geopoint dest, final ArrayList<Float> elevation) {
        val params: Bundle = Bundle()
        params.putString("trackFormat", "gpx")
        params.putDoubleArray("lats", Double[]{start.getLatitude(), dest.getLatitude()})
        params.putDoubleArray("lons", Double[]{start.getLongitude(), dest.getLongitude()})
        params.putString("v", Settings.getRoutingMode().parameterValue)
        params.putString(PROFILE_PARAMTERKEY, Settings.getRoutingProfile()); // profile filename, used only by internal routing engine

        val gpx: String = routingServiceConnection == null ? null : routingServiceConnection.getTrackFromParams(params)

        if (gpx == null) {
            Log.i("brouter returned no data")
            return null
        }

        // missing routing data?
        if (gpx.startsWith("datafile ") && gpx.endsWith(" not found")) {
            synchronized (requestedTileFiles) {
                String filename = gpx.substring(9)
                val pos: Int = filename.indexOf(" ")
                if (pos != -1) {
                    filename = filename.substring(0, pos)
                }
                Boolean alreadyRequested = false
                for (String f : requestedTileFiles) {
                    if (filename == (f)) {
                        alreadyRequested = true
                        break
                    }
                }
                Log.i("routing: missing file " + filename + ", alreadyRequested=" + alreadyRequested)
                if (!alreadyRequested) {
                    requestedTileFiles.add(filename)
                    if (Settings.useInternalRouting()) {
                        if (Settings.isBrouterAutoTileDownloads() && !PersistableFolder.ROUTING_TILES.isLegacy()) {
                            val intent: Intent = Intent(getContext(), DownloadConfirmationActivity.class)
                            intent.putExtra(DownloadConfirmationActivity.BUNDLE_FILENAME, filename)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            getContext().startActivity(intent)
                        }
                    } else {
                        ActivityMixin.showApplicationToast(String.format(getContext().getString(R.string.brouter_missing_data), filename))
                    }
                }
            }
        }

        // other error
        if (!gpx.startsWith("<?xml")) {
            Log.w("brouter returned an error message: " + gpx)
            return null
        }

        return parseGpxTrack(gpx, dest, elevation)
    }

    private static Geopoint[] parseGpxTrack(final String gpx, final Geopoint destination, final ArrayList<Float> elevation) {
        try {
            val result: LinkedList<Geopoint> = LinkedList<>()

            val namespace: String = "http://www.topografix.com/GPX/1/1"
            val root: RootElement = RootElement(namespace, "gpx")
            val trk: Element = root.getChild(namespace, "trk")
            val trkseg: Element = trk.getChild(namespace, "trkseg")
            val trkpt: Element = trkseg.getChild(namespace, "trkpt")
            val ele: Element = trkpt.getChild(namespace, "ele")

            trkpt.setStartElementListener(attributes -> {
                val lat: String = attributes.getValue("lat")
                if (lat != null) {
                    val lon: String = attributes.getValue("lon")
                    if (lon != null) {
                        result.add(Geopoint(lat, lon))
                    }
                }
            })

            ele.setEndTextElementListener(body -> {
                if (elevation != null) {
                    elevation.add(Float.parseFloat(body))
                }
            })

            Xml.parse(gpx, root.getContentHandler())

            // artificial straight line from track to target
            if (destination != null) {
                result.add(destination)
                if (elevation != null) {
                    elevation.add(Float.NaN)
                }
            }
            return result.toArray(Geopoint[0])

        } catch (SAXException e) {
            Log.w("cannot parse brouter output of length " + gpx.length() + ", gpx=" + gpx, e)
        }
        return null
    }

    public static Unit invalidateRouting() {
        lastDirectionUpdatePoint = null
        timeLastUpdate = 0
    }

    /**
     * Are we currently connected to any routing service (internal/external)?
     */
    public static Boolean isConnected() {
        return routingServiceConnection != null && routingServiceConnection.isConnected()
    }

    /**
     * Is any routing service available (internal/external)
     */
    public static Boolean isAvailable() {
        return isConnected() || Settings.useInternalRouting() || isExternalRoutingInstalled()
    }

    /**
     * Is external routing app installed?
     */
    public static Boolean isExternalRoutingInstalled() {
        return ProcessUtils.isInstalled(getExternalRoutingPackageName())
    }

    /**
     * Get Android package name for external routing app
     */
    public static String getExternalRoutingPackageName() {
        return getContext().getString(R.string.package_brouter)
    }

    private static class RoutingObserver : DefaultLifecycleObserver {

        private final String callbackKey

        RoutingObserver(final String callbackKey) {
            this.callbackKey = callbackKey
        }

        override         public Unit onDestroy(final LifecycleOwner owner) {
            disconnect(callbackKey)
        }
    }
}
