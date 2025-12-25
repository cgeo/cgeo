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

package cgeo.geocaching.models

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.SearchCacheData
import cgeo.geocaching.SearchResult
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.AmendmentUtils
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.ILoggingManager
import cgeo.geocaching.connector.capability.IFavoriteCapability
import cgeo.geocaching.connector.capability.ISearchByGeocode
import cgeo.geocaching.connector.capability.WatchListCapability
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.connector.su.SuConnector
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.CoordinateType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogCacheActivity
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogTemplateProvider
import cgeo.geocaching.log.LogTemplateProvider.LogContext
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef
import cgeo.geocaching.models.bettercacher.Category
import cgeo.geocaching.models.bettercacher.Tier
import cgeo.geocaching.network.HtmlImage
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.DataStore.StorageLocation
import cgeo.geocaching.storage.Folder
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.EventTimeParser
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.LazyInitializedList
import cgeo.geocaching.utils.LazyInitializedSet
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper
import cgeo.geocaching.utils.functions.Func1
import cgeo.geocaching.utils.Formatter.generateShortGeocode

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Message
import android.util.Pair

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread
import androidx.core.text.HtmlCompat

import java.util.ArrayList
import java.util.Calendar
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.EnumMap
import java.util.EnumSet
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.LinkedList
import java.util.List
import java.util.ListIterator
import java.util.Locale
import java.util.Map
import java.util.Map.Entry
import java.util.Objects
import java.util.Set
import java.util.regex.Pattern

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.ListUtils
import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils


/**
 * Internal representation of a "cache"
 */
class Geocache : INamedGeoCoordinate {

    private var updated: Long = 0
    private var detailedUpdate: Long = 0
    private var visitedDate: Long = 0
    private val lists: Set<Integer> = HashSet<>()
    private var detailed: Boolean = false

    private var geocode: String = ""
    private var cacheId: String = ""
    private var guid: String = ""
    private var cacheType: CacheType = CacheType.UNKNOWN
    private var name: String = ""
    private var ownerDisplayName: String = ""
    private var ownerGuid: String = ""
    private var ownerUserId: String = ""
    private var assignedEmoji: Int = 0
    private var alcMode: Int = 0
    private Tier tier

    private var hidden: Date = null
    private var lastFound: Date = null
    /**
     * lazy initialized
     */
    private var hint: String = null
    private var size: CacheSize = CacheSize.UNKNOWN
    private var difficulty: Float = 0
    private var terrain: Float = 0
    private var direction: Float = null
    private var distance: Float = null
    /**
     * lazy initialized
     */
    private var location: String = null
    private var coords: Geopoint = null
    private val personalNote: PersonalNote = PersonalNote()
    /**
     * lazy initialized
     */
    private var shortdesc: String = null
    /**
     * lazy initialized
     */
    private var description: String = null
    private var disabled: Boolean = null
    private var archived: Boolean = null
    private var premiumMembersOnly: Boolean = null
    private var found: Boolean = null
    private var didNotFound: Boolean = null
    private var favorite: Boolean = null
    private var onWatchlist: Boolean = null
    private var watchlistCount: Int = -1; // valid numbers are larger than -1
    private var favoritePoints: Int = -1; // valid numbers are larger than -1
    private var rating: Float = 0; // valid ratings are larger than zero
    // FIXME: this makes no sense to favor this over the other. 0 should not be a special case here as it is
    // in the range of acceptable values. This is probably the case at other places (rating etc.) too.
    private var votes: Int = 0
    private var myVote: Float = 0.0f; // valid ratings are larger than zero
    private var inventoryItems: Int = -1
    private val attributes: LazyInitializedList<String> = LazyInitializedList<String>() {
        override         public List<String> call() {
            return inDatabase() ? DataStore.loadAttributes(geocode) : LinkedList<>()
        }
    }
    private val waypoints: LazyInitializedList<Waypoint> = LazyInitializedList<Waypoint>() {
        override         public List<Waypoint> call() {
            return inDatabase() ? DataStore.loadWaypoints(geocode) : LinkedList<>()
        }
    }
    private val categories: LazyInitializedSet<Category> = LazyInitializedSet<>(() ->
            inDatabase() ? DataStore.loadCategories(geocode) : null)
    private var spoilers: List<Image> = null

    private var inventory: List<Trackable> = null
    private var logCounts: Map<LogType, Integer> = EnumMap<>(LogType.class)
    private var userModifiedCoords: Boolean = null
    // temporary values
    private var statusChecked: Boolean = false
    private var directionImg: String = ""
    private String nameForSorting
    private val storageLocation: EnumSet<StorageLocation> = EnumSet.of(StorageLocation.HEAP)
    private var finalDefined: Boolean = false
    private var logPasswordRequired: Boolean = false
    private var preventWaypointsFromNote: Boolean = Settings.isGlobalWpExtractionDisabled()

    private var hasLogOffline: Boolean = null
    private var offlineLog: OfflineLogEntry = null

    private val eventTimesInMin: EventTimesInMin = EventTimesInMin()

    private static val NUMBER_PATTERN: Pattern = Pattern.compile("\\d+")

    private var changeNotificationHandler: Handler = null

    private CacheVariableList variables

    //transient field, used for online searches only
    private var searchCacheData: SearchCacheData = null

    public Unit setChangeNotificationHandler(final Handler newNotificationHandler) {
        changeNotificationHandler = newNotificationHandler
    }

    /** Sends a change notification for this Geocache to interested parties */
    public Unit notifyChange() {
        notifyChange(CgeoApplication.getInstance().getApplicationContext())
    }

    private Unit notifyChange(final Context context) {
        if (changeNotificationHandler != null) {
            changeNotificationHandler.sendEmptyMessage(0)
        }
        if (context != null) {
            GeocacheChangedBroadcastReceiver.sendBroadcast(context, geocode)
        }
    }

    /**
     * Gather missing information for Geocache object from the stored Geocache object.
     * This is called in the Geocache parsed from website to set information not yet
     * parsed.
     *
     * @param other the other version, or null if non-existent
     * @return true if this cache is "equal" to the other version
     */
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public Boolean gatherMissingFrom(final Geocache other) {
        if (other == null) {
            return false
        }
        if (other == this) {
            return true
        }

        updated = System.currentTimeMillis()

        // if parsed cache is not yet detailed and stored is, the information of
        // the parsed cache will be overwritten
        if (!detailed && other.detailed) {
            detailed = true
            detailedUpdate = other.detailedUpdate
            // Boolean values must be enumerated here. Other types are assigned outside this if-statement
            finalDefined = other.finalDefined

            if (StringUtils.isBlank(getHint())) {
                hint = other.getHint()
            }
            if (StringUtils.isBlank(getShortDescription())) {
                shortdesc = other.getShortDescription()
            }
            if (attributes.isEmpty() && other.attributes != null) {
                attributes.addAll(other.attributes)
            }
        }

        if (premiumMembersOnly == null) {
            premiumMembersOnly = other.premiumMembersOnly
        }
        if (found == null) {
            found = other.found
        }
        if (didNotFound == null) {
            didNotFound = other.didNotFound
        }
        if (disabled == null) {
            disabled = other.disabled
        }
        if (favorite == null) {
            favorite = other.favorite
        }
        if (archived == null) {
            archived = other.archived
        }
        if (onWatchlist == null) {
            onWatchlist = other.onWatchlist
        }
        if (hasLogOffline == null) {
            hasLogOffline = other.hasLogOffline
        }
        if (visitedDate == 0) {
            visitedDate = other.visitedDate
        }
        if (lists.isEmpty()) {
            lists.addAll(other.lists)
        }
        if (StringUtils.isBlank(geocode)) {
            geocode = other.geocode
        }
        if (StringUtils.isBlank(cacheId)) {
            cacheId = other.cacheId
        }
        if (StringUtils.isBlank(guid)) {
            guid = other.guid
        }
        if (cacheType == CacheType.UNKNOWN) {
            cacheType = other.cacheType
        }
        if (StringUtils.isBlank(name)) {
            name = other.name
        }
        if (StringUtils.isBlank(ownerDisplayName)) {
            ownerDisplayName = other.ownerDisplayName
        }
        if (StringUtils.isBlank(ownerUserId)) {
            ownerUserId = other.ownerUserId
        }
        if (hidden == null) {
            hidden = other.hidden
        }
        if (lastFound == null) {
            lastFound = other.lastFound
        }
        if (size == CacheSize.UNKNOWN) {
            size = other.size
        }
        if (difficulty == 0) {
            difficulty = other.difficulty
        }
        if (terrain == 0) {
            terrain = other.terrain
        }
        if (direction == null) {
            direction = other.direction
        }
        if (distance == null) {
            distance = other.distance
        }
        if (StringUtils.isBlank(getLocation())) {
            location = other.getLocation()
        }

        personalNote.gatherMissingDataFrom(other.personalNote)

        if (StringUtils.isBlank(getDescription())) {
            description = other.getDescription()
        }
        if (favoritePoints == -1) {
            favoritePoints = other.favoritePoints
        }
        if (rating == 0) {
            rating = other.rating
        }
        if (votes == 0) {
            votes = other.votes
        }
        if (myVote == 0) {
            myVote = other.myVote
        }
        if (tier == null || Tier.NONE == tier) {
            tier = other.tier
        }
        if (categories.isEmpty()) {
            setCategories(other.getCategories())
        }

        mergeWaypoints(other.waypoints, false)

        if (spoilers == null) {
            spoilers = other.spoilers
        }
        if (inventory == null) {
            // If inventoryItems is 0, it can mean both
            // "don't know" or "0 items". Since we cannot distinguish
            // them here, only populate inventoryItems from
            // old data when we have to do it for inventory.
            setInventory(other.inventory)
        }
        if (logCounts.isEmpty()) {
            logCounts = other.logCounts
        }


        val takeOverUserModCoords: Boolean = !hasUserModifiedCoords()
        if (userModifiedCoords == null) {
            userModifiedCoords = other.userModifiedCoords
        }

        if (takeOverUserModCoords && other.hasUserModifiedCoords()) {
            val original: Waypoint = other.getOriginalWaypoint()
            if (original != null) {
                original.setCoords(getCoords())
            }
            setCoords(other.getCoords())
        } else {
            if (coords == null) {
                setCoords(other.coords)
            }
        }
        // if cache has ORIGINAL type waypoint ... it is considered that it has modified coordinates, otherwise not
        if (getOriginalWaypoint() != null) {
            userModifiedCoords = true
        }

        if (!preventWaypointsFromNote) {
            preventWaypointsFromNote = other.preventWaypointsFromNote
        }

        if (assignedEmoji == 0) {
            assignedEmoji = other.assignedEmoji
        }

        if (searchCacheData == null) {
            searchCacheData = other.searchCacheData
        }

        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
        return isEqualTo(other)
    }

