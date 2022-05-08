package cgeo.geocaching.apps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import locus.api.android.ActionDisplayPoints;
import locus.api.android.ActionDisplayVarious;
import locus.api.android.objects.LocusVersion;
import locus.api.android.objects.PackPoints;
import locus.api.android.objects.VersionCode;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.geoData.Point;
import locus.api.objects.geocaching.GeocachingAttribute;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingWaypoint;
import org.apache.commons.collections4.CollectionUtils;

/**
 * for the Locus API:
 *
 * @see <a href="https://docs.locusmap.eu/doku.php?id=manual:advanced:locus_api:installation">Locus API</a>
 * @see <a href="https://github.com/asamm/locus-api/blob/master/locus-api-android-sample/src/main/java/com/asamm/locus/api/sample/utils/SampleCalls.kt">Sample Calls</a>
 */
public abstract class AbstractLocusApp extends AbstractApp {

    /**
     * Locus Version
     */
    private final LocusVersion lv;

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    protected AbstractLocusApp(@NonNull final String text, @NonNull final String intent) {
        super(text, intent);

        try (ContextLogger cLog = new ContextLogger(true, "AbstractLocusApp.init (" + this.getClass().getName() + ")")) {
            lv = LocusUtils.INSTANCE.getActiveVersion(CgeoApplication.getInstance());
            cLog.add("V:" + lv);
            if (lv == null) { // locus not installed
                Log.w("Couldn't get active Locus version");
            }
        }
    }

    @Override
    public boolean isInstalled() {
        return lv != null;
    }

    /**
     * Display a list of caches / waypoints in Locus
     *
     * @param objectsToShow      which caches/waypoints to show
     * @param withCacheWaypoints Whether to give waypoints of caches to Locus or not
     */
    protected void showInLocus(final List<?> objectsToShow, final boolean withCacheWaypoints, final boolean export,
                               final Context context) {
        if (CollectionUtils.isEmpty(objectsToShow)) {
            return;
        }

        if (!isInstalled()) {
            return;
        }

        final boolean withCacheDetails = objectsToShow.size() < 200;
        final PackPoints pd = new PackPoints("c:geo");
        for (final Object o : objectsToShow) {
            Point p = null;
            // get icon and Point
            if (o instanceof Geocache) {
                p = getCachePoint((Geocache) o, withCacheWaypoints, withCacheDetails);
            } else if (o instanceof cgeo.geocaching.models.Waypoint) {
                p = getWaypointPoint((cgeo.geocaching.models.Waypoint) o);
            }
            if (p != null) {
                pd.addPoint(p);
            }
        }

        if (pd.getPoints().length == 0) {
            return;
        }

        if (pd.getPoints().length <= 1000) {
            try {
                ActionDisplayPoints.INSTANCE.sendPack(context, pd, export ? ActionDisplayVarious.ExtraAction.IMPORT : ActionDisplayVarious.ExtraAction.CENTER);
            } catch (final RequiredVersionMissingException e) {
                Log.e("AbstractLocusApp.showInLocus: problem in sendPack", e);
            }
        } else {
            final File externalDir = new File(context.getExternalFilesDir(null), "showIn.locus");
            final File file = new File(externalDir.toURI());

            final ArrayList<PackPoints> data = new ArrayList<>();
            data.add(pd);

            try {
                if (lv.isVersionValid(VersionCode.UPDATE_15)) {
                    // send file via FileProvider, you don't need WRITE_EXTERNAL_STORAGE permission for this
                    final Uri uri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file);
                    ActionDisplayPoints.INSTANCE.sendPacksFile(context, lv, data, file, uri, export ? ActionDisplayVarious.ExtraAction.IMPORT : ActionDisplayVarious.ExtraAction.CENTER);
                } else {
                    // send file old way, you need WRITE_EXTERNAL_STORAGE permission for this
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        ActionDisplayPoints.INSTANCE.sendPacksFile(context, lv, data, file, null, ActionDisplayVarious.ExtraAction.CENTER);
                    } else {
                        ActivityMixin.showToast(context, getString(R.string.storage_permission_needed));
                    }
                }
            } catch (final RequiredVersionMissingException e) {
                Log.e("AbstractLocusApp.showInLocus: problem in sendPacksFile", e);
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
    @Nullable
    private static Point getCachePoint(final Geocache cache, final boolean withWaypoints, final boolean withCacheDetails) {
        if (cache == null || cache.getCoords() == null) {
            return null;
        }

        // create one simple point with location
        final Location loc = new Location();
        loc.setProvider("cgeo");
        loc.setLatitude(cache.getCoords().getLatitude());
        loc.setLongitude(cache.getCoords().getLongitude());

        final Point wpt = new Point(cache.getName(), loc);
        // generate new data
        final GeocachingData gcData = new GeocachingData();

        // fill data with variables
        gcData.setCacheID(cache.getGeocode());
        gcData.setName(cache.getName());

        // rest is optional so fill as you want - should work
        gcData.setOwner(cache.getOwnerUserId());
        gcData.setPlacedBy(cache.getOwnerDisplayName());
        if (cache.getDifficulty() > 0) {
            gcData.setDifficulty(cache.getDifficulty());
        }
        if (cache.getTerrain() > 0) {
            gcData.setTerrain(cache.getTerrain());
        }
        gcData.setContainer(toLocusSize(cache.getSize()));
        final ArrayList<GeocachingAttribute> attributes = toLocusAttributes(cache.getAttributes());
        if (!attributes.isEmpty()) {
            gcData.setAttributes(attributes);
        }

        gcData.setAvailable(!cache.isDisabled());
        gcData.setArchived(cache.isArchived());
        gcData.setPremiumOnly(cache.isPremiumMembersOnly());
        final Date hiddenDate = cache.getHiddenDate();
        if (hiddenDate != null) {
            gcData.setDateHidden(hiddenDate.getTime());
        }
        gcData.setType(toLocusType(cache.getType()));
        gcData.setFound(cache.isFound());

        if (withWaypoints && cache.hasWaypoints()) {
            for (final cgeo.geocaching.models.Waypoint waypoint : cache.getWaypoints()) {
                if (waypoint == null) {
                    continue;
                }

                final GeocachingWaypoint gcWpt = new GeocachingWaypoint();
                gcWpt.setCode(waypoint.getLookup());
                gcWpt.setName(waypoint.getName());
                gcWpt.setDesc(waypoint.getNote());
                gcWpt.setType(toLocusWaypoint(waypoint.getWaypointType()));

                final Geopoint waypointCoords = waypoint.getCoords();
                if (waypointCoords != null) {
                    gcWpt.setLon(waypointCoords.getLongitude());
                    gcWpt.setLat(waypointCoords.getLatitude());
                }

                gcData.getWaypoints().add(gcWpt);
            }
        }

        // Other properties of caches. When there are many caches to be displayed
        // in Locus, using these properties can lead to Exceptions in Locus.
        // Should not be used if caches count > 200
        if (withCacheDetails) {
            gcData.setDescriptions(
                    cache.getShortDescription(), true,
                    cache.getDescription(), true);
            gcData.setEncodedHints(cache.getHint());
        }

        // set data and return point
        wpt.setGcData(gcData);
        return wpt;
    }

    /**
     * This method constructs a {@code Point} for displaying in Locus
     *
     * @return null, when the {@code Point} could not be constructed
     */
    @Nullable
    private static Point getWaypointPoint(final cgeo.geocaching.models.Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }
        final Geopoint coordinates = waypoint.getCoords();
        if (coordinates == null) {
            return null;
        }

        // create one simple point with location
        final Location loc = new Location();
        loc.setProvider("cgeo");
        loc.setLatitude(coordinates.getLatitude());
        loc.setLongitude(coordinates.getLongitude());

        final Point p = new Point(waypoint.getName(), loc);
        p.setParameterDescription(
                "Name: " + waypoint.getName() +
                        "<br />Note: " + waypoint.getNote() +
                        "<br />UserNote: " + waypoint.getUserNote() +
                        "<br /><br /> <a href=\"" + waypoint.getUrl() + "\">" + waypoint.getGeocode() + "</a>"
        );

        return p;
    }

