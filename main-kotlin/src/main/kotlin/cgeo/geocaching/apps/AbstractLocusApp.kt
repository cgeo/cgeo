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

package cgeo.geocaching.apps

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.Log

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import java.io.File
import java.util.ArrayList
import java.util.Date
import java.util.List

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import locus.api.android.ActionDisplayPoints
import locus.api.android.ActionDisplayVarious
import locus.api.android.objects.LocusVersion
import locus.api.android.objects.PackPoints
import locus.api.android.objects.VersionCode
import locus.api.android.utils.LocusUtils
import locus.api.android.utils.exceptions.RequiredVersionMissingException
import locus.api.objects.extra.Location
import locus.api.objects.geoData.GeoDataHelperKt
import locus.api.objects.geoData.Point
import locus.api.objects.geocaching.GeocachingAttribute
import locus.api.objects.geocaching.GeocachingData
import locus.api.objects.geocaching.GeocachingWaypoint
import org.apache.commons.collections4.CollectionUtils

/**
 * for the Locus API:
 *
 * @see <a href="https://docs.locusmap.eu/doku.php?id=manual:advanced:locus_api:installation">Locus API</a>
 * @see <a href="https://github.com/asamm/locus-api/blob/master/locus-api-android-sample/src/main/java/com/asamm/locus/api/sample/utils/SampleCalls.kt">Sample Calls</a>
 */
abstract class AbstractLocusApp : AbstractApp() {

    /**
     * Locus Version
     */
    private final LocusVersion lv

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    protected AbstractLocusApp(final String text, final String intent) {
        super(text, intent)

        try (ContextLogger cLog = ContextLogger(true, "AbstractLocusApp.init (" + this.getClass().getName() + ")")) {
            lv = LocusUtils.INSTANCE.getActiveVersion(CgeoApplication.getInstance())
            cLog.add("V:" + lv)
            if (lv == null) { // locus not installed
                Log.w("Couldn't get active Locus version")
            }
        }
    }

    override     public Boolean isInstalled() {
        return lv != null
    }