    public Unit mergeWaypoints(final List<Waypoint> otherWaypoints, final Boolean forceMerge) {
        if (waypoints.isEmpty()) {
            this.setWaypoints(otherWaypoints)
        } else {
            val newPoints: List<Waypoint> = ArrayList<>(waypoints)
            Waypoint.mergeWayPoints(newPoints, otherWaypoints, forceMerge)
            this.setWaypoints(newPoints)
        }
    }

    /**
     * Returns the Original Waypoint if exists
     */
    public Waypoint getOriginalWaypoint() {
        return getFirstMatchingWaypoint(wpt -> wpt.getWaypointType() == WaypointType.ORIGINAL)
    }

    /**
     * Returns the first found Waypoint matching the given condition
     */
    public Waypoint getFirstMatchingWaypoint(final Func1<Waypoint, Boolean> condition) {
        for (final Waypoint wpt : waypoints) {
            if (wpt != null && condition.call(wpt)) {
                return wpt
            }
        }
        return null
    }

    /**
     * Compare two caches quickly. For map and list fields only the references are compared !
     *
     * @param other the other cache to compare this one to
     * @return true if both caches have the same content
     */
    @SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
    private Boolean isEqualTo(final Geocache other) {
        return detailed == other.detailed &&
                StringUtils.equalsIgnoreCase(geocode, other.geocode) &&
                StringUtils.equalsIgnoreCase(name, other.name) &&
                cacheType == (other.cacheType) &&
                size == other.size &&
                Objects == (found, other.found) &&
                Objects == (didNotFound, other.didNotFound) &&
                Objects == (premiumMembersOnly, other.premiumMembersOnly) &&
                difficulty == other.difficulty &&
                terrain == other.terrain &&
                Objects == (coords, other.coords) &&
                Objects == (disabled, other.disabled) &&
                Objects == (archived, other.archived) &&
                Objects == (lists, other.lists) &&
                StringUtils.equalsIgnoreCase(ownerDisplayName, other.ownerDisplayName) &&
                StringUtils.equalsIgnoreCase(ownerUserId, other.ownerUserId) &&
                StringUtils.equalsIgnoreCase(getDescription(), other.getDescription()) &&
                Objects == (personalNote, other.personalNote) &&
                StringUtils.equalsIgnoreCase(getShortDescription(), other.getShortDescription()) &&
                StringUtils.equalsIgnoreCase(getLocation(), other.getLocation()) &&
                Objects == (favorite, other.favorite) &&
                favoritePoints == other.favoritePoints &&
                Objects == (onWatchlist, other.onWatchlist) &&
                Objects == (hidden, other.hidden) &&
                Objects == (lastFound, other.lastFound) &&
                StringUtils.equalsIgnoreCase(guid, other.guid) &&
                StringUtils.equalsIgnoreCase(getHint(), other.getHint()) &&
                StringUtils.equalsIgnoreCase(cacheId, other.cacheId) &&
                Objects == (direction, other.direction) &&
                Objects == (distance, other.distance) &&
                rating == other.rating &&
                votes == other.votes &&
                myVote == other.myVote &&
                inventoryItems == other.inventoryItems &&
                attributes == (other.attributes) &&
                waypoints == (other.waypoints) &&
                Objects == (spoilers, other.spoilers) &&
                Objects == (inventory, other.inventory) &&
                Objects == (logCounts, other.logCounts) &&
                Objects == (hasLogOffline, other.hasLogOffline) &&
                finalDefined == other.finalDefined &&
                Objects == (searchCacheData, other.searchCacheData)
    }

    public Boolean hasTrackables() {
        return inventoryItems > 0
    }

    public Int getTrackableCount() {
        return inventoryItems
    }

    public Boolean canBeAddedToCalendar() {
        // Is event type with event date set?
        return isEventCache() && hidden != null
    }

    public Boolean isEventCache() {
        return cacheType.isEvent()
    }

    // returns whether the current cache is a future event, for which
    // the user has logged a "will attend", but no "attended" yet
    public Boolean hasWillAttendForFutureEvent() {
        if (!isEventCache()) {
            return false
        }
        val eventDate: Date = getHiddenDate()
        val expired: Boolean = CalendarUtils.isPastEvent(this)
        if (eventDate == null || expired) {
            return false
        }

        Boolean willAttend = false
        val logs: List<LogEntry> = getLogs()
        for (final LogEntry logEntry : logs) {
            val logType: LogType = logEntry.logType
            if (logType == LogType.ATTENDED) {
                return false
            } else if (logType == LogType.WILL_ATTEND && logEntry.isOwn()) {
                willAttend = true
            }
        }
        return willAttend
    }

    public Unit logVisit(final Activity fromActivity) {
        if (!getConnector().canLog(this)) {
            ActivityMixin.showToast(fromActivity, fromActivity.getString(R.string.err_cannot_log_visit))
            return
        }
        String geocode = this.geocode
        if (StringUtils.isBlank(geocode)) {
            geocode = DataStore.getGeocodeForGuid(this.cacheId)
        }
        LogCacheActivity.startForCreate(fromActivity, geocode)
    }

    public Unit logVisitForResult(final Activity fromActivity, final Int requestCode) {
        if (!getConnector().canLog(this)) {
            ActivityMixin.showToast(fromActivity, fromActivity.getString(R.string.err_cannot_log_visit))
            return
        }
        String geocode = this.geocode
        if (StringUtils.isBlank(geocode)) {
            geocode = DataStore.getGeocodeForGuid(this.cacheId)
        }
        LogCacheActivity.startForCreateForResult(fromActivity, geocode, requestCode)
    }


    public Boolean hasLogOffline() {
        return BooleanUtils.isTrue(hasLogOffline)
    }

    public Unit setHasLogOffline(final Boolean hasLogOffline) {
        this.hasLogOffline = hasLogOffline
    }

    public Unit logOffline(final Activity fromActivity, final LogType logType, final ReportProblemType reportProblem) {
        val mustIncludeSignature: Boolean = StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature()
        val initial: String = mustIncludeSignature ? LogTemplateProvider.applyTemplates(Settings.getSignature(), LogContext(this, null, true)) : ""

        logOffline(fromActivity, OfflineLogEntry.Builder()
                .setLog(initial)
                .setDate(Calendar.getInstance().getTimeInMillis())
                .setLogType(logType)
                .setReportProblem(reportProblem)
                .build()
        )
    }

    /**
     * If the cache already has an offline log with log text and the offline log would overwrite that prompt for confirmation
     */
    public Unit logOffline(final Activity fromActivity, final OfflineLogEntry logEntry) {
        if (hasLogOffline() && !getOfflineLog().log.isEmpty() && !getOfflineLog().log == (logEntry.log)) {
            SimpleDialog.of(fromActivity)
                    .setTitle(R.string.caches_overwrite_offline_log)
                    .setMessage(R.string.caches_overwrite_offline_log_question, getOfflineLog().logType.getL10n())
                    .confirm(() -> storeLogOffline(fromActivity, logEntry))
        } else {
            storeLogOffline(fromActivity, logEntry)
        }
    }