    // https://github.com/asamm/locus-api/blob/master/locus-api-core/src/main/java/locus/api/objects/geocaching/GeocachingData.kt
    @VisibleForTesting
    static int toLocusType(final CacheType ct) {
        switch (ct) {
            case TRADITIONAL:
                return GeocachingData.CACHE_TYPE_TRADITIONAL;
            case MULTI:
                return GeocachingData.CACHE_TYPE_MULTI;
            case MYSTERY:
                return GeocachingData.CACHE_TYPE_MYSTERY;
            case LETTERBOX:
                return GeocachingData.CACHE_TYPE_LETTERBOX;
            case EVENT:
                return GeocachingData.CACHE_TYPE_EVENT;
            case MEGA_EVENT:
                return GeocachingData.CACHE_TYPE_MEGA_EVENT;
            case GIGA_EVENT:
                return GeocachingData.CACHE_TYPE_GIGA_EVENT;
            case EARTH:
                return GeocachingData.CACHE_TYPE_EARTH;
            case CITO:
                return GeocachingData.CACHE_TYPE_CACHE_IN_TRASH_OUT;
            case WEBCAM:
                return GeocachingData.CACHE_TYPE_WEBCAM;
            case VIRTUAL:
                return GeocachingData.CACHE_TYPE_VIRTUAL;
            case WHERIGO:
                return GeocachingData.CACHE_TYPE_WHERIGO;
            case COMMUN_CELEBRATION:
                return GeocachingData.CACHE_TYPE_COMMUNITY_CELEBRATION;
            case PROJECT_APE:
                return GeocachingData.CACHE_TYPE_PROJECT_APE;
            case GCHQ:
                return GeocachingData.CACHE_TYPE_GC_HQ;
            case GCHQ_CELEBRATION:
                return GeocachingData.CACHE_TYPE_GC_HQ_CELEBRATION;
            case GPS_EXHIBIT:
                return GeocachingData.CACHE_TYPE_GPS_ADVENTURE;
            case BLOCK_PARTY:
                return GeocachingData.CACHE_TYPE_GC_HQ_BLOCK_PARTY;
            case LOCATIONLESS:
                return GeocachingData.CACHE_TYPE_LOCATIONLESS;
// Benchmark, not published over API from Locus (state: locus-api-android:0.9.21)
//            case BENCHMARK:
//                return GeocachingData.CACHE_TYPE_BENCHMARK;
// Maze Exhibit, not published over API from Locus (state: locus-api-android:0.9.21)
//            case MAZE_EXHIBIT:
//                return GeocachingData.CACHE_TYPE_MAZE_EXHIBIT;
// Waymark, not published over API from Locus (state: locus-api-android:0.9.21)
//            case WAYMARK:
//                return GeocachingData.CACHE_TYPE_WAYMARK;
// Lab cache, not published over API from Locus (state: locus-api-android:0.9.21)
//            case LAB_CACHE:
//                return GeocachingData.CACHE_TYPE_LAB_CACHE;
            // special map types to CACHE_TYPE_UNDEFINED
            case USER_DEFINED:
            case UNKNOWN:
            case ALL:
                return GeocachingData.CACHE_TYPE_UNDEFINED;
        }
        // special and unknown cache type
        return GeocachingData.CACHE_TYPE_UNDEFINED;
    }

