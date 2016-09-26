package cgeo.geocaching.apps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.Log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.app.Activity;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import locus.api.android.ActionDisplay;
import locus.api.android.ActionDisplayPoints;
import locus.api.android.objects.PackWaypoints;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Waypoint;
import locus.api.objects.geocaching.GeocachingData;
import locus.api.objects.geocaching.GeocachingWaypoint;

/**
 * for the Locus API:
 *
 * @see <a href="http://docs.locusmap.eu/doku.php?id=manual:advanced:locus_api:installation">Locus API</a>
 * @see <a href="https://bitbucket.org/asamm/locus-api-android-sample/src/1fad202e6166239b6e424f03bac79f0000f8eb6d/src/main/java/menion/android/locus/api/utils/SampleCalls.java?at=default&fileviewer=file-view-default">Sample Calls</a>
 */
public abstract class AbstractLocusApp extends AbstractApp {

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    protected AbstractLocusApp(@NonNull final String text, @NonNull final String intent) {
        super(text, intent);
    }

    @Override
    public boolean isInstalled() {
        return LocusUtils.getActiveVersion(CgeoApplication.getInstance()) != null;
    }

    /**
     * Display a list of caches / waypoints in Locus
     *
     * @param objectsToShow
     *            which caches/waypoints to show
     * @param withCacheWaypoints
     *            Whether to give waypoints of caches to Locus or not
     */
    protected static boolean showInLocus(final List<?> objectsToShow, final boolean withCacheWaypoints, final boolean export,
            final Activity activity) {
        if (objectsToShow == null || objectsToShow.isEmpty()) {
            return false;
        }

        final boolean withCacheDetails = objectsToShow.size() < 200;
        final PackWaypoints pd = new PackWaypoints("c:geo");
        for (final Object o : objectsToShow) {
            Waypoint p = null;
            // get icon and Point
            if (o instanceof Geocache) {
                p = getCachePoint((Geocache) o, withCacheWaypoints, withCacheDetails);
            } else if (o instanceof cgeo.geocaching.models.Waypoint) {
                p = getWaypointPoint((cgeo.geocaching.models.Waypoint) o);
            }
            if (p != null) {
                pd.addWaypoint(p);
            }
        }

        if (pd.getWaypoints().isEmpty()) {
            return false;
        }

        if (pd.getWaypoints().size() <= 1000) {
            try {
                ActionDisplayPoints.sendPack(activity, pd, export ? ActionDisplay.ExtraAction.IMPORT : ActionDisplay.ExtraAction.CENTER);
            } catch (final RequiredVersionMissingException e) {
                Log.e("AbstractLocusApp.showInLocus: problem in sendPack", e);
                return false;
            }
        } else {
            final File externalDir = Environment.getExternalStorageDirectory();
            if (externalDir == null || !externalDir.exists()) {
                Log.w("AbstractLocusApp.showInLocus: problem with obtain of External dir");
                return false;
            }

            String filePath = externalDir.getAbsolutePath();
            if (!filePath.endsWith("/")) {
                filePath += "/";
            }
            filePath += "Android/data/menion.android.locus.api/files/showIn.locus";

            final ArrayList<PackWaypoints> data = new ArrayList<>();
            data.add(pd);

            try {
                ActionDisplayPoints.sendPacksFile(activity, data, filePath, export ? ActionDisplay.ExtraAction.IMPORT : ActionDisplay.ExtraAction.CENTER);
            } catch (final RequiredVersionMissingException e) {
                Log.e("AbstractLocusApp.showInLocus: problem in sendPacksFile", e);
                return false;
            }
        }

        return true;
    }