    /**
     * Display a list of caches / waypoints in Locus
     *
     * @param objectsToShow      which caches/waypoints to show
     * @param withCacheWaypoints Whether to give waypoints of caches to Locus or not
     */
    protected Unit showInLocus(final List<?> objectsToShow, final Boolean withCacheWaypoints, final Boolean export,
                               final Context context) {
        if (CollectionUtils.isEmpty(objectsToShow)) {
            return
        }

        if (!isInstalled()) {
            return
        }

        val withCacheDetails: Boolean = objectsToShow.size() < 200
        val pd: PackPoints = PackPoints("c:geo")
        for (final Object o : objectsToShow) {
            Point p = null
            // get icon and Point
            if (o is Geocache) {
                p = getCachePoint((Geocache) o, withCacheWaypoints, withCacheDetails)
            } else if (o is cgeo.geocaching.models.Waypoint) {
                p = getWaypointPoint((cgeo.geocaching.models.Waypoint) o)
            }
            if (p != null) {
                pd.addPoint(p)
            }
        }

        if (pd.getPoints().length == 0) {
            return
        }

        if (pd.getPoints().length <= 1000) {
            try {
                ActionDisplayPoints.INSTANCE.sendPack(context, pd, export ? ActionDisplayVarious.ExtraAction.IMPORT : ActionDisplayVarious.ExtraAction.CENTER)
            } catch (final RequiredVersionMissingException e) {
                Log.e("AbstractLocusApp.showInLocus: problem in sendPack", e)
            }
        } else {
            val externalDir: File = File(context.getExternalFilesDir(null), "showIn.locus")
            val file: File = File(externalDir.toURI())

            val data: ArrayList<PackPoints> = ArrayList<>()
            data.add(pd)

            try {
                if (lv.isVersionValid(VersionCode.UPDATE_15)) {
                    // send file via FileProvider, you don't need WRITE_EXTERNAL_STORAGE permission for this
                    val uri: Uri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file)
                    ActionDisplayPoints.INSTANCE.sendPacksFile(context, lv, data, file, uri, export ? ActionDisplayVarious.ExtraAction.IMPORT : ActionDisplayVarious.ExtraAction.CENTER)
                } else {
                    // send file old way, you need WRITE_EXTERNAL_STORAGE permission for this
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        ActionDisplayPoints.INSTANCE.sendPacksFile(context, lv, data, file, null, ActionDisplayVarious.ExtraAction.CENTER)
                    } else {
                        ActivityMixin.showToast(context, getString(R.string.storage_permission_needed))
                    }
                }
            } catch (final RequiredVersionMissingException e) {
                Log.e("AbstractLocusApp.showInLocus: problem in sendPacksFile", e)
            }
        }
    }

    /**
     * This method constructs a {@code Point} for displaying in Locus
     *
     * @param withWaypoints    whether to give waypoints to Locus or not
     * @param withCacheDetails whether to give cache details (description, hint) to Locus or not
     *                         should be false for all if more than 200 Caches are transferred
     * @return null, when the {@code Point} could not be constructed
     */
    private static Point getCachePoint(final Geocache cache, final Boolean withWaypoints, final Boolean withCacheDetails) {
        if (cache == null || cache.getCoords() == null) {
            return null
        }

        // create one simple point with location
        val loc: Location = Location()
        loc.setProvider("cgeo")
        loc.setLatitude(cache.getCoords().getLatitude())
        loc.setLongitude(cache.getCoords().getLongitude())

        val wpt: Point = Point(cache.getName(), loc)
        // generate data
        val gcData: GeocachingData = GeocachingData()

        // fill data with variables
        gcData.setCacheID(cache.getGeocode())
        gcData.setName(cache.getName())

        // rest is optional so fill as you want - should work
        gcData.setOwner(cache.getOwnerUserId())
        gcData.setPlacedBy(cache.getOwnerDisplayName())
        if (cache.getDifficulty() > 0) {
            gcData.setDifficulty(cache.getDifficulty())
        }
        if (cache.getTerrain() > 0) {
            gcData.setTerrain(cache.getTerrain())
        }
        gcData.setContainer(toLocusSize(cache.getSize()))
        val attributes: ArrayList<GeocachingAttribute> = toLocusAttributes(cache.getAttributes())
        if (!attributes.isEmpty()) {
            gcData.setAttributes(attributes)
        }

        gcData.setAvailable(!cache.isDisabled())
        gcData.setArchived(cache.isArchived())
        gcData.setPremiumOnly(cache.isPremiumMembersOnly())
        val hiddenDate: Date = cache.getHiddenDate()
        if (hiddenDate != null) {
            gcData.setDateHidden(hiddenDate.getTime())
        }
        gcData.setType(toLocusType(cache.getType()))
        gcData.setFound(cache.isFound())

        if (withWaypoints && cache.hasWaypoints()) {
            for (final cgeo.geocaching.models.Waypoint waypoint : cache.getWaypoints()) {
                if (waypoint == null) {
                    continue
                }

                val gcWpt: GeocachingWaypoint = GeocachingWaypoint()
                gcWpt.setCode(waypoint.getLookup())
                gcWpt.setName(waypoint.getName())
                gcWpt.setDesc(waypoint.getNote())
                gcWpt.setType(toLocusWaypoint(waypoint.getWaypointType()))

                val waypointCoords: Geopoint = waypoint.getCoords()
                if (waypointCoords != null) {
                    gcWpt.setLon(waypointCoords.getLongitude())
                    gcWpt.setLat(waypointCoords.getLatitude())
                }

                gcData.getWaypoints().add(gcWpt)
            }
        }

        // Other properties of caches. When there are many caches to be displayed
        // in Locus, using these properties can lead to Exceptions in Locus.
        // Should not be used if caches count > 200
        if (withCacheDetails) {
            gcData.setDescriptions(
                    cache.getShortDescription(), true,
                    cache.getDescription(), true)
            gcData.setEncodedHints(cache.getHint())
        }

        // set data and return point
        wpt.setGcData(gcData)
        return wpt
    }

    /**
     * This method constructs a {@code Point} for displaying in Locus
     *
     * @return null, when the {@code Point} could not be constructed
     */
    private static Point getWaypointPoint(final cgeo.geocaching.models.Waypoint waypoint) {
        if (waypoint == null) {
            return null
        }
        val coordinates: Geopoint = waypoint.getCoords()
        if (coordinates == null) {
            return null
        }

        // create one simple point with location
        val loc: Location = Location()
        loc.setProvider("cgeo")
        loc.setLatitude(coordinates.getLatitude())
        loc.setLongitude(coordinates.getLongitude())

        val p: Point = Point(waypoint.getName(), loc)
        GeoDataHelperKt.setParameterDescription(
                p, "Name: " + waypoint.getName() +
                        "<br />Note: " + waypoint.getNote() +
                        "<br />UserNote: " + waypoint.getUserNote() +
                        "<br /><br /> <a href=\"" + waypoint.getUrl() + "\">" + waypoint.getGeocode() + "</a>"
        )

        return p
    }

    // https://github.com/asamm/locus-api/blob/master/locus-api-core/src/main/java/locus/api/objects/geocaching/GeocachingData.kt
    @VisibleForTesting
    static Int toLocusType(final CacheType ct) {
        switch (ct) {
            case TRADITIONAL:
                return GeocachingData.CACHE_TYPE_TRADITIONAL
            case MULTI:
                return GeocachingData.CACHE_TYPE_MULTI
            case MYSTERY:
                return GeocachingData.CACHE_TYPE_MYSTERY
            case LETTERBOX:
                return GeocachingData.CACHE_TYPE_LETTERBOX
            case EVENT:
                return GeocachingData.CACHE_TYPE_EVENT
            case MEGA_EVENT:
                return GeocachingData.CACHE_TYPE_MEGA_EVENT
            case GIGA_EVENT:
                return GeocachingData.CACHE_TYPE_GIGA_EVENT
            case EARTH:
                return GeocachingData.CACHE_TYPE_EARTH
            case CITO:
                return GeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT
            case WEBCAM:
                return GeocachingData.CACHE_TYPE_WEBCAM
            case VIRTUAL:
                return GeocachingData.CACHE_TYPE_VIRTUAL
            case WHERIGO:
                return GeocachingData.CACHE_TYPE_WHERIGO
            case COMMUN_CELEBRATION:
                return GeocachingData.CACHE_TYPE_COMMUNITY_CELEBRATION
            case PROJECT_APE:
                return GeocachingData.CACHE_TYPE_PROJECT_APE
            case GCHQ:
                return GeocachingData.CACHE_TYPE_GC_HQ
            case GCHQ_CELEBRATION:
                return GeocachingData.CACHE_TYPE_GC_HQ_CELEBRATION
            case GPS_EXHIBIT:
                return GeocachingData.CACHE_TYPE_GPS_ADVENTURE
            case BLOCK_PARTY:
                return GeocachingData.CACHE_TYPE_GC_HQ_BLOCK_PARTY
            case LOCATIONLESS:
                return GeocachingData.CACHE_TYPE_LOCATIONLESS
            case ADVLAB:
                return GeocachingData.CACHE_TYPE_LAB_CACHE
// Benchmark, not c:geo cache type - removed from geocaching.com
//            case BENCHMARK:
//                return GeocachingData.CACHE_TYPE_BENCHMARK;
// Maze Exhibit, not supported c:geo cache type
//            case MAZE_EXHIBIT:
//                return GeocachingData.CACHE_TYPE_MAZE_EXHIBIT;
// Waymark, not supported c:geo cache type
//            case WAYMARK:
//                return GeocachingData.CACHE_TYPE_WAYMARK;
            // special types are mapped to CACHE_TYPE_UNDEFINED
            case USER_DEFINED:
            case UNKNOWN:
            case ALL:
                return GeocachingData.CACHE_TYPE_UNDEFINED
        }
        // special and unknown cache type
        return GeocachingData.CACHE_TYPE_UNDEFINED
    }

    @VisibleForTesting
    @SuppressWarnings("DuplicateBranchesInSwitch")
    static Int toLocusSize(final CacheSize cs) {
        switch (cs) {
            case NANO:
                return GeocachingData.CACHE_SIZE_MICRO; // used by OC only
            case MICRO:
                return GeocachingData.CACHE_SIZE_MICRO
            case SMALL:
                return GeocachingData.CACHE_SIZE_SMALL
            case REGULAR:
                return GeocachingData.CACHE_SIZE_REGULAR
            case LARGE:
                return GeocachingData.CACHE_SIZE_LARGE
            case VERY_LARGE:
                return GeocachingData.CACHE_SIZE_HUGE; // used by OC only
            case NOT_CHOSEN:
                return GeocachingData.CACHE_SIZE_NOT_CHOSEN
            case VIRTUAL:
                return GeocachingData.CACHE_SIZE_VIRTUAL
            case OTHER:
                return GeocachingData.CACHE_SIZE_OTHER
            case UNKNOWN:
                return GeocachingData.CACHE_SIZE_NOT_CHOSEN
        }
        // unknown cache size
        return GeocachingData.CACHE_SIZE_NOT_CHOSEN
    }

    @VisibleForTesting
    @SuppressWarnings("DuplicateBranchesInSwitch")
    static String toLocusWaypoint(final WaypointType wt) {
        switch (wt) {
            case FINAL:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL
            case OWN:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE
            case PARKING:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PARKING
            case PUZZLE:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_VIRTUAL_STAGE
            case STAGE:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE
            case TRAILHEAD:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_TRAILHEAD
            case WAYPOINT:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE
            case ORIGINAL:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE
            case GENERATED:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE
        }
        // unknown waypoint type
        return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE
    }

    @VisibleForTesting
    static ArrayList<GeocachingAttribute> toLocusAttributes(final List<String> attributes) {
        val loAttributes: ArrayList<GeocachingAttribute> = ArrayList<>()

        for (String attribute : attributes) {
            String rawAttribute = CacheAttribute.trimAttributeName(attribute)
            if (rawAttribute.isEmpty()) {
                continue
            }
            // translate to locus names
            switch (rawAttribute) {
                case "uv":
                    rawAttribute = "UV"
                    break
                case "dangerousanimals":
                    rawAttribute = "snakes"
                    break
                case "s_tool":
                    rawAttribute = "s-tool"
                    break
                case "abandonedbuilding":
                    rawAttribute = "AbandonedBuilding"
                    break
                case "touristok":
                    rawAttribute = "touristOK"
                    break
                case "bonuscache":
                    rawAttribute = "bonus"
                    break
                case "powertrail":
                    rawAttribute = "power"
                    break
                case "challengecache":
                    rawAttribute = "challenge"
                    break
                case "hqsolutionchecker":
                    rawAttribute = "checker"
                    break
                default:
                    // do nothing
                    break
            }
            val attributeUrl: String = "/" + rawAttribute + (CacheAttribute.isEnabled(attribute) ? "-yes." : "-no.")
            val ga: GeocachingAttribute = GeocachingAttribute(attributeUrl)
            // e.g. OC attributes - skip
            if (ga.getId() > 0) {
                loAttributes.add(ga)
            }
        }
        return loAttributes
    }
}
