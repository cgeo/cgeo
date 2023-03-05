package cgeo.geocaching.models;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.IFavoriteCapability;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.WatchListCapability;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogCacheActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogContext;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.DataStore.StorageLocation;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.EventTimeParser;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LazyInitializedList;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.utils.Formatter.generateShortGeocode;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * Internal representation of a "cache"
 */
public class Geocache implements IWaypoint {

    private long updated = 0;
    private long detailedUpdate = 0;
    private long visitedDate = 0;
    @NonNull
    private Set<Integer> lists = new HashSet<>();
    private boolean detailed = false;

    @NonNull
    private String geocode = "";
    private String cacheId = "";
    private String guid = "";
    @NonNull
    private CacheType cacheType = CacheType.UNKNOWN;
    private String name = "";
    private String ownerDisplayName = "";
    private String ownerGuid = "";
    private String ownerUserId = "";
    private int assignedEmoji = 0;
    private int alcMode = 0;

    @Nullable
    private Date hidden = null;
    private Date lastFound = null;
    /**
     * lazy initialized
     */
    private String hint = null;
    @NonNull private CacheSize size = CacheSize.UNKNOWN;
    private float difficulty = 0;
    private float terrain = 0;
    private Float direction = null;
    private Float distance = null;
    /**
     * lazy initialized
     */
    private String location = null;
    private Geopoint coords = null;
    private final PersonalNote personalNote = new PersonalNote();
    /**
     * lazy initialized
     */
    private String shortdesc = null;
    /**
     * lazy initialized
     */
    private String description = null;
    private Boolean disabled = null;
    private Boolean archived = null;
    private Boolean premiumMembersOnly = null;
    private Boolean found = null;
    private Boolean didNotFound = null;
    private Boolean favorite = null;
    private Boolean onWatchlist = null;
    private int watchlistCount = -1; // valid numbers are larger than -1
    private int favoritePoints = -1; // valid numbers are larger than -1
    private float rating = 0; // valid ratings are larger than zero
    // FIXME: this makes no sense to favor this over the other. 0 should not be a special case here as it is
    // in the range of acceptable values. This is probably the case at other places (rating etc.) too.
    private int votes = 0;
    private float myVote = 0.0f; // valid ratings are larger than zero
    private int inventoryItems = -1;
    private final LazyInitializedList<String> attributes = new LazyInitializedList<String>() {
        @Override
        public List<String> call() {
            return inDatabase() ? DataStore.loadAttributes(geocode) : new LinkedList<>();
        }
    };
    private final LazyInitializedList<Waypoint> waypoints = new LazyInitializedList<Waypoint>() {
        @Override
        public List<Waypoint> call() {
            return inDatabase() ? DataStore.loadWaypoints(geocode) : new LinkedList<>();
        }
    };
    private List<Image> spoilers = null;

    private List<Trackable> inventory = null;
    @NonNull
    private Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
    private Boolean userModifiedCoords = null;
    // temporary values
    private boolean statusChecked = false;
    @NonNull
    private String directionImg = "";
    private String nameForSorting;
    private final EnumSet<StorageLocation> storageLocation = EnumSet.of(StorageLocation.HEAP);
    private boolean finalDefined = false;
    private boolean logPasswordRequired = false;
    private boolean preventWaypointsFromNote = Settings.isGlobalWpExtractionDisabled();

    private Boolean hasLogOffline = null;
    private OfflineLogEntry offlineLog = null;

    private EventTimesInMin eventTimesInMin = new EventTimesInMin();

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private Handler changeNotificationHandler = null;

    private CacheVariableList variables;

    //transient field, used for online search by finder only
    private String finder = null;

    public void setChangeNotificationHandler(@Nullable final Handler newNotificationHandler) {
        changeNotificationHandler = newNotificationHandler;
    }

    /**
     * Sends a change notification to interested parties
     */
    private void notifyChange() {
        if (changeNotificationHandler != null) {
            changeNotificationHandler.sendEmptyMessage(0);
        }
    }

    /**
     * Gather missing information for new Geocache object from the stored Geocache object.
     * This is called in the new Geocache parsed from website to set information not yet
     * parsed.
     *
     * @param other the other version, or null if non-existent
     * @return true if this cache is "equal" to the other version
     */
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public boolean gatherMissingFrom(@Nullable final Geocache other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }

        updated = System.currentTimeMillis();
        //storageLocation.addAll(other.getStorageLocation()); // seems correct but has side effects / failing tests

        // if parsed cache is not yet detailed and stored is, the information of
        // the parsed cache will be overwritten
        if (!detailed && other.detailed) {
            detailed = true;
            detailedUpdate = other.detailedUpdate;
            // boolean values must be enumerated here. Other types are assigned outside this if-statement
            finalDefined = other.finalDefined;

            if (StringUtils.isBlank(getHint())) {
                hint = other.getHint();
            }
            if (StringUtils.isBlank(getShortDescription())) {
                shortdesc = other.getShortDescription();
            }
            if (attributes.isEmpty() && other.attributes != null) {
                attributes.addAll(other.attributes);
            }
        }

        if (premiumMembersOnly == null) {
            premiumMembersOnly = other.premiumMembersOnly;
        }
        if (found == null) {
            found = other.found;
        }
        if (didNotFound == null) {
            didNotFound = other.didNotFound;
        }
        if (disabled == null) {
            disabled = other.disabled;
        }
        if (favorite == null) {
            favorite = other.favorite;
        }
        if (archived == null) {
            archived = other.archived;
        }
        if (onWatchlist == null) {
            onWatchlist = other.onWatchlist;
        }
        if (hasLogOffline == null) {
            hasLogOffline = other.hasLogOffline;
        }
        if (visitedDate == 0) {
            visitedDate = other.visitedDate;
        }
        if (lists.isEmpty()) {
            lists.addAll(other.lists);
        }
        if (StringUtils.isBlank(geocode)) {
            geocode = other.geocode;
        }
        if (StringUtils.isBlank(cacheId)) {
            cacheId = other.cacheId;
        }
        if (StringUtils.isBlank(guid)) {
            guid = other.guid;
        }
        if (cacheType == CacheType.UNKNOWN) {
            cacheType = other.cacheType;
        }
        if (StringUtils.isBlank(name)) {
            name = other.name;
        }
        if (StringUtils.isBlank(ownerDisplayName)) {
            ownerDisplayName = other.ownerDisplayName;
        }
        if (StringUtils.isBlank(ownerUserId)) {
            ownerUserId = other.ownerUserId;
        }
        if (hidden == null) {
            hidden = other.hidden;
        }
        if (lastFound == null) {
            lastFound = other.lastFound;
        }
        if (size == CacheSize.UNKNOWN) {
            size = other.size;
        }
        if (difficulty == 0) {
            difficulty = other.difficulty;
        }
        if (terrain == 0) {
            terrain = other.terrain;
        }
        if (direction == null) {
            direction = other.direction;
        }
        if (distance == null) {
            distance = other.distance;
        }
        if (StringUtils.isBlank(getLocation())) {
            location = other.getLocation();
        }

        personalNote.gatherMissingDataFrom(other.personalNote);

        if (StringUtils.isBlank(getDescription())) {
            description = other.getDescription();
        }
        if (favoritePoints == -1) {
            favoritePoints = other.favoritePoints;
        }
        if (rating == 0) {
            rating = other.rating;
        }
        if (votes == 0) {
            votes = other.votes;
        }
        if (myVote == 0) {
            myVote = other.myVote;
        }
        if (waypoints.isEmpty()) {
            this.setWaypoints(other.waypoints, false);
        } else {
            final List<Waypoint> newPoints = new ArrayList<>(waypoints);
            Waypoint.mergeWayPoints(newPoints, other.waypoints, false);
            this.setWaypoints(newPoints, false);
        }
        if (spoilers == null) {
            spoilers = other.spoilers;
        }
        if (inventory == null) {
            // If inventoryItems is 0, it can mean both
            // "don't know" or "0 items". Since we cannot distinguish
            // them here, only populate inventoryItems from
            // old data when we have to do it for inventory.
            setInventory(other.inventory);
        }
        if (logCounts.isEmpty()) {
            logCounts = other.logCounts;
        }


        final boolean takeOverUserModCoords = !hasUserModifiedCoords();
        if (userModifiedCoords == null) {
            userModifiedCoords = other.userModifiedCoords;
        }