    /**
     * This method constructs a {@code Point} for displaying in Locus
     *
     * @param withWaypoints
     *            whether to give waypoints to Locus or not
     * @param withCacheDetails
     *            whether to give cache details (description, hint) to Locus or not
     *            should be false for all if more than 200 Caches are transferred
     * @return null, when the {@code Point} could not be constructed
     */
    @Nullable
    private static Waypoint getCachePoint(final Geocache cache, final boolean withWaypoints, final boolean withCacheDetails) {
        if (cache == null || cache.getCoords() == null) {
            return null;
        }

        // create one simple point with location
        final Location loc = new Location("cgeo");
        loc.setLatitude(cache.getCoords().getLatitude());
        loc.setLongitude(cache.getCoords().getLongitude());

        final Waypoint wpt = new Waypoint(cache.getName(), loc);
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
        final int container = toLocusSize(cache.getSize());
        if (container != NO_LOCUS_ID) {
            gcData.setContainer(container);
        }

        gcData.setAvailable(!cache.isDisabled());
        gcData.setArchived(cache.isArchived());
        gcData.setPremiumOnly(cache.isPremiumMembersOnly());
        final Date hiddenDate = cache.getHiddenDate();
        if (hiddenDate != null) {
            gcData.setDateHidden(hiddenDate.getTime());
        }
        final int type = toLocusType(cache.getType());
        if (type != NO_LOCUS_ID) {
            gcData.setType(type);
        }
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
                gcWpt.setType(GeocachingWaypoint.CACHE_WAYPOINT_TYPE_PARKING);

                final String locusWpId = toLocusWaypoint(waypoint.getWaypointType());
                if (locusWpId != null) {
                    gcWpt.setType(locusWpId);
                }

                final Geopoint waypointCoords = waypoint.getCoords();
                if (waypointCoords != null) {
                    gcWpt.setLon(waypointCoords.getLongitude());
                    gcWpt.setLat(waypointCoords.getLatitude());
                }

                gcData.waypoints.add(gcWpt);
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
        wpt.gcData = gcData;
        return wpt;
    }

    /**
     * This method constructs a {@code Point} for displaying in Locus
     *
     * @return null, when the {@code Point} could not be constructed
     */
    @Nullable
    private static Waypoint getWaypointPoint(final cgeo.geocaching.models.Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }
        final Geopoint coordinates = waypoint.getCoords();
        if (coordinates == null) {
            return null;
        }

        // create one simple point with location
        final Location loc = new Location("cgeo");
        loc.setLatitude(coordinates.getLatitude());
        loc.setLongitude(coordinates.getLongitude());

        final Waypoint p = new Waypoint(waypoint.getName(), loc);
        //TODO: not supported by new Locus API (or I haven't found it yet, pstorch)
        //p.setDescription("<a href=\"" + waypoint.getUrl() + "\">" + waypoint.getGeocode() + "</a>");

        return p;
    }

    private static final int NO_LOCUS_ID = -1;

    private static int toLocusType(final CacheType ct) {
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
            case LOSTANDFOUND:
                return GeocachingData.CACHE_TYPE_LF_EVENT;
            case PROJECT_APE:
                return GeocachingData.CACHE_TYPE_PROJECT_APE;
            case GPS_EXHIBIT:
                return GeocachingData.CACHE_TYPE_GPS_ADVENTURE;
            default:
                return NO_LOCUS_ID;
        }
    }

    private static int toLocusSize(final CacheSize cs) {
        switch (cs) {
            case MICRO:
                return GeocachingData.CACHE_SIZE_MICRO;
            case SMALL:
                return GeocachingData.CACHE_SIZE_SMALL;
            case REGULAR:
                return GeocachingData.CACHE_SIZE_REGULAR;
            case LARGE:
                return GeocachingData.CACHE_SIZE_LARGE;
            case NOT_CHOSEN:
                return GeocachingData.CACHE_SIZE_NOT_CHOSEN;
            case OTHER:
                return GeocachingData.CACHE_SIZE_OTHER;
            default:
                return NO_LOCUS_ID;
        }
    }

    @Nullable
    private static String toLocusWaypoint(final WaypointType wt) {
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
            default:
                return null;
        }
    }
}