    @VisibleForTesting
    @SuppressWarnings("DuplicateBranchesInSwitch")
    static int toLocusSize(final CacheSize cs) {
        switch (cs) {
            case NANO:
                return GeocachingData.CACHE_SIZE_MICRO; // used by OC only
            case MICRO:
                return GeocachingData.CACHE_SIZE_MICRO;
            case SMALL:
                return GeocachingData.CACHE_SIZE_SMALL;
            case REGULAR:
                return GeocachingData.CACHE_SIZE_REGULAR;
            case LARGE:
                return GeocachingData.CACHE_SIZE_LARGE;
            case VERY_LARGE:
                return GeocachingData.CACHE_SIZE_HUGE; // used by OC only
            case NOT_CHOSEN:
                return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
            case VIRTUAL:
                return GeocachingData.CACHE_SIZE_OTHER;
            case OTHER:
                return GeocachingData.CACHE_SIZE_OTHER;
            case UNKNOWN:
                return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
        }
        // unknown cache size
        return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
    }

    @NonNull
    @VisibleForTesting
    @SuppressWarnings("DuplicateBranchesInSwitch")
    static String toLocusWaypoint(final WaypointType wt) {
        switch (wt) {
            case FINAL:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_FINAL;
            case OWN:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE;
            case PARKING:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PARKING;
            case PUZZLE:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_VIRTUAL_STAGE;
            case STAGE:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PHYSICAL_STAGE;
            case TRAILHEAD:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_TRAILHEAD;
            case WAYPOINT:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
            case ORIGINAL:
                return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
        }
        // unknown waypoint type
        return GeocachingWaypoint.CACHE_WAYPOINT_TYPE_REFERENCE;
    }

    @NonNull
    @VisibleForTesting
    static ArrayList<GeocachingAttribute> toLocusAttributes(final List<String> attributes) {
        final ArrayList<GeocachingAttribute> loAttributes = new ArrayList<>();

        for (String attribute : attributes) {
            String rawAttribute = CacheAttribute.trimAttributeName(attribute);
            if (rawAttribute.equals("")) {
                continue;
            }
            // translate to locus names
            switch (rawAttribute) {
                case "uv":
                    rawAttribute = "UV";
                    break;
                case "dangerousanimals":
                    rawAttribute = "snakes";
                    break;
                case "s_tool":
                    rawAttribute = "s-tool";
                    break;
                case "abandonedbuilding":
                    rawAttribute = "AbandonedBuilding";
                    break;
                case "touristok":
                    rawAttribute = "touristOK";
                    break;
                case "bonuscache":
                    rawAttribute = "bonus";
                    break;
                case "powertrail":
                    rawAttribute = "power";
                    break;
                case "challengecache":
                    rawAttribute = "challenge";
                    break;
                case "hqsolutionchecker":
                    rawAttribute = "checker";
                    break;
                default:
                    // do nothing
                    break;
            }
            final String attributeUrl = "/" + rawAttribute + (CacheAttribute.isEnabled(attribute) ? "-yes." : "-no.");
            final GeocachingAttribute ga = new GeocachingAttribute(attributeUrl);
            // e.g. OC attributes - skip
            if (ga.getId() > 0) {
                loAttributes.add(ga);
            }
        }
        return loAttributes;
    }
}