        if (takeOverUserModCoords && other.hasUserModifiedCoords()) {
            final Waypoint original = other.getOriginalWaypoint();
            if (original != null) {
                original.setCoords(getCoords());
            }
            setCoords(other.getCoords());
        } else {
            if (coords == null) {
                coords = other.coords;
            }
        }
        // if cache has ORIGINAL type waypoint ... it is considered that it has modified coordinates, otherwise not
        if (getOriginalWaypoint() != null) {
            userModifiedCoords = true;
        }

        if (!preventWaypointsFromNote) {
            preventWaypointsFromNote = other.preventWaypointsFromNote;
        }

        if (assignedEmoji == 0) {
            assignedEmoji = other.assignedEmoji;
        }
        if (finder == null) {
            finder = other.finder;
        }

        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
        return isEqualTo(other);
    }

    /**
     * Returns the Original Waypoint if exists
     */
    @Nullable
    public Waypoint getOriginalWaypoint() {
        return getFirstMatchingWaypoint(wpt -> wpt.getWaypointType() == WaypointType.ORIGINAL);
    }

    /**
     * Returns the first found Waypoint matching the given condition
     */
    @Nullable
    public Waypoint getFirstMatchingWaypoint(final Func1<Waypoint, Boolean> condition) {
        for (final Waypoint wpt : waypoints) {
            if (wpt != null && condition.call(wpt)) {
                return wpt;
            }
        }
        return null;
    }

    /**
     * Compare two caches quickly. For map and list fields only the references are compared !
     *
     * @param other the other cache to compare this one to
     * @return true if both caches have the same content
     */
    @SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
    private boolean isEqualTo(final Geocache other) {
        return detailed == other.detailed &&
                StringUtils.equalsIgnoreCase(geocode, other.geocode) &&
                StringUtils.equalsIgnoreCase(name, other.name) &&
                cacheType.equals(other.cacheType) &&
                size == other.size &&
                Objects.equals(found, other.found) &&
                Objects.equals(didNotFound, other.didNotFound) &&
                Objects.equals(premiumMembersOnly, other.premiumMembersOnly) &&
                difficulty == other.difficulty &&
                terrain == other.terrain &&
                Objects.equals(coords, other.coords) &&
                Objects.equals(disabled, other.disabled) &&
                Objects.equals(archived, other.archived) &&
                Objects.equals(lists, other.lists) &&
                StringUtils.equalsIgnoreCase(ownerDisplayName, other.ownerDisplayName) &&
                StringUtils.equalsIgnoreCase(ownerUserId, other.ownerUserId) &&
                StringUtils.equalsIgnoreCase(getDescription(), other.getDescription()) &&
                Objects.equals(personalNote, other.personalNote) &&
                StringUtils.equalsIgnoreCase(getShortDescription(), other.getShortDescription()) &&
                StringUtils.equalsIgnoreCase(getLocation(), other.getLocation()) &&
                Objects.equals(favorite, other.favorite) &&
                favoritePoints == other.favoritePoints &&
                Objects.equals(onWatchlist, other.onWatchlist) &&
                Objects.equals(hidden, other.hidden) &&
                Objects.equals(lastFound, other.lastFound) &&
                StringUtils.equalsIgnoreCase(guid, other.guid) &&
                StringUtils.equalsIgnoreCase(getHint(), other.getHint()) &&
                StringUtils.equalsIgnoreCase(cacheId, other.cacheId) &&
                Objects.equals(direction, other.direction) &&
                Objects.equals(distance, other.distance) &&
                rating == other.rating &&
                votes == other.votes &&
                myVote == other.myVote &&
                inventoryItems == other.inventoryItems &&
                attributes.equals(other.attributes) &&
                waypoints.equals(other.waypoints) &&
                Objects.equals(spoilers, other.spoilers) &&
                Objects.equals(inventory, other.inventory) &&
                Objects.equals(logCounts, other.logCounts) &&
                Objects.equals(hasLogOffline, other.hasLogOffline) &&
                finalDefined == other.finalDefined &&
                Objects.equals(finder, other.finder);
    }

    public boolean hasTrackables() {
        return inventoryItems > 0;
    }

    public boolean canBeAddedToCalendar() {
        // Is event type with event date set?
        return isEventCache() && hidden != null;
    }

    public boolean isPastEvent() {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assert hidden != null; // Android Studio compiler issue
        return hidden.compareTo(cal.getTime()) < 0;
    }

    public boolean isEventCache() {
        return cacheType.isEvent();
    }

    // returns whether the current cache is a future event, for which
    // the user has logged a "will attend", but no "attended" yet
    public boolean hasWillAttendForFutureEvent() {
        if (!isEventCache()) {
            return false;
        }
        final Date eventDate = getHiddenDate();
        final boolean expired = CalendarUtils.isPastEvent(this);
        if (eventDate == null || expired) {
            return false;
        }

        boolean willAttend = false;
        final List<LogEntry> logs = getLogs();
        for (final LogEntry logEntry : logs) {
            final LogType logType = logEntry.logType;
            if (logType == LogType.ATTENDED) {
                return false;
            } else if (logType == LogType.WILL_ATTEND && logEntry.isOwn()) {
                willAttend = true;
            }
        }
        return willAttend;
    }

    public void logVisit(@NonNull final Activity fromActivity) {
        if (!getConnector().canLog(this)) {
            ActivityMixin.showToast(fromActivity, fromActivity.getString(R.string.err_cannot_log_visit));
            return;
        }
        fromActivity.startActivity(LogCacheActivity.getLogCacheIntent(fromActivity, cacheId, geocode));
    }

    public boolean hasLogOffline() {
        return BooleanUtils.isTrue(hasLogOffline);
    }

    public void setHasLogOffline(final boolean hasLogOffline) {
        this.hasLogOffline = hasLogOffline;
    }

    public void logOffline(@NonNull final Activity fromActivity, @NonNull final LogType logType, @NonNull final ReportProblemType reportProblem) {
        final boolean mustIncludeSignature = StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature();
        final String initial = mustIncludeSignature ? LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(this, null, true)) : "";

        logOffline(fromActivity, new OfflineLogEntry.Builder<>()
                .setLog(initial)
                .setDate(Calendar.getInstance().getTimeInMillis())
                .setLogType(logType)
                .setReportProblem(reportProblem)
                .build()
        );
    }

    public void logOffline(@NonNull final Activity fromActivity, @NonNull final OfflineLogEntry logEntry) {

        if (logEntry.logType == LogType.UNKNOWN) {
            return;
        }

        if (!isOffline()) {
            getLists().add(StoredList.STANDARD_LIST_ID);
            DataStore.saveCache(this, LoadFlags.SAVE_ALL);
        }

        final boolean status = DataStore.saveLogOffline(geocode, logEntry);

        final Resources res = fromActivity.getResources();
        if (status) {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.info_log_saved));
            DataStore.saveVisitDate(geocode, logEntry.date);
            hasLogOffline = Boolean.TRUE;
            offlineLog = logEntry;
            notifyChange();
        } else {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.err_log_post_failed));
        }
    }

    /**
     * Get the Offline Log entry if any.
     *
     * @return The Offline LogEntry
     */
    @Nullable
    public OfflineLogEntry getOfflineLog() {
        if (!BooleanUtils.isFalse(hasLogOffline) && offlineLog == null) {
            offlineLog = DataStore.loadLogOffline(geocode);
            setHasLogOffline(offlineLog != null);
        }
        return offlineLog;
    }

    /**
     * Get the Offline Log entry if any.
     *
     * @return The Offline LogEntry else Null
     */
    @Nullable
    public LogType getOfflineLogType() {
        final LogEntry offlineLog = getOfflineLog();
        if (offlineLog == null) {
            return null;
        }
        return offlineLog.logType;
    }

    /**
     * Drop offline log for a given geocode.
     */
    public void clearOfflineLog() {
        DataStore.clearLogOffline(geocode);
        setHasLogOffline(false);
        notifyChange();
    }

    @NonNull
    public List<LogType> getPossibleLogTypes() {
        return getConnector().getPossibleLogTypes(this);
    }

    public void openInBrowser(final Context context) {
        ShareUtils.openUrl(context, getUrl(), true);
    }

    public void openLogInBrowser(final Context context, @NonNull final LogEntry logEntry) {
        ShareUtils.openUrl(context, getConnector().getCacheLogUrl(this, logEntry), true);
    }

    public void openCreateNewLogInBrowser(final Context context) {
        ShareUtils.openUrl(context, getConnector().getCacheCreateNewLogUrl(this), true);
    }

    @NonNull
    private IConnector getConnector() {
        return ConnectorFactory.getConnector(this);
    }

    public boolean supportsRefresh() {
        return getConnector() instanceof ISearchByGeocode && !InternalConnector.getInstance().equals(getConnector());
    }

    public boolean supportsWatchList() {
        final IConnector connector = getConnector();
        return (connector instanceof WatchListCapability) && ((WatchListCapability) connector).canAddToWatchList(this);
    }

    public boolean supportsFavoritePoints() {
        final IConnector connector = getConnector();
        return (connector instanceof IFavoriteCapability) && ((IFavoriteCapability) connector).supportsFavoritePoints(this);
    }

    public boolean supportsLogging() {
        return getConnector().supportsLogging();
    }

    public boolean supportsLoggingOnline() {
        return getConnector().getCacheCreateNewLogUrl(this) != null;
    }


    public boolean supportsLogImages() {
        return getConnector().supportsLogImages();
    }

    public boolean supportsOwnCoordinates() {
        return getConnector().supportsOwnCoordinates();
    }

    @NonNull
    public ILoggingManager getLoggingManager(final LogCacheActivity activity) {
        return getConnector().getLoggingManager(activity, this);
    }

    public float getDifficulty() {
        return difficulty;
    }

    @Override
    @NonNull
    public String getGeocode() {
        return geocode;
    }

    @NonNull
    public String getShortGeocode() {
        return generateShortGeocode(geocode);
    }

    /**
     * @return displayed owner, might differ from the real owner
     */
    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public String getOwnerGuid() {
        return ownerGuid;
    }

    @NonNull
    public CacheSize getSize() {
        return size;
    }

    public float getTerrain() {
        return terrain;
    }

    public boolean isArchived() {
        return BooleanUtils.isTrue(archived);
    }

    public boolean isDisabled() {
        // a cache can never be disabled and archived at the same time, so the archived state should win (see #11428)
        return !isArchived() && BooleanUtils.isTrue(disabled);
    }

    public boolean isPremiumMembersOnly() {
        return BooleanUtils.isTrue(premiumMembersOnly);
    }

    @Nullable
    public Boolean isPremiumMembersOnlyRaw() {
        return premiumMembersOnly;
    }

    public void setPremiumMembersOnly(final boolean members) {
        this.premiumMembersOnly = members;
    }

    /**
     * @return {@code true} if the user is the owner of the cache, {@code false} otherwise
     */
    public boolean isOwner() {
        return getConnector().isOwner(this);
    }

    /**
     * @return GC username of the (actual) owner, might differ from the owner. Never empty.
     */
    @NonNull
    public String getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     *
     * @return the decrypted hint
     */
    @NonNull
    public String getHint() {
        initializeCacheTexts();
        assertTextNotNull(hint, "Hint");
        return hint;
    }

    /**
     * After lazy loading the lazily loaded field must be non {@code null}.
     */
    private static void assertTextNotNull(final String field, final String name) throws InternalError {
        if (field == null) {
            throw new InternalError(name + " field is not allowed to be null here");
        }
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    @NonNull
    public String getDescription() {
        initializeCacheTexts();
        assertTextNotNull(description, "Description");
        return description;
    }

    /**
     * loads long text parts of a cache on demand (but all fields together)
     */
    private void initializeCacheTexts() {
        if (description == null || shortdesc == null || hint == null || location == null) {
            if (inDatabase()) {
                final Geocache partial = DataStore.loadCacheTexts(this.getGeocode());
                if (description == null) {
                    setDescription(partial.getDescription());
                }
                if (shortdesc == null) {
                    setShortDescription(partial.getShortDescription());
                }
                if (hint == null) {
                    setHint(partial.getHint());
                }
                if (location == null) {
                    setLocation(partial.getLocation());
                }
            } else {
                setDescription(StringUtils.defaultString(description));
                setShortDescription(StringUtils.defaultString(shortdesc));
                setHint(StringUtils.defaultString(hint));
                setLocation(StringUtils.defaultString(location));
            }
        }
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    @NonNull
    public String getShortDescription() {
        initializeCacheTexts();
        assertTextNotNull(shortdesc, "Short description");
        return shortdesc;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getCacheId() {
        // For some connectors ID can be calculated out of geocode
        if (StringUtils.isBlank(cacheId)) {
            if (getConnector() instanceof GCConnector) {
                return String.valueOf(GCUtils.gcLikeCodeToGcLikeId(geocode));
            }
            if (getConnector() instanceof SuConnector) {
                return SuConnector.geocodeToId(geocode);
            }
        }

        return cacheId;
    }

    public String getGuid() {
        return guid;
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    @NonNull
    public String getLocation() {
        initializeCacheTexts();
        assertTextNotNull(location, "Location");
        return location;
    }

    @Nullable
    public String getPersonalNote() {
        return this.personalNote.getNote();
    }

    public boolean supportsNamechange() {
        return getConnector().supportsNamechange();
    }

    public boolean supportsDescriptionchange() {
        return getConnector().supportsDescriptionchange();
    }

    public boolean supportsSettingFoundState() {
        return getConnector().supportsSettingFoundState();
    }

    @NonNull
    private String getShareSubject() {
        final StringBuilder subject = new StringBuilder("Geocache ");
        subject.append(geocode);
        if (StringUtils.isNotBlank(name)) {
            subject.append(" - ").append(name);
        }
        return subject.toString();
    }

    public void shareCache(@NonNull final Activity fromActivity, final Resources res) {
        ShareUtils.shareLink(fromActivity, getShareSubject(), getUrl());
    }

    public boolean canShareLog(@NonNull final LogEntry logEntry) {
        return StringUtils.isNotBlank(getConnector().getCacheLogUrl(this, logEntry));
    }

    @Nullable
    public String getServiceSpecificLogId(@Nullable final LogEntry logEntry) {
        if (logEntry == null) {
            return null;
        }
        return getConnector().getServiceSpecificLogId(logEntry.serviceLogId);
    }

    public void shareLog(@NonNull final Activity fromActivity, final LogEntry logEntry) {
        ShareUtils.shareLink(fromActivity, getShareSubject(), getConnector().getCacheLogUrl(this, logEntry));
    }

    @Nullable
    public String getUrl() {
        return getConnector().getCacheUrl(this);
    }

    @Nullable
    public String getLongUrl() {
        return getConnector().getLongCacheUrl(this);
    }

    public void setDescription(final String description) {
        this.description = description;
        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
    }

    public boolean isFound() {
        return BooleanUtils.isTrue(found);
    }

    public boolean isDNF() {
        return BooleanUtils.isTrue(didNotFound);
    }

    /**
     * @return {@code true} if the user has put a favorite point onto this cache
     */
    public boolean isFavorite() {
        return BooleanUtils.isTrue(favorite);
    }

    @Nullable
    public Boolean isFavoriteRaw() {
        return favorite;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    @Nullable
    public Date getHiddenDate() {
        if (hidden != null) {
            return new Date(hidden.getTime());
        }
        return null;
    }

    @Nullable
    public Date getLastFound() {
        if (lastFound == null && inDatabase()) {
            for (LogEntry logEntry : getLogs()) {
                if (logEntry.logType.isFoundLog()) {
                    lastFound = new Date(logEntry.date);
                    break;
                }
            }
        }
        if (lastFound != null) {
            return new Date(lastFound.getTime());
        }
        return null;
    }

    @NonNull
    public List<String> getAttributes() {
        return attributes.getUnderlyingList();
    }

    @NonNull
    public List<Image> getSpoilers() {
        return ListUtils.unmodifiableList(ListUtils.emptyIfNull(spoilers));
    }

    /** applies filter/plausibilization logic to cache spoilers */
    @NonNull
    public List<Image> getFilteredSpoilers() {
        if (getSpoilers().isEmpty()) {
            return Collections.emptyList();
        }
        final Set<String> listingUrls = new HashSet<>();
        ImageUtils.forEachImageUrlInHtml(s -> listingUrls.add(ImageUtils.imageUrlForSpoilerCompare(s)), getShortDescription(), getDescription());
        final List<Image> result = new ArrayList<>();
        for (Image spoilerCandidate : getSpoilers()) {
            final boolean spoilerInTitle = spoilerCandidate.getTitle() != null &&
                    spoilerCandidate.getTitle().toLowerCase(Locale.US).contains("spoiler");
            final boolean containedInListing = listingUrls.contains(ImageUtils.imageUrlForSpoilerCompare(spoilerCandidate.getUrl()));
            if (spoilerInTitle || !containedInListing) {
                result.add(spoilerCandidate);
            }
        }
        return result;
    }

    /**
     * @return a statistic how often the caches has been found, disabled, archived etc.
     */
    @NonNull
    public Map<LogType, Integer> getLogCounts() {
        if (logCounts.isEmpty() && inDatabase()) {
            @Nullable
            final Map<LogType, Integer> savedLogCounts = DataStore.loadLogCounts(getGeocode());
            if (MapUtils.isNotEmpty(savedLogCounts)) {
                logCounts = savedLogCounts;
            }
        }
        return logCounts;
    }

    public int getFavoritePoints() {
        return favoritePoints;
    }

    /**
     * @return the normalized cached name to be used for sorting, taking into account the numerical parts in the name
     */
    public String getNameForSorting() {
        if (nameForSorting == null) {
            nameForSorting = name;
            // pad each number part to a fixed size of 6 digits, so that numerical sorting becomes equivalent to string sorting
            MatcherWrapper matcher = new MatcherWrapper(NUMBER_PATTERN, nameForSorting);
            int start = 0;
            while (matcher.find(start)) {
                final String number = matcher.group();
                nameForSorting = StringUtils.substring(nameForSorting, 0, matcher.start()) + StringUtils.leftPad(number, 6, '0') + StringUtils.substring(nameForSorting, matcher.start() + number.length());
                start = matcher.start() + Math.max(6, number.length());
                matcher = new MatcherWrapper(NUMBER_PATTERN, nameForSorting);
            }
        }
        return nameForSorting;
    }

    public boolean isVirtual() {
        return cacheType.isVirtual();
    }

    public boolean showSize() {
        return !(size == CacheSize.NOT_CHOSEN || isEventCache() || isVirtual());
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(final long updated) {
        this.updated = updated;
    }

    public long getDetailedUpdate() {
        return detailedUpdate;
    }

    public void setDetailedUpdate(final long detailedUpdate) {
        this.detailedUpdate = detailedUpdate;
    }

    public long getVisitedDate() {
        return visitedDate;
    }

    public void setVisitedDate(final long visitedDate) {
        this.visitedDate = visitedDate;
    }

    @NonNull
    public Set<Integer> getLists() {
        return lists;
    }

    public void setLists(final Set<Integer> lists) {
        // Create a new set to allow immutable structures such as SingletonSet to be
        // given by the caller. We want the value returned by getLists() to be mutable
        // since remove or add operations may be done on it.
        this.lists = new HashSet<>(lists);
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(final boolean detailed) {
        this.detailed = detailed;
    }

    public void setHidden(@Nullable final Date hidden) {
        this.hidden = hidden != null ? new Date(hidden.getTime()) : null;
    }

    public void setLastFound(@Nullable final Date lastFound) {
        this.lastFound = lastFound != null ? new Date(lastFound.getTime()) : null;
    }

    @Nullable
    public Float getDirection() {
        return direction;
    }

    public void setDirection(final Float direction) {
        this.direction = direction;
    }

    @Nullable
    public Float getDistance() {
        return distance;
    }

    public void setDistance(final Float distance) {
        this.distance = distance;
    }

    @Override
    public Geopoint getCoords() {
        return coords;
    }

    /**
     * Set reliable coordinates
     */
    public void setCoords(final Geopoint coords) {
        this.coords = coords;
    }

    public void setShortDescription(final String shortdesc) {
        this.shortdesc = shortdesc;
        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
    }

    public void setFavoritePoints(final int favoriteCnt) {
        this.favoritePoints = favoriteCnt;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(final float rating) {
        this.rating = rating;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(final int votes) {
        this.votes = votes;
    }

    public float getMyVote() {
        return myVote;
    }

    public void setMyVote(final float myVote) {
        this.myVote = myVote;
    }

    /**
     * Get the current inventory count
     *
     * @return the inventory size
     */
    public int getInventoryItems() {
        return Math.max(inventoryItems, 0);
    }

    public boolean hasInventoryItemsSet() {
        return inventoryItems >= 0;
    }

    /**
     * Set the current inventory count
     *
     * @param inventoryItems the new inventory size
     */
    public void setInventoryItems(final int inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    /**
     * Get the current inventory
     *
     * @return the inventory of Trackables as unmodifiable collection. Use {@link #setInventory(List)} or
     * {@link #addInventoryItem(Trackable)} for modifications.
     */
    @NonNull
    public List<Trackable> getInventory() {
        return inventory == null ? Collections.emptyList() : Collections.unmodifiableList(inventory);
    }

    /**
     * Replace the inventory with new content.
     * No checks are performed.
     *
     * @param newInventory to set on Geocache
     */
    public void setInventory(final List<Trackable> newInventory) {
        inventory = newInventory;
        inventoryItems = CollectionUtils.size(inventory);
    }

    /**
     * Add new Trackables to inventory safely.
     * This takes care of removing old items if they are from the same brand.
     * If items are present, data is merged, not duplicated.
     *
     * @param newTrackables to be added to the Geocache
     */
    public void mergeInventory(@NonNull final List<Trackable> newTrackables, final EnumSet<TrackableBrand> processedBrands) {

        final List<Trackable> mergedTrackables = new ArrayList<>(newTrackables);

        for (final Trackable trackable : ListUtils.emptyIfNull(inventory)) {
            if (processedBrands.contains(trackable.getBrand())) {
                final ListIterator<Trackable> iterator = mergedTrackables.listIterator();
                while (iterator.hasNext()) {
                    final Trackable newTrackable = iterator.next();
                    if (trackable.getUniqueID().equals(newTrackable.getUniqueID())) {
                        // Respect the merge order. New Values replace existing values.
                        trackable.mergeTrackable(newTrackable);
                        iterator.set(trackable);
                        break;
                    }
                }
            } else {
                mergedTrackables.add(trackable);
            }
        }
        setInventory(mergedTrackables);
    }

    /**
     * Add new Trackable to inventory safely.
     * If items are present, data are merged, not duplicated.
     *
     * @param newTrackable to be added to the Geocache
     */
    public void addInventoryItem(final Trackable newTrackable) {
        if (inventory == null) {
            inventory = new ArrayList<>();
        }
        boolean foundTrackable = false;
        for (final Trackable trackable : inventory) {
            if (trackable.getUniqueID().equals(newTrackable.getUniqueID())) {
                // Trackable already present, merge data
                foundTrackable = true;
                trackable.mergeTrackable(newTrackable);
                break;
            }
        }
        if (!foundTrackable) {
            inventory.add(newTrackable);
        }
        inventoryItems = inventory.size();
    }

    /**
     * @return {@code true} if the cache is on the user's watchlist, {@code false} otherwise
     */
    public boolean isOnWatchlist() {
        return BooleanUtils.isTrue(onWatchlist);
    }

    @Nullable
    public Boolean isOnWatchlistRaw() {
        return onWatchlist;
    }

    public void setOnWatchlist(final boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    public boolean isLinearAlc() {
        Log.d("_AL isLinearAlc: " + alcMode);
        return alcMode == 1;
    }

    public void setAlcMode(final int alcMode) {
        Log.d("_AL setAlcMode: " + alcMode);
        this.alcMode = alcMode;
    }

    public int getAlcMode() {
        Log.d("_AL getAlcMode: " + alcMode);
        return alcMode;
    }

    /**
     * Set the number of users watching this geocache
     *
     * @param watchlistCount Number of users watching this geocache
     */
    public void setWatchlistCount(final int watchlistCount) {
        this.watchlistCount = watchlistCount;
    }

    /**
     * get the number of users watching this geocache
     *
     * @return watchlistCount Number of users watching this geocache
     */
    public int getWatchlistCount() {
        return watchlistCount;
    }

    /**
     * return an immutable list of waypoints.
     *
     * @return always non {@code null}
     */
    @NonNull
    public List<Waypoint> getWaypoints() {
        return waypoints.getUnderlyingList();
    }

    /**
     * @param waypoints      List of waypoints to set for cache
     * @param saveToDatabase Indicates whether to add the waypoints to the database. Should be false if
     *                       called while loading or building a cache
     * @return {@code true} if waypoints successfully added to waypoint database
     */
    public boolean setWaypoints(@Nullable final List<Waypoint> waypoints, final boolean saveToDatabase) {
        this.waypoints.clear();
        if (waypoints != null) {
            this.waypoints.addAll(waypoints);
        }
        finalDefined = false;
        if (waypoints != null) {
            for (final Waypoint waypoint : waypoints) {
                waypoint.setGeocode(geocode);
                if (waypoint.isFinalWithCoords()) {
                    finalDefined = true;
                }
            }
        }
        return saveToDatabase && DataStore.saveWaypoints(this);
    }

    /**
     * The list of logs is immutable, because it is directly fetched from the database on demand, and not stored at this
     * object. If you want to modify logs, you have to load all logs of the cache, create a new list from the existing
     * list and store that new list in the database.
     *
     * @return immutable list of logs
     */
    @NonNull
    public List<LogEntry> getLogs() {
        //if a cache was freshly loaded from server, it may not have the "logs" flag although logs exist in local db.
        return DataStore.loadLogs(geocode);
        //return inDatabase() ? DataStore.loadLogs(geocode) : Collections.emptyList();
    }

    /**
     * @return only the logs of friends
     */
    @NonNull
    public List<LogEntry> getFriendsLogs() {
        final List<LogEntry> friendLogs = new ArrayList<>();
        for (final LogEntry log : getLogs()) {
            if (log.friend) {
                friendLogs.add(log);
            }
        }
        return Collections.unmodifiableList(friendLogs);
    }

    public boolean isStatusChecked() {
        return statusChecked;
    }

    public void setStatusChecked(final boolean statusChecked) {
        this.statusChecked = statusChecked;
    }

    @NonNull
    public String getDirectionImg() {
        return directionImg;
    }

    public void setDirectionImg(@NonNull final String directionImg) {
        this.directionImg = directionImg;
    }

    public void setGeocode(@NonNull final String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
    }

    public void setCacheId(final String cacheId) {
        this.cacheId = cacheId;
    }

    public void setGuid(final String guid) {
        this.guid = guid;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setOwnerDisplayName(final String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public void setOwnerGuid(final String ownerGuid) {
        this.ownerGuid = ownerGuid;
    }

    public void setAssignedEmoji(final int assignedEmoji) {
        this.assignedEmoji = assignedEmoji;
    }

    public void setOwnerUserId(final String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public void setHint(final String hint) {
        this.hint = hint;
    }

    public void setSize(@NonNull final CacheSize size) {
        this.size = size;
    }

    public void setDifficulty(final float difficulty) {
        this.difficulty = difficulty;
    }

    public void setTerrain(final float terrain) {
        this.terrain = terrain;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setPersonalNote(final String personalNote) {
        setPersonalNote(personalNote, false);
    }

    public void setPersonalNote(final String personalNote, final boolean isFromProvider) {
        this.personalNote.setNote(personalNote);
        this.personalNote.setFromProvider(isFromProvider);
    }

    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
    }

    public void setArchived(final boolean archived) {
        this.archived = archived;
    }

    public void setFound(final boolean found) {
        this.found = found;
    }

    public void setDNF(final boolean didNotFound) {
        this.didNotFound = didNotFound;
    }

    public void setAttributes(final List<String> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            this.attributes.addAll(attributes);
        }
    }

    public void setSpoilers(final List<Image> spoilers) {
        this.spoilers = spoilers;
    }

    public boolean hasSpoilersSet() {
        return this.spoilers != null;
    }

    public void setLogCounts(@NonNull final Map<LogType, Integer> logCounts) {
        this.logCounts = logCounts;
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.IBasicCache#getType()
     *
     * @returns Never null
     */
    @NonNull
    public CacheType getType() {
        return cacheType;
    }

    public void setType(final CacheType cacheType) {
        if (cacheType == null || cacheType == CacheType.ALL) {
            throw new IllegalArgumentException("Illegal cache type");
        }
        this.cacheType = cacheType;
        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
    }

    public boolean hasDifficulty() {
        return difficulty > 0f;
    }

    public boolean hasTerrain() {
        return terrain > 0f;
    }

    /**
     * @return the storageLocation
     */
    @NonNull
    public EnumSet<StorageLocation> getStorageLocation() {
        return storageLocation;
    }

    /**
     * @param storageLocation the storageLocation to set
     */
    public void addStorageLocation(final StorageLocation storageLocation) {
        this.storageLocation.add(storageLocation);
    }

    /**
     * Check if this cache instance comes from or has been stored into the database.
     */
    public boolean inDatabase() {
        return storageLocation.contains(StorageLocation.DATABASE);
    }

    /**
     * @param waypoint       Waypoint to add to the cache
     * @param saveToDatabase Indicates whether to add the waypoint to the database. Should be false if
     *                       called while loading or building a cache
     * @return {@code true} if waypoint successfully added to waypoint database
     */
    public boolean addOrChangeWaypoint(final Waypoint waypoint, final boolean saveToDatabase) {
        waypoint.setGeocode(geocode);

        if (waypoint.isNewWaypoint()) {
            if (StringUtils.isBlank(waypoint.getPrefix())) {
                assignUniquePrefix(waypoint);
            }
            waypoints.add(waypoint);
            if (waypoint.isFinalWithCoords()) {
                finalDefined = true;
            }
        } else { // this is a waypoint being edited
            final int index = getWaypointIndex(waypoint);
            if (index >= 0) {
                final Waypoint oldWaypoint = waypoints.remove(index);
                waypoint.setPrefix(oldWaypoint.getPrefix());
                //migration
                if (StringUtils.isBlank(waypoint.getPrefix())
                        || StringUtils.equalsIgnoreCase(waypoint.getPrefix(), Waypoint.PREFIX_OWN)) {
                    assignUniquePrefix(waypoint);
                }
            }
            waypoints.add(waypoint);
            // when waypoint was edited, finalDefined may have changed
            resetFinalDefined();
        }
        return saveToDatabase && DataStore.saveWaypoint(waypoint.getId(), geocode, waypoint);
    }

    private void assignUniquePrefix(final Waypoint waypoint) {
        Waypoint.assignUniquePrefix(waypoint, waypoints);
    }

    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    public boolean hasUserdefinedWaypoints() {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.isUserDefined()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGeneratedWaypoints() {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.getWaypointType() == WaypointType.GENERATED) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFinalDefined() {
        return finalDefined;
    }

    // Only for loading
    public void setFinalDefined(final boolean finalDefined) {
        this.finalDefined = finalDefined;
    }

    /**
     * Reset {@code finalDefined} based on current list of stored waypoints
     */
    private void resetFinalDefined() {
        finalDefined = getFirstMatchingWaypoint(Waypoint::isFinalWithCoords) != null;
    }

    public boolean hasUserModifiedCoords() {
        return BooleanUtils.isTrue(userModifiedCoords);
    }

    @Nullable
    public Boolean getUserModifiedCoordsRaw() {
        return userModifiedCoords;
    }

    public void setUserModifiedCoords(final boolean coordsChanged) {
        userModifiedCoords = coordsChanged;
    }

    /**
     * Duplicate a waypoint.
     *
     * @param original the waypoint to duplicate
     * @return the copy of the waypoint if it was duplicated, {@code null} otherwise (invalid index)
     */
    @Nullable
    public Waypoint duplicateWaypoint(final Waypoint original, final boolean addPrefix) {
        if (original == null) {
            return null;
        }
        final int index = getWaypointIndex(original);
        final Waypoint copy = new Waypoint(original);
        copy.setName((addPrefix ? CgeoApplication.getInstance().getString(R.string.waypoint_copy_of) + " " : "") + copy.getName());

        // create unique prefix
        copy.setUserDefined();
        final String basePrefix = copy.getPrefix();
        int counter = 0;
        boolean found = true;
        while (found) {
            found = false;
            for (Waypoint waypoint : waypoints) {
                if (StringUtils.equals(waypoint.getPrefix(), copy.getPrefix())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                counter++;
                copy.setPrefix(basePrefix + "-" + counter);
            }
        }

        waypoints.add(index + 1, copy);
        return DataStore.saveWaypoint(-1, geocode, copy) ? copy : null;
    }

    /**
     * delete a user-defined waypoint
     *
     * @param waypoint to be removed from cache
     * @return {@code true}, if the waypoint was deleted
     */
    public boolean deleteWaypoint(final Waypoint waypoint) {
        if (waypoint == null) {
            return false;
        }
        if (waypoint.isNewWaypoint()) {
            return false;
        }
        if (waypoint.getWaypointType() != WaypointType.ORIGINAL || waypoint.belongsToUserDefinedCache()) {
            final int index = getWaypointIndex(waypoint);
            waypoints.remove(index);
            DataStore.deleteWaypoint(waypoint.getId());
            DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE));
            // Check status if Final is defined
            if (waypoint.isFinalWithCoords()) {
                resetFinalDefined();
            }
            return true;
        }
        return false;
    }

    /**
     * deletes any waypoint
     */

    public void deleteWaypointForce(final Waypoint waypoint) {
        final int index = getWaypointIndex(waypoint);
        waypoints.remove(index);
        DataStore.deleteWaypoint(waypoint.getId());
        DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE));
        resetFinalDefined();
    }

    /**
     * Find index of given {@code waypoint} in cache's {@code waypoints} list
     *
     * @param waypoint to find index for
     * @return index in {@code waypoints} if found, -1 otherwise
     */
    private int getWaypointIndex(final Waypoint waypoint) {
        final int id = waypoint.getId();
        for (int index = 0; index < waypoints.size(); index++) {
            if (waypoints.get(index).getId() == id) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Lookup a waypoint by its id.
     *
     * @param id the id of the waypoint to look for
     * @return waypoint or {@code null}
     */
    @Nullable
    public Waypoint getWaypointById(final int id) {
        for (final Waypoint waypoint : waypoints) {
            if (waypoint.getId() == id) {
                return waypoint;
            }
        }
        return null;
    }

    /**
     * Lookup a waypoint by its prefix.
     *
     * @param prefix the prefix of the waypoint to look for
     * @return waypoint or {@code null}
     */
    @Nullable
    public Waypoint getWaypointByPrefix(final String prefix) {
        for (final Waypoint waypoint : waypoints) {
            if (waypoint.getPrefix().equals(prefix)) {
                return waypoint;
            }
        }
        return null;
    }

    /** Convenience method for {@link #addCacheArtefactsFromNotes(String)} with parameter null */
    public boolean addCacheArtefactsFromNotes() {
        return addCacheArtefactsFromNotes(null);
    }

    /**
     * Detect cache artefacts (waypoints+variables) in the user's notes (cache + waypoints) and add them to user-defined waypoints/variables.
     * @param previousAllUserNotes if given, then a parse of previous notes is used to help in deciding which values potentially to overwrite
     */
    public boolean addCacheArtefactsFromNotes(@Nullable final String previousAllUserNotes) {
        return addCacheArtefactsFromText(getAllUserNotes(), false, CgeoApplication.getInstance().getString(R.string.cache_personal_note), false, previousAllUserNotes);
    }

    /** returns a concatenation of all user notes (Cache + waypoints) */
    @NonNull
    public String getAllUserNotes() {
        final StringBuilder sb = new StringBuilder();
        for (Waypoint w : getWaypoints()) {
            if (!StringUtils.isBlank(w.getUserNote())) {
                sb.append(w.getUserNote()).append("\n");
            }
        }
        return sb.append(StringUtils.defaultString(getPersonalNote(), "")).toString();

    }


    /**
     * Detect cache artefacts (waypoints+variables) in the given text and add them to user-defined waypoints
     * or updates existing ones with meta information.
     *
     * @param text            text which might contain coordinates
     * @param updateDb        if true the added waypoints are stored in DB right away
     * @param namePrefix      prefix for default waypoint names (if names cannot be extracted from text)
     * @param forceExtraction if extraction should be enforced, regardless of cache setting
     * @param previousAllUserNotes if given, then a parse of previous notes is used to help in deciding which values potentially to overwrite
     */
    public boolean addCacheArtefactsFromText(@Nullable final String text, final boolean updateDb, @NonNull final String namePrefix, final boolean forceExtraction, @Nullable final String previousAllUserNotes) {
        boolean changed = false;
        if (forceExtraction || !preventWaypointsFromNote) {
            final CacheArtefactParser previousParser = previousAllUserNotes == null ? null : new CacheArtefactParser(this, namePrefix).parse(previousAllUserNotes);

            final CacheArtefactParser cacheArtefactParser = new CacheArtefactParser(this, namePrefix).parse(StringUtils.defaultString(text));
            for (final Waypoint parsedWaypoint : cacheArtefactParser.getWaypoints()) {
                changed |= addOrMergeInfoToExistingWaypoint(updateDb, namePrefix, parsedWaypoint);
            }
            final Map<String, String> newVars = new HashMap<>();
            for (Map.Entry<String, String> var : cacheArtefactParser.getVariables().entrySet()) {
                if (getVariables().isWorthAddingWithoutLoss(var.getKey(), var.getValue(), previousParser == null ? null : previousParser.getVariables().get(var.getKey()))) {
                    newVars.put(var.getKey(), var.getValue());
                }
            }
            changeVariables(newVars);
        }
        return changed;
    }

    /** Returns a map of variables which differ from list and parsing user notes */
    @NonNull
    public Map<String, Pair<String, String>> getVariableDifferencesFromUserNotes() {
        final Map<String, String> noteVars = new CacheArtefactParser(this, "").parse(getAllUserNotes()).getVariables();
        final Map<String, String> cacheVars = getVariables().toMap();
        final Map<String, Pair<String, String>> result = CommonUtils.compare(noteVars, cacheVars);
        final Iterator<Map.Entry<String, Pair<String, String>>> it = result.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, Pair<String, String>> entry = it.next();
            if (StringUtils.isBlank(entry.getValue().first)) {
                it.remove();
            }
        }
        return result;
    }

    /** changes this cache's variables with the given one's. Change is forced! */
    public void changeVariables(final Map<String, String> newVars) {
        for (Map.Entry<String, String> var : newVars.entrySet()) {
            getVariables().addVariable(var.getKey(), var.getValue());
            getVariables().saveState();
            getVariables().recalculateWaypoints(this); //need to do this because new wps are not saved to DB yet
        }
    }


    private boolean addOrMergeInfoToExistingWaypoint(final boolean updateDb, @NonNull final String namePrefix, final Waypoint wpCandidate) {
        boolean changed = false;
        final Waypoint existingWaypoint = findWaypoint(wpCandidate);
        if (null == existingWaypoint) {
            //add as new waypoint
            addOrChangeWaypoint(wpCandidate, updateDb);
            changed = true;
        } else {
            //if parsed waypoint contains more up-to-date-information -> overwrite it
            if (existingWaypoint.mergeFromParsedText(wpCandidate, namePrefix)) {
                addOrChangeWaypoint(existingWaypoint, updateDb);
                changed = true;
            }
        }
        return changed;
    }

    public int addCalculatedWaypoints(final Collection<Pair<String, String>> foundPatterns, final String namePrefix) {
        int added = 0;
        for (final Pair<String, String> pattern : foundPatterns) {
            final Waypoint wpCandidate = new Waypoint(namePrefix + " " + (added + 1), WaypointType.WAYPOINT, true);
            final CalculatedCoordinate cc = new CalculatedCoordinate();
            cc.setLatitudePattern(pattern.first);
            cc.setLongitudePattern(pattern.second);
            wpCandidate.setCalculated(cc, f -> variables.getValue(f));
            final boolean changed = addOrMergeInfoToExistingWaypoint(true, namePrefix, wpCandidate);
            if (changed) {
                added++;
            }
        }
        return added;
    }

    @Nullable
    private Waypoint findWaypoint(@NonNull final Waypoint searchWp) {
        //try to match prefix
        final String searchWpPrefix = searchWp.getPrefix();
        if (StringUtils.isNotBlank(searchWpPrefix)) {
            for (final Waypoint waypoint : waypoints) {
                if (searchWpPrefix.equals(waypoint.getPrefix())) {
                    return waypoint;
                }
            }
            return null;
        }

        //try to match coordinate
        final Geopoint searchWpPoint = searchWp.getCoords();
        if (null != searchWpPoint) {
            for (final Waypoint waypoint : waypoints) {
                // waypoint can have no coords such as a Final set by cache owner
                final Geopoint coords = waypoint.getCoords();
                if (coords != null && coords.equalsDecMinute(searchWpPoint)) {
                    return waypoint;
                }
            }
        }

        //try to match calculate coordinate
        if (searchWp.isCalculated()) {
            for (final Waypoint waypoint : waypoints) {
                // calculated waypoints match if they have same config
                if (waypoint.isCalculated() && Objects.equals(waypoint.getCalcStateConfig(), searchWp.getCalcStateConfig())) {
                    return waypoint;
                }
            }
        }

        //try to match name if prefix is empty and coords are not equal.
        //But only, if coordinates of waypoint can be updated, otherwise create a new waypoint
        //(it's not a bug, it's a feature - otherwise the coordinates gets lost)
        final String searchWpName = searchWp.getName();
        final String searchWpType = searchWp.getWaypointType().getL10n();
        if (StringUtils.isNotBlank(searchWpName)) {
            for (final Waypoint waypoint : waypoints) {
                final Geopoint point = waypoint.getCoords();
                final boolean canChangeCoordinates = null == searchWpPoint || null == point;
                if (canChangeCoordinates && searchWpName.equals(waypoint.getName()) && searchWpType.equals(waypoint.getWaypointType().getL10n())) {
                    return waypoint;
                }
            }
        }
        return null;
    }

    /*
     * For working in the debugger
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    @NonNull
    public String toString() {
        return this.geocode + " " + this.name;
    }

    @Override
    public int hashCode() {
        return StringUtils.defaultString(geocode).hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        // TODO: explain the following line or remove this non-standard equality method
        // just compare the geocode even if that is not what "equals" normally does
        return this == obj || (obj instanceof Geocache && StringUtils.isNotEmpty(geocode) && geocode.equals(((Geocache) obj).geocode));
    }

    public void store() {
        if (lists.isEmpty()) {
            lists.add(StoredList.STANDARD_LIST_ID);
        }
        storeCache(this, null, lists, false, null);
    }

    public void store(final Set<Integer> listIds, final DisposableHandler handler) {
        lists.clear();
        lists.addAll(listIds);
        storeCache(this, null, lists, false, handler);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    @Nullable
    public WaypointType getWaypointType() {
        return null;
    }

    @Override
    @NonNull
    public CoordinatesType getCoordType() {
        return CoordinatesType.CACHE;
    }

    @NonNull
    public Disposable drop(final Handler handler) {
        return Schedulers.io().scheduleDirect(() -> {
            try {
                dropSynchronous();
                handler.sendMessage(Message.obtain());
            } catch (final Exception e) {
                Log.e("cache.drop: ", e);
            }
        });
    }

    public void dropSynchronous() {
        DataStore.markDropped(Collections.singletonList(this));
        DataStore.removeCache(getGeocode(), EnumSet.of(RemoveFlag.CACHE));
    }

    private void warnIncorrectParsingIf(final boolean incorrect, final String field) {
        if (incorrect) {
            Log.w(field + " not parsed correctly for " + geocode);
        }
    }

    private void warnIncorrectParsingIfBlank(final String str, final String field) {
        warnIncorrectParsingIf(StringUtils.isBlank(str), field);
    }

    public void checkFields() {
        warnIncorrectParsingIfBlank(getGeocode(), "geo");
        warnIncorrectParsingIfBlank(getName(), "name");
        warnIncorrectParsingIfBlank(getGuid(), "guid");
        warnIncorrectParsingIf(getTerrain() == 0.0, "terrain");
        warnIncorrectParsingIf(getDifficulty() == 0.0, "difficulty");
        warnIncorrectParsingIfBlank(getOwnerDisplayName(), "owner");
        warnIncorrectParsingIfBlank(getOwnerUserId(), "owner");
        warnIncorrectParsingIf(getHiddenDate() == null, "hidden");
        warnIncorrectParsingIf(getFavoritePoints() < 0, "favoriteCount");
        warnIncorrectParsingIf(getSize() == CacheSize.UNKNOWN, "size");
        warnIncorrectParsingIf(getType() == null || getType() == CacheType.UNKNOWN, "type");
        warnIncorrectParsingIf(getCoords() == null, "coordinates");
        warnIncorrectParsingIfBlank(getLocation(), "location");
    }

    @NonNull
    public Disposable refresh(@NonNull final DisposableHandler handler, @NonNull final Scheduler scheduler) {
        return scheduler.scheduleDirect(() -> refreshSynchronous(handler));
    }

    public void refreshSynchronous(@NonNull final DisposableHandler handler) {
        refreshSynchronous(handler, lists);
    }

    public void refreshSynchronous(@Nullable final DisposableHandler handler, @NonNull final Set<Integer> additionalListIds) {
        final Set<Integer> combinedListIds = new HashSet<>(lists);
        combinedListIds.addAll(additionalListIds);
        storeCache(null, geocode, combinedListIds, true, handler);
    }

    /**
     * Download and store a cache synchronous
     *
     * @param origCache the cache which should be refreshed, can be null
     * @param geocode the geocode of the cache which should be downloaded
     * @param lists to which lists the cache should be added
     * @param forceRedownload whether the cache should be re-downloaded, even if it's already stored offline
     * @param handler a handler to receive status updates, can be null
     * @return true, if the cache was stored successfully
     */
    @WorkerThread
    @SuppressWarnings("PMD.NPathComplexity")
    public static boolean storeCache(@Nullable final Geocache origCache, @Nullable final String geocode, @NonNull final Set<Integer> lists, final boolean forceRedownload, @Nullable final DisposableHandler handler) {
        try {
            final Geocache cache;
            // get cache details, they may not yet be complete
            if (origCache != null) {
                // only reload the cache if it was already stored or doesn't have full details (by checking the description)
                if (origCache.isOffline() || StringUtils.isBlank(origCache.getDescription())) {
                    final SearchResult search = searchByGeocode(origCache.getGeocode(), null, false, handler);
                    cache = search != null ? search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB) : origCache;
                    if (search != null && search.getError() != StatusCode.NO_ERROR) {
                        DisposableHandler.sendShowStatusToast(handler, search.getError().getErrorString());
                    }
                } else {
                    cache = origCache;
                }
            } else if (StringUtils.isNotBlank(geocode)) {
                final SearchResult search = searchByGeocode(geocode, null, forceRedownload, handler);
                cache = search != null ? search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB) : null;
                if (search != null && search.getError() != StatusCode.NO_ERROR) {
                    DisposableHandler.sendShowStatusToast(handler, search.getError().getErrorString());
                }
            } else {
                cache = null;
            }

            if (cache == null) {
                if (handler != null) {
                    handler.sendMessage(Message.obtain());
                }

                return false;
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false;
            }

            final HtmlImage imgGetter = new HtmlImage(cache.getGeocode(), false, true, forceRedownload);

            // store images from description
            if (StringUtils.isNotBlank(cache.getDescription())) {
                HtmlCompat.fromHtml(cache.getDescription(), HtmlCompat.FROM_HTML_MODE_LEGACY, imgGetter, null);
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false;
            }

            // store spoilers
            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                for (final Image oneSpoiler : cache.getSpoilers()) {
                    imgGetter.getDrawable(oneSpoiler.getUrl());
                }
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false;
            }

            // store images from logs
            if (Settings.isStoreLogImages()) {
                for (final LogEntry log : cache.getLogs()) {
                    if (log.hasLogImages()) {
                        for (final Image oneLogImg : log.logImages) {
                            imgGetter.getDrawable(oneLogImg.getUrl());
                        }
                    }
                }
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false;
            }

            // Need to wait for images loading since HtmlImage.getDrawable is non-blocking here
            imgGetter.waitForEndCompletable(null).blockingAwait();

            cache.setLists(lists);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

            if (DisposableHandler.isDisposed(handler)) {
                return false;
            }

            if (handler != null) {
                handler.sendEmptyMessage(DisposableHandler.DONE);
            }
            return true;
        } catch (final Exception e) {
            Log.e("Geocache.storeCache", e);
            return false;
        }
    }

    @Nullable
    @WorkerThread
    public static SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final boolean forceReload, @Nullable final DisposableHandler handler) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            Log.e("Geocache.searchByGeocode: No geocode nor guid given");
            return null;
        }

        if (!forceReload && (DataStore.isOffline(geocode, guid) || DataStore.isThere(geocode, guid, true))) {
            final SearchResult search = new SearchResult();
            final String realGeocode = StringUtils.isNotBlank(geocode) ? geocode : DataStore.getGeocodeForGuid(guid);
            search.addGeocode(realGeocode);
            return search;
        }

        // if we have no geocode, we can't dynamically select the handler, but must explicitly use GC
        if (geocode == null) {
            return GCConnector.getInstance().searchByGeocode(null, guid, handler);
        }

        final IConnector connector = ConnectorFactory.getConnector(geocode);
        if (connector instanceof ISearchByGeocode) {
            return ((ISearchByGeocode) connector).searchByGeocode(geocode, guid, handler);
        }
        return null;
    }

    public boolean isOffline() {
        return !lists.isEmpty() && (lists.size() > 1 || lists.iterator().next() != StoredList.TEMPORARY_LIST.id);
    }

    public int getEventStartTimeInMinutes() {
        if (eventTimesInMin.start == null) {
            guessEventTimeMinutes();
        }
        return eventTimesInMin.start;
    }

    public int getEventEndTimeInMinutes() {
        return eventTimesInMin.end == null ? -1 : eventTimesInMin.end;
    }

    /**
     * guess an event start time from the description
     *
     * @return start time in minutes after midnight
     */
    private void guessEventTimeMinutes() {
        if (!isEventCache()) {
            eventTimesInMin.start = -1;
            return;
        }

        // GC Listings have the start and end time in short description, try that first
        final int[] gcDates = EventTimeParser.getEventTimesFromGcShortDesc(getShortDescription());
        if (gcDates[0] >= 0 && gcDates[1] >= 0) {
            eventTimesInMin.start = gcDates[0];
            eventTimesInMin.end = gcDates[1];
        } else {
            // if not successful scan the whole description for what looks like a start time
            final String searchText = getShortDescription() + ' ' + getDescription();
            eventTimesInMin.start = EventTimeParser.guessEventTimeMinutes(searchText);
        }
    }

    @NonNull
    public Collection<Image> getImages() {
        final List<Image> result = new LinkedList<>(getSpoilers());
        ImageUtils.addImagesFromHtml(result, geocode, getShortDescription(), getDescription());
        for (final LogEntry log : getLogs()) {
            result.addAll(log.logImages);
        }
        addLocalSpoilersTo(result);
        return result;
    }

    @NonNull
    public List<Image> getNonStaticImages() {
        final ArrayList<Image> result = new ArrayList<>();
        for (final Image image : getImages()) {
            // search strings fit geocaching.com and opencaching, may need to add others
            // Xiaomi does not support java.lang.CharSequence#containsAny(java.lang.CharSequence[]),
            // which is called by StringUtils.containsAny(CharSequence, CharSequence...).
            // Thus, we have to use StringUtils.contains(...) instead (see issue #5766).
            final String url = image.getUrl();
            if (!StringUtils.contains(url, "/static") &&
                    !StringUtils.contains(url, "/resource") &&
                    !StringUtils.contains(url, "/icons/")) {
                result.add(image);
            }
        }
        return result;
    }

    /**
     * Add spoilers stored locally in <tt>/sdcard/cgeo/GeocachePhotos</tt>. If a cache is named GC123ABC, the
     * directory will be <tt>/sdcard/cgeo/GeocachePhotos/C/B/GC123ABC/</tt>.
     *
     * @param spoilers the list to add to
     */
    private void addLocalSpoilersTo(@NonNull final List<Image> spoilers) {
        if (StringUtils.length(geocode) >= 2) {
            final String suffix = StringUtils.right(geocode, 2);
            final Folder spoilerFolder = Folder.fromFolder(PersistableFolder.SPOILER_IMAGES.getFolder(),
                    suffix.substring(1) + "/" + suffix.substring(0, 1) + "/" + geocode);
            for (ContentStorage.FileInformation imageFile : ContentStorage.get().list(spoilerFolder)) {
                if (imageFile.isDirectory) {
                    continue;
                }
                spoilers.add(new Image.Builder().setUrl(imageFile.uri).setTitle(imageFile.name).setCategory(Image.ImageCategory.OWN).build());
            }
        }
    }

    public void setDetailedUpdatedNow() {
        final long now = System.currentTimeMillis();
        setUpdated(now);
        setDetailedUpdate(now);
        setDetailed(true);
    }

    /**
     * Gets whether the user has logged the specific log type for this cache. Only checks the currently stored logs of
     * the cache, so the result might be wrong.
     */
    public boolean hasOwnLog(@NonNull final LogType logType) {
        for (final LogEntry logEntry : getLogs()) {
            if (logEntry.logType == logType && logEntry.isOwn()) {
                return true;
            }
        }
        return false;
    }

    @DrawableRes
    public int getMapMarkerId() {
        return getConnector().getCacheMapMarkerId();
    }

    @DrawableRes
    public int getMapMarkerBackgroundId() {
        return getConnector().getCacheMapMarkerBackgroundId();
    }

    @DrawableRes
    public int getMapDotMarkerId() {
        return getConnector().getCacheMapDotMarkerId();
    }

    @DrawableRes
    public int getMapDotMarkerBackgroundId() {
        return getConnector().getCacheMapDotMarkerBackgroundId();
    }

    public boolean isLogPasswordRequired() {
        return logPasswordRequired;
    }

    public void setLogPasswordRequired(final boolean required) {
        logPasswordRequired = required;
    }

    public boolean isPreventWaypointsFromNote() {
        return preventWaypointsFromNote;
    }

    public void setPreventWaypointsFromNote(final boolean preventWaypointsFromNote) {
        this.preventWaypointsFromNote = preventWaypointsFromNote;
    }

    @NonNull
    public String getWaypointGpxId(@NonNull final String prefix) {
        return getConnector().getWaypointGpxId(prefix, geocode);
    }

    @NonNull
    public String getWaypointPrefix(final String name) {
        return getConnector().getWaypointPrefix(name);
    }

    /**
     * Get number of overall finds for a cache, or 0 if the number of finds is not known.
     * TODO: 0 should be a valid value, maybe need to return -1 if the number is not known
     */
    public int getFindsCount() {
        int sumFound = 0;
        for (final Entry<LogType, Integer> logCount : getLogCounts().entrySet()) {
            if (logCount.getKey().isFoundLog()) {
                final Integer logged = logCount.getValue();
                if (logged != null) {
                    sumFound += logged;
                }
            }
        }
        return sumFound;
    }

    public boolean applyDistanceRule() {
        return mayApplyDistanceRule()
                && (getType().applyDistanceRule() || hasUserModifiedCoords());
    }

    public boolean mayApplyDistanceRule() {
        return !isArchived()
                && (getConnector() == GCConnector.getInstance() || getConnector() == InternalConnector.getInstance());
    }

    @NonNull
    public LogType getDefaultLogType() {
        if (isEventCache()) {
            final Date eventDate = getHiddenDate();
            final boolean expired = CalendarUtils.isPastEvent(this);

            if (hasOwnLog(LogType.WILL_ATTEND) || expired || (eventDate != null && CalendarUtils.daysSince(eventDate.getTime()) == 0)) {
                return hasOwnLog(LogType.ATTENDED) ? LogType.NOTE : LogType.ATTENDED;
            }
            return LogType.WILL_ATTEND;
        }
        if (isFound()) {
            return LogType.NOTE;
        }
        if (getType() == CacheType.WEBCAM) {
            return LogType.WEBCAM_PHOTO_TAKEN;
        }
        return LogType.FOUND_IT;
    }

    /**
     * Get the geocodes of a collection of caches.
     *
     * @param caches a collection of caches
     * @return the non-blank geocodes of the caches
     */
    @NonNull
    public static Set<String> getGeocodes(@NonNull final Collection<Geocache> caches) {
        final Set<String> geocodes = new HashSet<>(caches.size());
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();
            if (StringUtils.isNotBlank(geocode)) {
                geocodes.add(geocode);
            }
        }
        return geocodes;
    }

    /**
     * Show the hint as toast message. If no hint is available, a default "no hint available" will be shown instead.
     */
    public void showHintToast(@NonNull final Activity activity) {
        final String hint = getHint();
        ActivityMixin.showToast(activity, StringUtils.defaultIfBlank(hint, activity.getString(R.string.cache_hint_not_available)));
    }

    @NonNull
    public GeoitemRef getGeoitemRef() {
        return new GeoitemRef(getGeocode(), getCoordType(), getGeocode(), 0, getName(), getType().markerId);
    }

    @NonNull
    public static String getAlternativeListingText(@NonNull final String alternativeCode) {
        return CgeoApplication.getInstance().getResources()
                .getString(R.string.cache_listed_on, GCConnector.getInstance().getName()) +
                ": <a href=\"https://coord.info/" +
                alternativeCode +
                "\">" +
                alternativeCode +
                "</a><br /><br />";
    }

    public boolean isGotoHistoryUDC() {
        return geocode.equals(InternalConnector.GEOCODE_HISTORY_CACHE);
    }

    @NonNull
    public Comparator<? super Waypoint> getWaypointComparator() {
        return isGotoHistoryUDC() ? Waypoint.WAYPOINT_ID_COMPARATOR : Waypoint.WAYPOINT_COMPARATOR;
    }

    public int getAssignedEmoji() {
        return assignedEmoji;
    }

    @NonNull
    public CacheVariableList getVariables() {
        if (variables == null) {
            variables = new CacheVariableList(this.geocode);
        }
        return variables;
    }

    /**
     * used for online search metainfos (e.g. finder)
     */
    @Nullable
    public String getSearchFinder() {
        return finder;
    }

    /**
     * used for online search metainfos (e.g. finder)
     */
    public void setSearchFinder(@Nullable final String finder) {
        this.finder = finder;
    }

    private class EventTimesInMin {
        public Integer start = null;
        public Integer end = null;

        public void reset() {
            this.start = null;
            this.end = null;
        }
    }
}