    public Unit storeLogOffline(final Activity fromActivity, final OfflineLogEntry logEntry) {

        if (logEntry.logType == LogType.UNKNOWN) {
            return
        }

        if (!isOffline()) {
            getLists().add(StoredList.STANDARD_LIST_ID)
            DataStore.saveCache(this, LoadFlags.SAVE_ALL)
        }

        val status: Boolean = DataStore.saveLogOffline(geocode, logEntry)

        val res: Resources = fromActivity.getResources()
        if (status) {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.info_log_saved))
            DataStore.saveVisitDate(geocode, logEntry.date)
            hasLogOffline = Boolean.TRUE
            if (Settings.removeFromRouteOnLog()) {
                DataStore.removeFirstMatchingIdFromIndividualRoute(fromActivity, geocode)
            }
            offlineLog = logEntry
            notifyChange(fromActivity)
        } else {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.err_log_post_failed))
        }
    }

    /**
     * Get the Offline Log entry if any.
     *
     * @return The Offline LogEntry
     */
    public OfflineLogEntry getOfflineLog() {
        if (!BooleanUtils.isFalse(hasLogOffline) && offlineLog == null) {
            offlineLog = DataStore.loadLogOffline(geocode)
            setHasLogOffline(offlineLog != null)
        }
        return offlineLog
    }

    /**
     * Get the Offline Log entry if any.
     *
     * @return The Offline LogEntry else Null
     */
    public LogType getOfflineLogType() {
        val offlineLog: LogEntry = getOfflineLog()
        if (offlineLog == null) {
            return null
        }
        return offlineLog.logType
    }

    /**
     * Drop offline log for a given geocode.
     */
    public Unit clearOfflineLog(final Context context) {
        DataStore.clearLogOffline(geocode)
        setHasLogOffline(false)
        notifyChange(context)
    }

    public List<LogType> getPossibleLogTypes() {
        return getConnector().getPossibleLogTypes(this)
    }

    /**
     * Get the browser URL for the given LogEntry. May return null if no url available or identifiable.
     */
    public String getLogUrl(final LogEntry logEntry) {
        return getConnector().getCacheLogUrl(this, logEntry)
    }

    /**
     * Get a browser URL to create a log entry. May return null if connector does not support this.
     */
    public String getCreateNewLogUrl() {
        return getConnector().getCacheCreateNewLogUrl(this)
    }

    private IConnector getConnector() {
        return ConnectorFactory.getConnector(this)
    }

    public Boolean supportsRefresh() {
        return getConnector() is ISearchByGeocode && !InternalConnector.getInstance() == (getConnector())
    }

    public Boolean supportsWatchList() {
        val connector: IConnector = getConnector()
        return (connector is WatchListCapability) && ((WatchListCapability) connector).canAddToWatchList(this)
    }

    public Boolean supportsFavoritePoints() {
        val connector: IConnector = getConnector()
        return (connector is IFavoriteCapability) && ((IFavoriteCapability) connector).supportsFavoritePoints(this)
    }

    public Boolean supportsLogging() {
        return getConnector().supportsLogging()
    }

    public Boolean supportsEditLog(final LogEntry logEntry) {
        return getConnector().canEditLog(this, logEntry)
    }

    public Boolean supportsDeleteLog(final LogEntry logEntry) {
        return getConnector().canDeleteLog(this, logEntry)
    }

    public Boolean supportsOwnCoordinates() {
        return getConnector().supportsOwnCoordinates()
    }

    public ILoggingManager getLoggingManager() {
        return getConnector().getLoggingManager(this)
    }

    public Boolean supportsDifficultyTerrain() {
        return getConnector().supportsDifficultyTerrain()
    }

    public Float getDifficulty() {
        return difficulty
    }

    override     public String getGeocode() {
        return geocode
    }

    public String getShortGeocode() {
        return generateShortGeocode(geocode)
    }

    /**
     * @return displayed owner, might differ from the real owner
     */
    public String getOwnerDisplayName() {
        return ownerDisplayName
    }

    public String getOwnerGuid() {
        return ownerGuid
    }

    public CacheSize getSize() {
        return size
    }

    public Float getTerrain() {
        return terrain
    }

    public Boolean isArchived() {
        return BooleanUtils.isTrue(archived)
    }

    public Boolean isDisabled() {
        // a cache can never be disabled and archived at the same time, so the archived state should win (see #11428)
        return !isArchived() && BooleanUtils.isTrue(disabled)
    }

    public Boolean isEnabled() {
        return !isDisabled() && !isArchived()
    }

    public Boolean isPremiumMembersOnly() {
        return BooleanUtils.isTrue(premiumMembersOnly)
    }

    public Boolean isPremiumMembersOnlyRaw() {
        return premiumMembersOnly
    }

    public Unit setPremiumMembersOnly(final Boolean members) {
        this.premiumMembersOnly = members
    }

    /**
     * @return {@code true} if the user is the owner of the cache, {@code false} otherwise
     */
    public Boolean isOwner() {
        return getConnector().isOwner(this)
    }

    /**
     * @return GC username of the (actual) owner, might differ from the owner. Never empty.
     */
    public String getOwnerUserId() {
        return ownerUserId
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     *
     * @return the decrypted hint
     */
    public String getHint() {
        initializeCacheTexts()
        assertTextNotNull(hint, "Hint")
        return hint
    }

    /**
     * After lazy loading the lazily loaded field must be non {@code null}.
     */
    private static Unit assertTextNotNull(final String field, final String name) throws InternalError {
        if (field == null) {
            throw InternalError(name + " field is not allowed to be null here")
        }
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    public String getDescription() {
        initializeCacheTexts()
        assertTextNotNull(description, "Description")
        return description
    }

    /**
     * loads Long text parts of a cache on demand (but all fields together)
     */
    private Unit initializeCacheTexts() {
        if (description == null || shortdesc == null || hint == null || location == null) {
            if (inDatabase()) {
                val partial: Geocache = DataStore.loadCacheTexts(this.getGeocode())
                if (description == null) {
                    setDescription(partial.getDescription())
                }
                if (shortdesc == null) {
                    setShortDescription(partial.getShortDescription())
                }
                if (hint == null) {
                    setHint(partial.getHint())
                }
                if (location == null) {
                    setLocation(partial.getLocation())
                }
            } else {
                setDescription(StringUtils.defaultString(description))
                setShortDescription(StringUtils.defaultString(shortdesc))
                setHint(StringUtils.defaultString(hint))
                setLocation(StringUtils.defaultString(location))
            }
        }
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    public String getShortDescription() {
        initializeCacheTexts()
        assertTextNotNull(shortdesc, "Short description")
        return shortdesc
    }

    override     public String getName() {
        return name
    }

    public String getCacheId() {
        // For some connectors ID can be calculated out of geocode
        if (StringUtils.isBlank(cacheId)) {
            if (getConnector() is GCConnector) {
                return String.valueOf(GCUtils.gcLikeCodeToGcLikeId(geocode))
            }
            if (getConnector() is SuConnector) {
                return SuConnector.geocodeToId(geocode)
            }
        }

        return cacheId
    }

    public String getGuid() {
        return guid
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    public String getLocation() {
        initializeCacheTexts()
        assertTextNotNull(location, "Location")
        return location
    }

    public String getPersonalNote() {
        return this.personalNote.getNote()
    }

    public Boolean supportsNamechange() {
        return getConnector().supportsNamechange()
    }

    public Boolean supportsDescriptionchange() {
        return getConnector().supportsDescriptionchange()
    }

    public Boolean supportsSettingFoundState() {
        return getConnector().supportsSettingFoundState()
    }

    public String getShareSubject() {
        val subject: StringBuilder = StringBuilder("Geocache ")
        subject.append(geocode)
        if (StringUtils.isNotBlank(name)) {
            subject.append(" - ").append(name)
        }
        return subject.toString()
    }

    public String getServiceSpecificLogId(final LogEntry logEntry) {
        if (logEntry == null) {
            return null
        }
        return getConnector().getServiceSpecificLogId(logEntry.serviceLogId)
    }

    public String getUrl() {
        return getConnector().getCacheUrl(this)
    }

    public String getLongUrl() {
        return getConnector().getLongCacheUrl(this)
    }

    public Unit setDescription(final String description) {
        this.description = description
        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
    }

    public Boolean isFound() {
        return BooleanUtils.isTrue(found)
    }

    public Boolean isDNF() {
        return BooleanUtils.isTrue(didNotFound)
    }

    /**
     * @return {@code true} if the user has put a favorite point onto this cache
     */
    public Boolean isFavorite() {
        return BooleanUtils.isTrue(favorite)
    }

    public Boolean isFavoriteRaw() {
        return favorite
    }

    public Unit setFavorite(final Boolean favorite) {
        this.favorite = favorite
    }

    public Date getHiddenDate() {
        if (hidden != null) {
            return Date(hidden.getTime())
        }
        return null
    }

    public Date getLastFound() {
        if (lastFound == null && inDatabase()) {
            for (LogEntry logEntry : getLogs()) {
                if (logEntry.logType.isFoundLog()) {
                    lastFound = Date(logEntry.date)
                    break
                }
            }
        }
        if (lastFound != null) {
            return Date(lastFound.getTime())
        }
        return null
    }

    public List<String> getAttributes() {
        return attributes.getUnderlyingList()
    }

    public Set<Category> getCategories() {
        return categories.getUnderlyingSet()
    }

    public List<Image> getSpoilers() {
        return ListUtils.unmodifiableList(ListUtils.emptyIfNull(spoilers))
    }

    /** applies filter/plausibilization logic to cache spoilers */
    public List<Image> getFilteredSpoilers() {
        if (getSpoilers().isEmpty()) {
            return Collections.emptyList()
        }
        val listingUrls: Set<String> = HashSet<>()
        ImageUtils.forEachImageUrlInHtml(s -> listingUrls.add(ImageUtils.imageUrlForSpoilerCompare(s)), getShortDescription(), getDescription())
        val result: List<Image> = ArrayList<>()
        for (Image spoilerCandidate : getSpoilers()) {
            val spoilerInTitle: Boolean = spoilerCandidate.getTitle() != null &&
                    spoilerCandidate.getTitle().toLowerCase(Locale.US).contains("spoiler")
            val containedInListing: Boolean = listingUrls.contains(ImageUtils.imageUrlForSpoilerCompare(spoilerCandidate.getUrl()))
            if (spoilerInTitle || !containedInListing) {
                result.add(spoilerCandidate)
            }
        }
        return result
    }

    /**
     * @return a statistic how often the caches has been found, disabled, archived etc.
     */
    public Map<LogType, Integer> getLogCounts() {
        if (logCounts.isEmpty() && inDatabase()) {
            val savedLogCounts: Map<LogType, Integer> = DataStore.loadLogCounts(getGeocode())
            if (MapUtils.isNotEmpty(savedLogCounts)) {
                logCounts = savedLogCounts
            }
        }
        return logCounts
    }

    public Int getFavoritePoints() {
        return favoritePoints
    }

    /**
     * @return the normalized cached name to be used for sorting, taking into account the numerical parts in the name
     */
    public String getNameForSorting() {
        if (nameForSorting == null) {
            nameForSorting = name
            // pad each number part to a fixed size of 6 digits, so that numerical sorting becomes equivalent to string sorting
            MatcherWrapper matcher = MatcherWrapper(NUMBER_PATTERN, nameForSorting)
            Int start = 0
            while (matcher.find(start)) {
                val number: String = matcher.group()
                nameForSorting = StringUtils.substring(nameForSorting, 0, matcher.start()) + StringUtils.leftPad(number, 6, '0') + StringUtils.substring(nameForSorting, matcher.start() + number.length())
                start = matcher.start() + Math.max(6, number.length())
                matcher = MatcherWrapper(NUMBER_PATTERN, nameForSorting)
            }
        }
        return nameForSorting
    }

    public Boolean isVirtual() {
        return cacheType.isVirtual()
    }

    public Boolean showSize() {
        return !(size == CacheSize.NOT_CHOSEN || isEventCache() || isVirtual())
    }

    public Long getUpdated() {
        return updated
    }

    public Unit setUpdated(final Long updated) {
        this.updated = updated
    }

    public Long getDetailedUpdate() {
        return detailedUpdate
    }

    public Unit setDetailedUpdate(final Long detailedUpdate) {
        this.detailedUpdate = detailedUpdate
    }

    public Long getVisitedDate() {
        return visitedDate
    }

    public Unit setVisitedDate(final Long visitedDate) {
        this.visitedDate = visitedDate
    }

    public Set<Integer> getLists() {
        return lists
    }

    public Unit setLists(final Set<Integer> lists) {
        //special case: own list is passed. Ignore that. See #16505
        if (lists == this.lists || this.lists == (lists)) {
            return
        }
        this.lists.clear()
        if (lists != null) {
            this.lists.addAll(lists)
        }
    }

    public Boolean isDetailed() {
        return detailed
    }

    public Unit setDetailed(final Boolean detailed) {
        this.detailed = detailed
    }

    public Unit setHidden(final Date hidden) {
        this.hidden = hidden != null ? Date(hidden.getTime()) : null
    }

    public Unit setLastFound(final Date lastFound) {
        this.lastFound = lastFound != null ? Date(lastFound.getTime()) : null
    }

    public Float getDirection() {
        return direction
    }

    public Unit setDirection(final Float direction) {
        this.direction = direction
    }

    public Float getDistance() {
        return distance
    }

    public Unit setDistance(final Float distance) {
        this.distance = distance
    }

    override     public Geopoint getCoords() {
        return coords
    }

    /**
     * Set reliable coordinates
     */
    public Unit setCoords(final Geopoint coords) {
        if (this.coords != null && coords == null) {
            Log.w("Geocache: setting non-null-coordinates to null for cache´" + geocode + " (was: " + coords + ")")
        }
        this.coords = coords
    }

    public Unit setShortDescription(final String shortdesc) {
        this.shortdesc = shortdesc
        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
    }

    public Unit setFavoritePoints(final Int favoriteCnt) {
        this.favoritePoints = favoriteCnt
    }

    public Float getRating() {
        return rating
    }

    public Unit setRating(final Float rating) {
        this.rating = rating
    }

    public Int getVotes() {
        return votes
    }

    public Unit setVotes(final Int votes) {
        this.votes = votes
    }

    public Float getMyVote() {
        return myVote
    }

    public Unit setMyVote(final Float myVote) {
        this.myVote = myVote
    }

    /**
     * Get the current inventory count
     *
     * @return the inventory size
     */
    public Int getInventoryItems() {
        return Math.max(inventoryItems, 0)
    }

    public Boolean hasInventoryItemsSet() {
        return inventoryItems >= 0
    }

    /**
     * Set the current inventory count
     *
     * @param inventoryItems the inventory size
     */
    public Unit setInventoryItems(final Int inventoryItems) {
        this.inventoryItems = inventoryItems
    }

    /**
     * Get the current inventory
     *
     * @return the inventory of Trackables as unmodifiable collection. Use {@link #setInventory(List)} or
     * {@link #addInventoryItem(Trackable)} for modifications.
     */
    public List<Trackable> getInventory() {
        return inventory == null ? Collections.emptyList() : Collections.unmodifiableList(inventory)
    }

    /**
     * Replace the inventory with content.
     * No checks are performed.
     *
     * @param newInventory to set on Geocache
     */
    public Unit setInventory(final List<Trackable> newInventory) {
        inventory = newInventory
        inventoryItems = CollectionUtils.size(inventory)
    }

    /**
     * Add Trackables to inventory safely.
     * This takes care of removing old items if they are from the same brand.
     * If items are present, data is merged, not duplicated.
     *
     * @param newTrackables to be added to the Geocache
     */
    public Unit mergeInventory(final List<Trackable> newTrackables, final EnumSet<TrackableBrand> processedBrands) {

        val mergedTrackables: List<Trackable> = ArrayList<>(newTrackables)

        for (final Trackable trackable : ListUtils.emptyIfNull(inventory)) {
            if (processedBrands.contains(trackable.getBrand())) {
                val iterator: ListIterator<Trackable> = mergedTrackables.listIterator()
                while (iterator.hasNext()) {
                    val newTrackable: Trackable = iterator.next()
                    if (trackable.getUniqueID() == (newTrackable.getUniqueID())) {
                        // Respect the merge order. New Values replace existing values.
                        trackable.mergeTrackable(newTrackable)
                        iterator.set(trackable)
                        break
                    }
                }
            } else {
                mergedTrackables.add(trackable)
            }
        }
        setInventory(mergedTrackables)
    }

    /**
     * Add Trackable to inventory safely.
     * If items are present, data are merged, not duplicated.
     *
     * @param newTrackable to be added to the Geocache
     */
    public Unit addInventoryItem(final Trackable newTrackable) {
        if (inventory == null) {
            inventory = ArrayList<>()
        }
        Boolean foundTrackable = false
        for (final Trackable trackable : inventory) {
            if (trackable.getUniqueID() == (newTrackable.getUniqueID())) {
                // Trackable already present, merge data
                foundTrackable = true
                trackable.mergeTrackable(newTrackable)
                break
            }
        }
        if (!foundTrackable) {
            inventory.add(newTrackable)
        }
        inventoryItems = inventory.size()
    }

    /**
     * @return {@code true} if the cache is on the user's watchlist, {@code false} otherwise
     */
    public Boolean isOnWatchlist() {
        return BooleanUtils.isTrue(onWatchlist)
    }

    public Boolean isOnWatchlistRaw() {
        return onWatchlist
    }

    public Unit setOnWatchlist(final Boolean onWatchlist) {
        this.onWatchlist = onWatchlist
    }

    public Boolean isLinearAlc() {
        Log.d("_AL isLinearAlc: " + alcMode)
        return alcMode == 1
    }

    public Unit setAlcMode(final Int alcMode) {
        Log.d("_AL setAlcMode: " + alcMode)
        this.alcMode = alcMode
    }

    public Int getAlcMode() {
        Log.d("_AL getAlcMode: " + alcMode)
        return alcMode
    }

    public Unit setTier(final Tier tier) {
        this.tier = tier
    }

    public Tier getTier() {
        return this.tier
    }

    /**
     * Set the number of users watching this geocache
     *
     * @param watchlistCount Number of users watching this geocache
     */
    public Unit setWatchlistCount(final Int watchlistCount) {
        this.watchlistCount = watchlistCount
    }

    /**
     * get the number of users watching this geocache
     *
     * @return watchlistCount Number of users watching this geocache
     */
    public Int getWatchlistCount() {
        return watchlistCount
    }

    /**
     * return an immutable list of waypoints.
     *
     * @return always non {@code null}
     */
    public List<Waypoint> getWaypoints() {
        return waypoints.getUnderlyingList()
    }

    public List<Waypoint> getSortedWaypointList() {
        if (hasWaypoints()) {
            val waypoints: List<Waypoint> = getWaypoints()
            Collections.sort(waypoints, getWaypointComparator())
            return waypoints
        }
        return Collections.emptyList()
    }

    /**
     * @param waypoints      List of waypoints to set for cache
     */
    public Unit setWaypoints(final List<Waypoint> waypoints) {
        this.waypoints.clear()
        if (waypoints != null) {
            this.waypoints.addAll(waypoints)
        }
        if (waypoints != null) {
            for (final Waypoint waypoint : waypoints) {
                waypoint.setGeocode(geocode)
            }
        }
        resetFinalDefined()
    }

    /**
     * The list of logs is immutable, because it is directly fetched from the database on demand, and not stored at this
     * object. If you want to modify logs, you have to load all logs of the cache, create a list from the existing
     * list and store that list in the database.
     *
     * @return immutable list of logs
     */
    public List<LogEntry> getLogs() {
        //if a cache was freshly loaded from server, it may not have the "logs" flag although logs exist in local db.
        return DataStore.loadLogs(geocode)
        //return inDatabase() ? DataStore.loadLogs(geocode) : Collections.emptyList()
    }

    /**
     * @return only the logs of friends
     */
    public List<LogEntry> getFriendsLogs() {
        val friendLogs: List<LogEntry> = ArrayList<>()
        for (final LogEntry log : getLogs()) {
            if (log.friend) {
                friendLogs.add(log)
            }
        }
        return Collections.unmodifiableList(friendLogs)
    }

    public Boolean isStatusChecked() {
        return statusChecked
    }

    public Unit setStatusChecked(final Boolean statusChecked) {
        this.statusChecked = statusChecked
    }

    public String getDirectionImg() {
        return directionImg
    }

    public Unit setDirectionImg(final String directionImg) {
        this.directionImg = directionImg
    }

    public Unit setGeocode(final String geocode) {
        this.geocode = geocode == null ? "" : StringUtils.upperCase(geocode)
    }

    public Unit setCacheId(final String cacheId) {
        this.cacheId = cacheId
    }

    public Unit setGuid(final String guid) {
        this.guid = guid
    }

    public Unit setName(final String name) {
        this.name = name
    }

    public Unit setOwnerDisplayName(final String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName
    }

    public Unit setOwnerGuid(final String ownerGuid) {
        this.ownerGuid = ownerGuid
    }

    public Unit setAssignedEmoji(final Int assignedEmoji) {
        this.assignedEmoji = assignedEmoji
    }

    public Unit setOwnerUserId(final String ownerUserId) {
        this.ownerUserId = ownerUserId
    }

    public Unit setHint(final String hint) {
        this.hint = hint
    }

    public Unit setSize(final CacheSize size) {
        this.size = size
    }

    public Unit setDifficulty(final Float difficulty) {
        this.difficulty = difficulty
    }

    public Unit setTerrain(final Float terrain) {
        this.terrain = terrain
    }

    public Unit setLocation(final String location) {
        this.location = location
    }

    public Unit setPersonalNote(final String personalNote) {
        setPersonalNote(personalNote, false)
    }

    public Unit setPersonalNote(final String personalNote, final Boolean isFromProvider) {
        this.personalNote.setNote(personalNote)
        this.personalNote.setFromProvider(isFromProvider)
    }

    public Unit setDisabled(final Boolean disabled) {
        this.disabled = disabled
    }

    public Unit setArchived(final Boolean archived) {
        this.archived = archived
    }

    public Unit setFound(final Boolean found) {
        this.found = found
    }

    public Unit setDNF(final Boolean didNotFound) {
        this.didNotFound = didNotFound
    }

    public Unit setAttributes(final List<String> attributes) {
        this.attributes.clear()
        if (attributes != null) {
            this.attributes.addAll(attributes)
        }
    }

    public Unit setCategories(final Collection<Category> categories) {
        this.categories.clear()
        if (categories != null) {
            this.categories.addAll(CollectionStream.of(categories).filter(Category::isValid).toList())
        }
    }

    public Unit setSpoilers(final List<Image> spoilers) {
        this.spoilers = spoilers
    }

    public Boolean hasSpoilersSet() {
        return this.spoilers != null
    }

    public Unit setLogCounts(final Map<LogType, Integer> logCounts) {
        this.logCounts = logCounts
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.IBasicCache#getType()
     *
     * @returns Never null
     */
    public CacheType getType() {
        return cacheType
    }

    public Unit setType(final CacheType cacheType) {
        if (cacheType == null || cacheType == CacheType.ALL) {
            throw IllegalArgumentException("Illegal cache type")
        }
        this.cacheType = cacheType
        this.eventTimesInMin.reset(); // will be recalculated if/when necessary
    }

    public Boolean hasDifficulty() {
        return difficulty > 0f
    }

    public Boolean hasTerrain() {
        return terrain > 0f
    }

    /**
     * @param storageLocation the storageLocation to set
     */
    public Unit addStorageLocation(final StorageLocation storageLocation) {
        this.storageLocation.add(storageLocation)
    }

    /**
     * Check if this cache instance comes from or has been stored into the database.
     */
    public Boolean inDatabase() {
        return storageLocation.contains(StorageLocation.DATABASE)
    }

    /**
     * @param waypoint       Waypoint to add to the cache
     * @param saveToDatabase Indicates whether to add the waypoint to the database. Should be false if
     *                       called while loading or building a cache
     * @return {@code true} if waypoint successfully added to waypoint database
     */
    public Boolean addOrChangeWaypoint(final Waypoint waypoint, final Boolean saveToDatabase) {
        waypoint.setGeocode(geocode)

        if (waypoint.isNewWaypoint()) {
            if (StringUtils.isBlank(waypoint.getPrefix())) {
                assignUniquePrefix(waypoint)
            }
            waypoints.add(waypoint)
            if (waypoint.isFinalWithCoords()) {
                finalDefined = true
            }
        } else { // this is a waypoint being edited
            val index: Int = getWaypointIndex(waypoint)
            if (index >= 0) {
                val oldWaypoint: Waypoint = waypoints.remove(index)
                waypoint.setPrefix(oldWaypoint.getPrefix())
                //migration
                if (StringUtils.isBlank(waypoint.getPrefix())
                        || StringUtils.equalsIgnoreCase(waypoint.getPrefix(), Waypoint.PREFIX_OWN)) {
                    assignUniquePrefix(waypoint)
                }
            }
            waypoints.add(waypoint)
            // when waypoint was edited, finalDefined may have changed
            resetFinalDefined()
        }
        return saveToDatabase && DataStore.saveWaypoint(waypoint.getId(), geocode, waypoint)
    }

    private Unit assignUniquePrefix(final Waypoint waypoint) {
        Waypoint.assignUniquePrefix(waypoint, waypoints)
    }

    public Boolean hasWaypoints() {
        return !waypoints.isEmpty()
    }

    public Boolean hasUserdefinedWaypoints() {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.isUserDefined()) {
                return true
            }
        }
        return false
    }

    public Boolean hasGeneratedWaypoints() {
        for (Waypoint waypoint : waypoints) {
            if (waypoint.getWaypointType() == WaypointType.GENERATED) {
                return true
            }
        }
        return false
    }

    public Boolean hasFinalDefined() {
        return finalDefined
    }

    // Only for loading
    public Unit setFinalDefined(final Boolean finalDefined) {
        this.finalDefined = finalDefined
    }

    /**
     * Reset {@code finalDefined} based on current list of stored waypoints
     */
    private Unit resetFinalDefined() {
        finalDefined = getFirstMatchingWaypoint(Waypoint::isFinalWithCoords) != null
    }

    public Boolean hasUserModifiedCoords() {
        return BooleanUtils.isTrue(userModifiedCoords)
    }

    public Boolean getUserModifiedCoordsRaw() {
        return userModifiedCoords
    }

    public Unit setUserModifiedCoords(final Boolean coordsChanged) {
        userModifiedCoords = coordsChanged
    }

    public Unit resetUserModifiedCoords(final Waypoint waypoint) {
        setCoords(waypoint.getCoords())
        setUserModifiedCoords(false)
        deleteWaypointForce(waypoint)
        DataStore.saveUserModifiedCoords(this)
    }

    public Unit createOriginalWaypoint(final Geopoint originalCoords) {
        if (originalCoords != null) {
            val waypoint: Waypoint = Waypoint(CgeoApplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false)
            waypoint.setCoords(originalCoords)
            addOrChangeWaypoint(waypoint, false)
            setUserModifiedCoords(true)
        }
    }

    /**
     * Duplicate a waypoint.
     *
     * @param original the waypoint to duplicate
     * @return the copy of the waypoint if it was duplicated, {@code null} otherwise (invalid index)
     */
    public Waypoint duplicateWaypoint(final Waypoint original, final Boolean addPrefix) {
        if (original == null) {
            return null
        }
        val index: Int = getWaypointIndex(original)
        val copy: Waypoint = Waypoint(original)
        copy.setName((addPrefix ? CgeoApplication.getInstance().getString(R.string.waypoint_copy_of) + " " : "") + copy.getName())

        // create unique prefix
        copy.setUserDefined()
        val basePrefix: String = copy.getPrefix()
        Int counter = 0
        Boolean found = true
        while (found) {
            found = false
            for (Waypoint waypoint : waypoints) {
                if (StringUtils == (waypoint.getPrefix(), copy.getPrefix())) {
                    found = true
                    break
                }
            }
            if (found) {
                counter++
                copy.setPrefix(basePrefix + "-" + counter)
            }
        }

        waypoints.add(index + 1, copy)
        return DataStore.saveWaypoint(-1, geocode, copy) ? copy : null
    }

    /**
     * delete a user-defined waypoint
     *
     * @param waypoint to be removed from cache
     * @return {@code true}, if the waypoint was deleted
     */
    public Boolean deleteWaypoint(final Waypoint waypoint) {
        if (waypoint == null) {
            return false
        }
        if (waypoint.isNewWaypoint()) {
            return false
        }
        if (waypoint.getWaypointType() != WaypointType.ORIGINAL || waypoint.belongsToUserDefinedCache()) {
            val index: Int = getWaypointIndex(waypoint)
            waypoints.remove(index)
            DataStore.deleteWaypoint(waypoint.getId())
            DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE))
            // Check status if Final is defined
            if (waypoint.isFinalWithCoords()) {
                resetFinalDefined()
            }
            return true
        }
        return false
    }

    /**
     * deletes any waypoint
     */

    public Unit deleteWaypointForce(final Waypoint waypoint) {
        val index: Int = getWaypointIndex(waypoint)
        waypoints.remove(index)
        DataStore.deleteWaypoint(waypoint.getId())
        DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE))
        resetFinalDefined()
    }

    /**
     * Find index of given {@code waypoint} in cache's {@code waypoints} list
     *
     * @param waypoint to find index for
     * @return index in {@code waypoints} if found, -1 otherwise
     */
    private Int getWaypointIndex(final Waypoint waypoint) {
        val id: Int = waypoint.getId()
        for (Int index = 0; index < waypoints.size(); index++) {
            if (waypoints.get(index).getId() == id) {
                return index
            }
        }
        return -1
    }

    /**
     * Lookup a waypoint by its id.
     *
     * @param id the id of the waypoint to look for
     * @return waypoint or {@code null}
     */
    public Waypoint getWaypointById(final Int id) {
        for (final Waypoint waypoint : waypoints) {
            if (waypoint.getId() == id) {
                return waypoint
            }
        }
        return null
    }

    /**
     * Lookup a waypoint by its prefix.
     *
     * @param prefix the prefix of the waypoint to look for
     * @return waypoint or {@code null}
     */
    public Waypoint getWaypointByPrefix(final String prefix) {
        for (final Waypoint waypoint : waypoints) {
            if (waypoint.getPrefix() == (prefix)) {
                return waypoint
            }
        }
        return null
    }

    /** Convenience method for {@link #addCacheArtefactsFromNotes(String)} with parameter null */
    public Boolean addCacheArtefactsFromNotes() {
        return addCacheArtefactsFromNotes(null)
    }

    /**
     * Detect cache artefacts (waypoints+variables) in the user's notes (cache + waypoints) and add them to user-defined waypoints/variables.
     * @param previousAllUserNotes if given, then a parse of previous notes is used to help in deciding which values potentially to overwrite
     */
    public Boolean addCacheArtefactsFromNotes(final String previousAllUserNotes) {
        return addCacheArtefactsFromText(getAllUserNotes(), false, CgeoApplication.getInstance().getString(R.string.cache_personal_note), false, previousAllUserNotes)
    }

    /** returns a concatenation of all user notes (Cache + waypoints) */
    public String getAllUserNotes() {
        val sb: StringBuilder = StringBuilder()
        for (Waypoint w : getWaypoints()) {
            if (!StringUtils.isBlank(w.getUserNote())) {
                sb.append(w.getUserNote()).append("\n")
            }
        }
        return sb.append(StringUtils.defaultString(getPersonalNote(), "")).toString()

    }


    /**
     * Detect cache artefacts (waypoints+variables) in the given text and add them to user-defined waypoints
     * or updates existing ones with meta information.
     *
     * @param text                 text which might contain coordinates
     * @param updateDb             if true the added waypoints are stored in DB right away
     * @param namePrefix           prefix for default waypoint names (if names cannot be extracted from text)
     * @param forceExtraction      if extraction should be enforced, regardless of cache setting
     * @param previousAllUserNotes if given, then a parse of previous notes is used to help in deciding which values potentially to overwrite
     */
    public Boolean addCacheArtefactsFromText(final String text, final Boolean updateDb, final String namePrefix, final Boolean forceExtraction, final String previousAllUserNotes) {
        Boolean changed = false
        if (forceExtraction || !preventWaypointsFromNote) {
            val previousParser: CacheArtefactParser = previousAllUserNotes == null ? null : CacheArtefactParser(this, namePrefix).parse(previousAllUserNotes)

            val cacheArtefactParser: CacheArtefactParser = CacheArtefactParser(this, namePrefix).parse(StringUtils.defaultString(text))
            for (final Waypoint parsedWaypoint : cacheArtefactParser.getWaypoints()) {
                changed |= addOrMergeInfoToExistingWaypoint(updateDb, namePrefix, parsedWaypoint)
            }
            val newVars: Map<String, String> = HashMap<>()
            for (Map.Entry<String, String> var : cacheArtefactParser.getVariables().entrySet()) {
                if (getVariables().isWorthAddingWithoutLoss(var.getKey(), var.getValue(), previousParser == null ? null : previousParser.getVariables().get(var.getKey()))) {
                    newVars.put(var.getKey(), var.getValue())
                }
            }
            changeVariables(newVars)
        }
        return changed
    }

    /** Returns a map of variables which differ from list and parsing user notes */
    public Map<String, Pair<String, String>> getVariableDifferencesFromUserNotes() {
        val noteVars: Map<String, String> = CacheArtefactParser(this, "").parse(getAllUserNotes()).getVariables()
        val cacheVars: Map<String, String> = getVariables().toMap()
        final Map<String, Pair<String, String>> result = CommonUtils.compare(noteVars, cacheVars)
        final Iterator<Map.Entry<String, Pair<String, String>>> it = result.entrySet().iterator()
        while (it.hasNext()) {
            final Map.Entry<String, Pair<String, String>> entry = it.next()
            if (StringUtils.isBlank(entry.getValue().first)) {
                it.remove()
            }
        }
        return result
    }

    /** changes this cache's variables with the given one's. Change is forced! */
    public Unit changeVariables(final Map<String, String> newVars) {
        for (Map.Entry<String, String> var : newVars.entrySet()) {
            getVariables().addVariable(var.getKey(), var.getValue())
            getVariables().saveState()
            recalculateWaypoints(); //need to do this because wps are not saved to DB yet
        }
    }

    public Boolean recalculateWaypoints() {
        return recalculateWaypoints(getVariables())
    }

    public Boolean recalculateWaypoints(final CacheVariableList variableList) {
        Boolean hasCalculatedWp = false
        for (Waypoint wp : getWaypoints()) {
            hasCalculatedWp |= wp.recalculateVariableDependentValues(variableList)
        }
        if (hasCalculatedWp) {
            resetFinalDefined()
            DataStore.saveWaypoints(this)
        }
        return hasCalculatedWp
    }

    private Boolean addOrMergeInfoToExistingWaypoint(final Boolean updateDb, final String namePrefix, final Waypoint wpCandidate) {
        Boolean changed = false
        val existingWaypoint: Waypoint = findWaypoint(wpCandidate)
        if (null == existingWaypoint) {
            //add as waypoint
            addOrChangeWaypoint(wpCandidate, updateDb)
            changed = true
        } else {
            //if parsed waypoint contains more up-to-date-information -> overwrite it
            if (existingWaypoint.mergeFromParsedText(wpCandidate, namePrefix)) {
                addOrChangeWaypoint(existingWaypoint, updateDb)
                changed = true
            }
        }
        return changed
    }

    public Int addCalculatedWaypoints(final Collection<Pair<String, String>> foundPatterns, final String namePrefix) {
        Int added = 0
        for (final Pair<String, String> pattern : foundPatterns) {
            val wpCandidate: Waypoint = Waypoint(namePrefix + " " + (added + 1), WaypointType.WAYPOINT, true)
            val cc: CalculatedCoordinate = CalculatedCoordinate()
            cc.setLatitudePattern(pattern.first)
            cc.setLongitudePattern(pattern.second)
            wpCandidate.setCalculated(cc, f -> variables.getValue(f))
            val changed: Boolean = addOrMergeInfoToExistingWaypoint(true, namePrefix, wpCandidate)
            if (changed) {
                added++
            }
        }
        return added
    }

    private Waypoint findWaypoint(final Waypoint searchWp) {
        //try to match prefix
        val searchWpPrefix: String = searchWp.getPrefix()
        if (StringUtils.isNotBlank(searchWpPrefix)) {
            for (final Waypoint waypoint : waypoints) {
                if (searchWpPrefix == (waypoint.getPrefix())) {
                    return waypoint
                }
            }
            return null
        }

        //try to match coordinate
        val searchWpPoint: Geopoint = searchWp.getCoords()
        if (null != searchWpPoint) {
            for (final Waypoint waypoint : waypoints) {
                // waypoint can have no coords such as a Final set by cache owner
                val coords: Geopoint = waypoint.getCoords()
                if (coords != null && coords.equalsDecMinute(searchWpPoint)) {
                    return waypoint
                }
            }
        }

        //try to match calculate coordinate
        if (searchWp.isCalculated()) {
            for (final Waypoint waypoint : waypoints) {
                // calculated waypoints match if they have same config
                if (waypoint.isCalculated() && Objects == (waypoint.getCalcStateConfig(), searchWp.getCalcStateConfig())) {
                    return waypoint
                }
            }
        }

        //try to match name if prefix is empty and coords are not equal.
        //But only, if coordinates of waypoint can be updated, otherwise create a waypoint
        //(it's not a bug, it's a feature - otherwise the coordinates gets lost)
        val searchWpName: String = searchWp.getName()
        val searchWpType: String = searchWp.getWaypointType().getL10n()
        if (StringUtils.isNotBlank(searchWpName)) {
            for (final Waypoint waypoint : waypoints) {
                val point: Geopoint = waypoint.getCoords()
                val canChangeCoordinates: Boolean = null == searchWpPoint || null == point
                if (canChangeCoordinates && searchWpName == (waypoint.getName()) && searchWpType == (waypoint.getWaypointType().getL10n())) {
                    return waypoint
                }
            }
        }
        return null
    }

    /*
     * For working in the debugger
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    override     public String toString() {
        return this.geocode + " " + this.name
    }

    override     public Int hashCode() {
        return StringUtils.defaultString(geocode).hashCode()
    }

    override     public Boolean equals(final Object obj) {
        // just compare the geocode even if that is not what "equals" normally does.
        // Reason: geocaches should be treated as equal even if two geocache objects have different amount of data cause they come from different sources.
        return this == obj || (obj is Geocache && StringUtils.isNotEmpty(geocode) && geocode == (((Geocache) obj).geocode))
    }

    public Unit store() {
        if (lists.isEmpty()) {
            lists.add(StoredList.STANDARD_LIST_ID)
        }
        storeCache(this, null, lists, false, null)
    }

    public Unit store(final Set<Integer> listIds, final DisposableHandler handler) {
        lists.clear()
        lists.addAll(listIds)
        storeCache(this, null, lists, false, handler)
        notifyChange()
    }

    override     public CoordinateType getCoordType() {
        return CoordinateType.CACHE
    }

    public Disposable drop(final Handler handler) {
        return Schedulers.io().scheduleDirect(() -> {
            try {
                dropSynchronous()
                handler.sendMessage(Message.obtain())
            } catch (final Exception e) {
                Log.e("cache.drop: ", e)
            }
        })
    }

    public Unit dropSynchronous() {
        DataStore.markDropped(Collections.singletonList(this))
        DataStore.removeCache(getGeocode(), EnumSet.of(RemoveFlag.CACHE))
    }

    private Unit warnIncorrectParsingIf(final Boolean incorrect, final String field) {
        if (incorrect) {
            Log.w(field + " not parsed correctly for " + geocode)
        }
    }

    private Unit warnIncorrectParsingIfBlank(final String str, final String field) {
        warnIncorrectParsingIf(StringUtils.isBlank(str), field)
    }

    public Unit checkFields() {
        warnIncorrectParsingIfBlank(getGeocode(), "geo")
        warnIncorrectParsingIfBlank(getName(), "name")
        warnIncorrectParsingIfBlank(getGuid(), "guid")
        warnIncorrectParsingIf(getTerrain() == 0.0, "terrain")
        warnIncorrectParsingIf(getDifficulty() == 0.0, "difficulty")
        warnIncorrectParsingIfBlank(getOwnerDisplayName(), "owner")
        warnIncorrectParsingIfBlank(getOwnerUserId(), "owner")
        warnIncorrectParsingIf(getHiddenDate() == null, "hidden")
        warnIncorrectParsingIf(getFavoritePoints() < 0, "favoriteCount")
        warnIncorrectParsingIf(getSize() == CacheSize.UNKNOWN, "size")
        warnIncorrectParsingIf(getType() == null || getType() == CacheType.UNKNOWN, "type")
        warnIncorrectParsingIf(getCoords() == null, "coordinates")
        warnIncorrectParsingIfBlank(getLocation(), "location")
    }

    public Disposable refresh(final DisposableHandler handler, final Scheduler scheduler) {
        return scheduler.scheduleDirect(() -> refreshSynchronous(handler))
    }

    public Unit refreshSynchronous(final DisposableHandler handler) {
        refreshSynchronous(handler, lists)
    }

    public Unit refreshSynchronous(final DisposableHandler handler, final Set<Integer> additionalListIds) {
        val combinedListIds: Set<Integer> = HashSet<>(lists)
        combinedListIds.addAll(additionalListIds)
        storeCache(null, geocode, combinedListIds, true, handler)
    }

    /**
     * Download and store a cache synchronous
     *
     * @param origCache       the cache which should be refreshed, can be null
     * @param geocode         the geocode of the cache which should be downloaded
     * @param lists           to which lists the cache should be added
     * @param forceRedownload whether the cache should be re-downloaded, even if it's already stored offline
     * @param handler         a handler to receive status updates, can be null
     * @return true, if the cache was stored successfully
     */
    @WorkerThread
    @SuppressWarnings("PMD.NPathComplexity")
    public static Boolean storeCache(final Geocache origCache, final String geocode, final Set<Integer> lists, final Boolean forceRedownload, final DisposableHandler handler) {
        try {
            final Geocache cache
            // get cache details, they may not yet be complete
            if (origCache != null) {
                // only reload the cache if it was already stored or doesn't have full details (by checking the description)
                if (origCache.isOffline() || StringUtils.isBlank(origCache.getDescription())) {
                    val search: SearchResult = searchByGeocode(origCache.getGeocode(), null, false, handler)
                    cache = search != null ? search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB) : origCache
                    if (search != null && search.getError() != StatusCode.NO_ERROR) {
                        DisposableHandler.sendShowStatusToast(handler, search.getError().getErrorStringId())
                    }
                } else {
                    cache = origCache
                }
            } else if (StringUtils.isNotBlank(geocode)) {
                val search: SearchResult = searchByGeocode(geocode, null, forceRedownload, handler)
                cache = search != null ? search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB) : null
                if (search != null && search.getError() != StatusCode.NO_ERROR) {
                    DisposableHandler.sendShowStatusToast(handler, search.getError().getErrorStringId())
                }
            } else {
                cache = null
            }

            if (cache == null) {
                if (handler != null) {
                    handler.sendMessage(Message.obtain())
                }

                return false
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false
            }

            val imgGetter: HtmlImage = HtmlImage(cache.getGeocode(), false, true, forceRedownload)

            // store images from description
            if (StringUtils.isNotBlank(cache.getDescription())) {
                HtmlCompat.fromHtml(cache.getDescription(), HtmlCompat.FROM_HTML_MODE_LEGACY, imgGetter, null)
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false
            }

            // store spoilers
            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                for (final Image oneSpoiler : cache.getSpoilers()) {
                    imgGetter.getDrawable(oneSpoiler.getUrl())
                }
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false
            }

            // store images from logs
            if (Settings.isStoreLogImages()) {
                for (final LogEntry log : cache.getLogs()) {
                    if (log.hasLogImages()) {
                        for (final Image oneLogImg : log.logImages) {
                            imgGetter.getDrawable(oneLogImg.getUrl())
                        }
                    }
                }
            }

            if (DisposableHandler.isDisposed(handler)) {
                return false
            }

            // Need to wait for images loading since HtmlImage.getDrawable is non-blocking here
            imgGetter.waitForEndCompletable(null).blockingAwait()

            cache.setLists(lists)

            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB))

            if (DisposableHandler.isDisposed(handler)) {
                return false
            }

            if (handler != null) {
                handler.sendEmptyMessage(DisposableHandler.DONE)
            }
            return true
        } catch (final Exception e) {
            Log.e("Geocache.storeCache", e)
            return false
        }
    }

    @WorkerThread
    public static SearchResult searchByGeocode(final String geocode, final String guid, final Boolean forceReload, final DisposableHandler handler) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            Log.e("Geocache.searchByGeocode: No geocode nor guid given")
            return null
        }

        if (!forceReload && (DataStore.isOffline(geocode, guid) || DataStore.isThere(geocode, guid, true))) {
            val search: SearchResult = SearchResult()
            val realGeocode: String = StringUtils.isNotBlank(geocode) ? geocode : DataStore.getGeocodeForGuid(guid)
            search.addGeocode(realGeocode)
            return search
        }


        SearchResult result = null
        // if we have no geocode, we can't dynamically select the handler, but must explicitly use GC
        if (geocode == null) {
            result = GCConnector.getInstance().searchByGeocode(null, guid, handler)
        } else {
            val connector: IConnector = ConnectorFactory.getConnector(geocode)
            if (connector is ISearchByGeocode) {
                result = ((ISearchByGeocode) connector).searchByGeocode(geocode, guid, handler)
            }
        }
        AmendmentUtils.amendCaches(result)
        return result
    }

    public Boolean isOffline() {
        return !lists.isEmpty() && (lists.size() > 1 || !lists.contains(StoredList.TEMPORARY_LIST.id))
    }

    public Int getEventStartTimeInMinutes() {
        if (eventTimesInMin.start == null) {
            guessEventTimeMinutes()
        }
        return eventTimesInMin.start
    }

    public Int getEventEndTimeInMinutes() {
        return eventTimesInMin.end == null ? -1 : eventTimesInMin.end
    }

    /**
     * guess an event start time from the description
     */
    private Unit guessEventTimeMinutes() {
        if (!isEventCache()) {
            eventTimesInMin.start = -1
            return
        }

        // GC Listings have the start and end time in Short description, try that first
        final Int[] gcDates = EventTimeParser.getEventTimesFromGcShortDesc(getShortDescription())
        if (gcDates[0] >= 0 && gcDates[1] >= 0) {
            eventTimesInMin.start = gcDates[0]
            eventTimesInMin.end = gcDates[1]
        } else {
            // if not successful scan the whole description for what looks like a start time
            val searchText: String = getShortDescription() + ' ' + getDescription()
            eventTimesInMin.start = EventTimeParser.guessEventTimeMinutes(searchText)
        }
    }

    public Collection<Image> getImages() {
        val images: List<Image> = LinkedList<>()
        //the order of adding imgaes will determine which duplicates will be removed. prio is to images further up
        for (final LogEntry log : getLogs()) {
            images.addAll(log.logImages)
        }
        images.addAll(ImageUtils.getImagesFromText(
                (url, imgBuilder) -> imgBuilder.setCategory(Image.ImageCategory.NOTE),
                getPersonalNote()))
        images.addAll(ImageUtils.getImagesFromHtml(
                (url, imgBuilder) -> imgBuilder.setCategory(Image.ImageCategory.LISTING),
                getShortDescription(), getDescription()))
        images.addAll(getSpoilers()); //for gc.com this includes gallery images, spoilers and background
        addLocalSpoilersTo(images)

        // Deduplicate images and return them in requested size
        ImageUtils.deduplicateImageList(images)

        return images
    }

    public List<Image> getNonStaticImages() {
        val result: ArrayList<Image> = ArrayList<>()
        for (final Image image : getImages()) {
            // search strings fit geocaching.com and opencaching, may need to add others
            // Xiaomi does not support java.lang.CharSequence#containsAny(java.lang.CharSequence[]),
            // which is called by StringUtils.containsAny(CharSequence, CharSequence...).
            // Thus, we have to use StringUtils.contains(...) instead (see issue #5766).
            val url: String = image.getUrl()
            if (!StringUtils.contains(url, "/static") &&
                    !StringUtils.contains(url, "/resource") &&
                    !StringUtils.contains(url, "/icons/")) {
                result.add(image)
            }
        }
        return result
    }

    /**
     * Add spoilers stored locally in <tt>/sdcard/cgeo/GeocachePhotos</tt>. If a cache is named GC123ABC, the
     * directory will be <tt>/sdcard/cgeo/GeocachePhotos/C/B/GC123ABC/</tt>.
     *
     * @param spoilers the list to add to
     */
    private Unit addLocalSpoilersTo(final List<Image> spoilers) {
        if (StringUtils.length(geocode) >= 2) {
            val suffix: String = StringUtils.right(geocode, 2)
            val spoilerFolder: Folder = Folder.fromFolder(PersistableFolder.SPOILER_IMAGES.getFolder(),
                    suffix.substring(1) + "/" + suffix.charAt(0) + "/" + geocode)
            for (ContentStorage.FileInformation imageFile : ContentStorage.get().list(spoilerFolder)) {
                if (imageFile.isDirectory) {
                    continue
                }
                spoilers.add(Image.Builder().setUrl(imageFile.uri).setTitle(imageFile.name).setCategory(Image.ImageCategory.OWN).build())
            }
        }
    }

    public Unit setDetailedUpdatedNow() {
        val now: Long = System.currentTimeMillis()
        setUpdated(now)
        setDetailedUpdate(now)
        setDetailed(true)
    }

    /**
     * Gets whether the user has logged the specific log type for this cache. Only checks the currently stored logs of
     * the cache, so the result might be wrong.
     */
    public Boolean hasOwnLog(final LogType logType) {
        for (final LogEntry logEntry : getLogs()) {
            if (logEntry.logType == logType && logEntry.isOwn()) {
                return true
            }
        }
        return false
    }

    @DrawableRes
    public Int getMapMarkerId() {
        return getConnector().getCacheMapMarkerId()
    }

    @DrawableRes
    public Int getMapMarkerBackgroundId() {
        return getConnector().getCacheMapMarkerBackgroundId()
    }

    @DrawableRes
    public Int getMapDotMarkerId() {
        return getConnector().getCacheMapDotMarkerId()
    }

    @DrawableRes
    public Int getMapDotMarkerBackgroundId() {
        return getConnector().getCacheMapDotMarkerBackgroundId()
    }

    public Boolean isLogPasswordRequired() {
        return logPasswordRequired
    }

    public Unit setLogPasswordRequired(final Boolean required) {
        logPasswordRequired = required
    }

    public Boolean isPreventWaypointsFromNote() {
        return preventWaypointsFromNote
    }

    public Unit setPreventWaypointsFromNote(final Boolean preventWaypointsFromNote) {
        this.preventWaypointsFromNote = preventWaypointsFromNote
    }

    public String getWaypointGpxId(final String prefix) {
        return getConnector().getWaypointGpxId(prefix, geocode)
    }

    public String getFullWaypointGpxId(final String prefix) {
        return getConnector().getFullWaypointGpxId(prefix, geocode)
    }

    public String getWaypointPrefix(final String name) {
        return getConnector().getWaypointPrefix(name)
    }

    /**
     * Get number of overall finds for a cache, or 0 if the number of finds is not known.
     * TODO: 0 should be a valid value, maybe need to return -1 if the number is not known
     */
    public Int getFindsCount() {
        Int sumFound = 0
        for (final Entry<LogType, Integer> logCount : getLogCounts().entrySet()) {
            if (logCount.getKey().isFoundLog()) {
                val logged: Integer = logCount.getValue()
                if (logged != null) {
                    sumFound += logged
                }
            }
        }
        return sumFound
    }

    public Boolean applyDistanceRule() {
        return mayApplyDistanceRule()
                && (getType().applyDistanceRule() || hasUserModifiedCoords())
    }

    public Boolean mayApplyDistanceRule() {
        return !isArchived()
                && (getConnector() == GCConnector.getInstance() || getConnector() == InternalConnector.getInstance())
    }

    public LogType getDefaultLogType() {
        if (isEventCache()) {
            val eventDate: Date = getHiddenDate()
            val expired: Boolean = CalendarUtils.isPastEvent(this)

            if (hasOwnLog(LogType.WILL_ATTEND) || expired || (eventDate != null && CalendarUtils.daysSince(eventDate.getTime()) == 0)) {
                return hasOwnLog(LogType.ATTENDED) ? LogType.NOTE : LogType.ATTENDED
            }
            return LogType.WILL_ATTEND
        }
        if (isFound()) {
            return LogType.NOTE
        }
        if (getType() == CacheType.WEBCAM) {
            return LogType.WEBCAM_PHOTO_TAKEN
        }
        return LogType.FOUND_IT
    }

    /**
     * Get the geocodes of a collection of caches.
     *
     * @param caches a collection of caches
     * @return the non-blank geocodes of the caches
     */
    public static Set<String> getGeocodes(final Collection<Geocache> caches) {
        return getGeocodes(caches, HashSet<>(caches.size()))
    }

    public static <T : Collection()<String>> T getGeocodes(final Iterable<Geocache> caches, final T result) {
        for (final Geocache cache : caches) {
            val geocode: String = cache.getGeocode()
            if (StringUtils.isNotBlank(geocode)) {
                result.add(geocode)
            }
        }
        return result
    }

    /**
     * Show the hint as toast message. If no hint is available, a default "no hint available" will be shown instead.
     */
    public Unit showHintToast(final Activity activity) {
        val hint: String = getHint()
        ActivityMixin.showToast(activity, StringUtils.defaultIfBlank(hint, activity.getString(R.string.cache_hint_not_available)))
    }

    public GeoitemRef getGeoitemRef() {
        return GeoitemRef(getGeocode(), getCoordType(), getGeocode(), 0, getName(), getType().iconId)
    }

    public static String getAlternativeListingText(final String alternativeCode) {
        return CgeoApplication.getInstance().getResources()
                .getString(R.string.cache_listed_on, GCConnector.getInstance().getName()) +
                ": <a href=\"https://coord.info/" +
                alternativeCode +
                "\">" +
                alternativeCode +
                "</a><br /><br />"
    }

    public Boolean isGotoHistoryUDC() {
        return geocode == (InternalConnector.GEOCODE_HISTORY_CACHE)
    }

    public Comparator<? super Waypoint> getWaypointComparator() {
        return isGotoHistoryUDC() ? Waypoint.WAYPOINT_ID_COMPARATOR : Waypoint.WAYPOINT_COMPARATOR
    }

    public Int getAssignedEmoji() {
        return assignedEmoji
    }

    public CacheVariableList getVariables() {
        if (variables == null) {
            variables = CacheVariableList(this.geocode)
        }
        return variables
    }

    /**
     * used for online search metainfos (e.g. finder)
     */
    public SearchCacheData getSearchData() {
        return searchCacheData
    }

    /**
     * used for online search metainfos (e.g. finder)
     */
    public Unit setSearchData(final SearchCacheData searchCacheData) {
        this.searchCacheData = searchCacheData
    }

    private static class EventTimesInMin {
        var start: Integer = null
        var end: Integer = null

        public Unit reset() {
            this.start = null
            this.end = null
        }
    }
}
