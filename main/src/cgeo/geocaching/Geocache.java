package cgeo.geocaching;

import cgeo.geocaching.DataStore.StorageLocation;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.SimpleWebviewActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ILoggingManager;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.connector.gc.UncertainProperty;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.GPXParser;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.DateUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.LazyInitializedList;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.RxUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Html;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Internal representation of a "cache"
 */
public class Geocache implements IWaypoint {

    private static final int OWN_WP_PREFIX_OFFSET = 17;
    private long updated = 0;
    private long detailedUpdate = 0;
    private long visitedDate = 0;
    private int listId = StoredList.TEMPORARY_LIST.id;
    private boolean detailed = false;
    private String geocode = "";
    private String cacheId = "";
    private String guid = "";
    private UncertainProperty<CacheType> cacheType = new UncertainProperty<>(CacheType.UNKNOWN, Tile.ZOOMLEVEL_MIN - 1);
    private String name = "";
    private String ownerDisplayName = "";
    private String ownerUserId = "";
    private Date hidden = null;
    /**
     * lazy initialized
     */
    private String hint = null;
    private CacheSize size = CacheSize.UNKNOWN;
    private float difficulty = 0;
    private float terrain = 0;
    private Float direction = null;
    private Float distance = null;
    /**
     * lazy initialized
     */
    private String location = null;
    private UncertainProperty<Geopoint> coords = new UncertainProperty<>(null);
    private boolean reliableLatLon = false;
    private String personalNote = null;
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
    private Boolean favorite = null;
    private Boolean onWatchlist = null;
    private Boolean logOffline = null;
    private int favoritePoints = 0;
    private float rating = 0; // valid ratings are larger than zero
    private int votes = 0;
    private float myVote = 0; // valid ratings are larger than zero
    private int inventoryItems = 0;
    private final LazyInitializedList<String> attributes = new LazyInitializedList<String>() {
        @Override
        public List<String> call() {
            return DataStore.loadAttributes(geocode);
        }
    };
    private final LazyInitializedList<Waypoint> waypoints = new LazyInitializedList<Waypoint>() {
        @Override
        public List<Waypoint> call() {
            return DataStore.loadWaypoints(geocode);
        }
    };
    private List<Image> spoilers = null;

    private List<Trackable> inventory = null;
    private Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
    private boolean userModifiedCoords = false;
    // temporary values
    private boolean statusChecked = false;
    private String directionImg = "";
    private String nameForSorting;
    private final EnumSet<StorageLocation> storageLocation = EnumSet.of(StorageLocation.HEAP);
    private boolean finalDefined = false;
    private boolean logPasswordRequired = false;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private Handler changeNotificationHandler = null;

    /**
     * Create a new cache. To be used everywhere except for the GPX parser
     */
    public Geocache() {
        // empty
    }

    /**
     * Cache constructor to be used by the GPX parser only. This constructor explicitly sets several members to empty
     * lists.
     *
     * @param gpxParser ignored parameter allowing to select this constructor
     */
    public Geocache(final GPXParser gpxParser) {
        setReliableLatLon(true);
        setAttributes(Collections.<String> emptyList());
        setWaypoints(Collections.<Waypoint> emptyList(), false);
    }

    public void setChangeNotificationHandler(final Handler newNotificationHandler) {
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
     * @param other
     *            the other version, or null if non-existent
     * @return true if this cache is "equal" to the other version
     */
    public boolean gatherMissingFrom(final Geocache other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }

        updated = System.currentTimeMillis();
        // if parsed cache is not yet detailed and stored is, the information of
        // the parsed cache will be overwritten
        if (!detailed && other.detailed) {
            detailed = true;
            detailedUpdate = other.detailedUpdate;
            // boolean values must be enumerated here. Other types are assigned outside this if-statement
            reliableLatLon = other.reliableLatLon;
            finalDefined = other.finalDefined;
        }

        if (premiumMembersOnly == null) {
            premiumMembersOnly = other.premiumMembersOnly;
        }
        if (found == null) {
            found = other.found;
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
        if (logOffline == null) {
            logOffline = other.logOffline;
        }
        if (visitedDate == 0) {
            visitedDate = other.visitedDate;
        }
        if (listId == StoredList.TEMPORARY_LIST.id) {
            listId = other.listId;
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
        cacheType = UncertainProperty.getMergedProperty(cacheType, other.cacheType);
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
        if (!detailed && StringUtils.isBlank(getHint())) {
            hint = other.getHint();
        }
        if (size == null || CacheSize.UNKNOWN == size) {
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
        coords = UncertainProperty.getMergedProperty(coords, other.coords);
        // don't use StringUtils.isBlank here. Otherwise we cannot recognize a note which was deleted on GC
        if (personalNote == null) {
            personalNote = other.personalNote;
        } else if (other.personalNote != null && !personalNote.equals(other.personalNote)) {
            final PersonalNote myNote = new PersonalNote(this);
            final PersonalNote otherNote = new PersonalNote(other);
            final PersonalNote mergedNote = myNote.mergeWith(otherNote);
            personalNote = mergedNote.toString();
        }
        if (!detailed && StringUtils.isBlank(getShortDescription())) {
            shortdesc = other.getShortDescription();
        }
        if (StringUtils.isBlank(getDescription())) {
            description = other.getDescription();
        }
        // FIXME: this makes no sense to favor this over the other. 0 should not be a special case here as it is
        // in the range of acceptable values. This is probably the case at other places (rating, votes, etc.) too.
        if (favoritePoints == 0) {
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
        if (!detailed && attributes.isEmpty()) {
            if (other.attributes != null) {
                attributes.addAll(other.attributes);
            }
        }
        if (waypoints.isEmpty()) {
            this.setWaypoints(other.waypoints, false);
        }
        else {
            final ArrayList<Waypoint> newPoints = new ArrayList<>(waypoints);
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
            inventory = other.inventory;
            inventoryItems = other.inventoryItems;
        }
        if (logCounts.isEmpty()) {
            logCounts = other.logCounts;
        }

        // if cache has ORIGINAL type waypoint ... it is considered that it has modified coordinates, otherwise not
        userModifiedCoords = false;
        for (final Waypoint wpt : waypoints) {
            if (wpt.getWaypointType() == WaypointType.ORIGINAL) {
                userModifiedCoords = true;
                break;
            }
        }

        if (!reliableLatLon) {
            reliableLatLon = other.reliableLatLon;
        }

        return isEqualTo(other);
    }

    /**
     * Compare two caches quickly. For map and list fields only the references are compared !
     *
     * @param other
     *            the other cache to compare this one to
     * @return true if both caches have the same content
     */
    @SuppressWarnings("deprecation")
    @SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
    private boolean isEqualTo(final Geocache other) {
        return detailed == other.detailed &&
                StringUtils.equalsIgnoreCase(geocode, other.geocode) &&
                StringUtils.equalsIgnoreCase(name, other.name) &&
                cacheType == other.cacheType &&
                size == other.size &&
                ObjectUtils.equals(found, other.found) &&
                ObjectUtils.equals(premiumMembersOnly, other.premiumMembersOnly) &&
                difficulty == other.difficulty &&
                terrain == other.terrain &&
                (coords != null ? coords.equals(other.coords) : null == other.coords) &&
                reliableLatLon == other.reliableLatLon &&
                ObjectUtils.equals(disabled, other.disabled) &&
                ObjectUtils.equals(archived, other.archived) &&
                listId == other.listId &&
                StringUtils.equalsIgnoreCase(ownerDisplayName, other.ownerDisplayName) &&
                StringUtils.equalsIgnoreCase(ownerUserId, other.ownerUserId) &&
                StringUtils.equalsIgnoreCase(getDescription(), other.getDescription()) &&
                StringUtils.equalsIgnoreCase(personalNote, other.personalNote) &&
                StringUtils.equalsIgnoreCase(getShortDescription(), other.getShortDescription()) &&
                StringUtils.equalsIgnoreCase(getLocation(), other.getLocation()) &&
                ObjectUtils.equals(favorite, other.favorite) &&
                favoritePoints == other.favoritePoints &&
                ObjectUtils.equals(onWatchlist, other.onWatchlist) &&
                (hidden != null ? hidden.equals(other.hidden) : null == other.hidden) &&
                StringUtils.equalsIgnoreCase(guid, other.guid) &&
                StringUtils.equalsIgnoreCase(getHint(), other.getHint()) &&
                StringUtils.equalsIgnoreCase(cacheId, other.cacheId) &&
                (direction != null ? direction.equals(other.direction) : null == other.direction) &&
                (distance != null ? distance.equals(other.distance) : null == other.distance) &&
                rating == other.rating &&
                votes == other.votes &&
                myVote == other.myVote &&
                inventoryItems == other.inventoryItems &&
                attributes == other.attributes &&
                waypoints == other.waypoints &&
                spoilers == other.spoilers &&
                inventory == other.inventory &&
                logCounts == other.logCounts &&
                ObjectUtils.equals(logOffline, other.logOffline) &&
                finalDefined == other.finalDefined;
    }

    public boolean hasTrackables() {
        return inventoryItems > 0;
    }

    public boolean canBeAddedToCalendar() {
        // is event type?
        if (!isEventCache()) {
            return false;
        }
        // has event date set?
        if (hidden == null) {
            return false;
        }
        // is not in the past?
        final Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return hidden.compareTo(cal.getTime()) >= 0;
    }

    public boolean isEventCache() {
        return cacheType.getValue().isEvent();
    }

    public void logVisit(final Activity fromActivity) {
        if (!getConnector().canLog(this)) {
            ActivityMixin.showToast(fromActivity, fromActivity.getResources().getString(R.string.err_cannot_log_visit));
            return;
        }
        fromActivity.startActivity(LogCacheActivity.getLogCacheIntent(fromActivity, cacheId, geocode));
    }

    public void logOffline(final Activity fromActivity, final LogType logType) {
        final boolean mustIncludeSignature = StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature();
        final String initial = mustIncludeSignature ? LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(this, null, true)) : "";
        logOffline(fromActivity, initial, Calendar.getInstance(), logType);
    }

    void logOffline(final Activity fromActivity, final String log, final Calendar date, final LogType logType) {
        if (logType == LogType.UNKNOWN) {
            return;
        }
        final boolean status = DataStore.saveLogOffline(geocode, date.getTime(), logType, log);

        final Resources res = fromActivity.getResources();
        if (status) {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.info_log_saved));
            DataStore.saveVisitDate(geocode);
            logOffline = Boolean.TRUE;

            notifyChange();
        } else {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.err_log_post_failed));
        }
    }

    public void clearOfflineLog() {
        DataStore.clearLogOffline(geocode);
        notifyChange();
    }

    public List<LogType> getPossibleLogTypes() {
        return getConnector().getPossibleLogTypes(this);
    }

    public void openInBrowser(final Activity fromActivity) {
        final Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getLongUrl()));

        // Check if cgeo is the default, show the chooser to let the user choose a browser
        if (viewIntent.resolveActivity(fromActivity.getPackageManager()).getPackageName().equals(fromActivity.getPackageName())) {
            final Intent chooser = Intent.createChooser(viewIntent, fromActivity.getString(R.string.cache_menu_browser));

            final Intent internalBrowser = new Intent(fromActivity, SimpleWebviewActivity.class);
            internalBrowser.setData(Uri.parse(getUrl()));

            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[] {internalBrowser});


            fromActivity.startActivity(chooser);
        } else {
            fromActivity.startActivity(viewIntent);
        }
    }


    private String getCacheUrl() {
        return getConnector().getCacheUrl(this);
    }

    private IConnector getConnector() {
        return ConnectorFactory.getConnector(this);
    }

    public boolean canOpenInBrowser() {
        return getCacheUrl() != null;
    }

    public boolean supportsRefresh() {
        return getConnector() instanceof ISearchByGeocode;
    }

    public boolean supportsWatchList() {
        return getConnector().supportsWatchList();
    }

    public boolean supportsFavoritePoints() {
        return getConnector().supportsFavoritePoints(this);
    }

    public boolean supportsLogging() {
        return getConnector().supportsLogging();
    }

    public boolean supportsLogImages() {
        return getConnector().supportsLogImages();
    }

    public boolean supportsOwnCoordinates() {
        return getConnector().supportsOwnCoordinates();
    }

    public ILoggingManager getLoggingManager(final LogCacheActivity activity) {
        return getConnector().getLoggingManager(activity, this);
    }

    public float getDifficulty() {
        return difficulty;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    /**
     * @return displayed owner, might differ from the real owner
     */
    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public CacheSize getSize() {
        if (size == null) {
            return CacheSize.UNKNOWN;
        }
        return size;
    }

    public float getTerrain() {
        return terrain;
    }

    public boolean isArchived() {
        return BooleanUtils.isTrue(archived);
    }

    public boolean isDisabled() {
        return BooleanUtils.isTrue(disabled);
    }

    public boolean isPremiumMembersOnly() {
        return BooleanUtils.isTrue(premiumMembersOnly);
    }

    public void setPremiumMembersOnly(final boolean members) {
        this.premiumMembersOnly = members;
    }

    /**
     *
     * @return {@code true} if the user is the owner of the cache, {@code false} otherwise
     */
    public boolean isOwner() {
        return getConnector().isOwner(this);
    }

    /**
     * @return GC username of the (actual) owner, might differ from the owner. Never empty.
     */
    @NonNull public String getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     *
     * @return the decrypted hint
     */
    public String getHint() {
        initializeCacheTexts();
        assertTextNotNull(hint, "Hint");
        return hint;
    }

    /**
     * After lazy loading the lazily loaded field must be non {@code null}.
     *
     */
    private static void assertTextNotNull(final String field, final String name) throws InternalError {
        if (field == null) {
            throw new InternalError(name + " field is not allowed to be null here");
        }
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
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
        }
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
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
        if (StringUtils.isBlank(cacheId) && getConnector().equals(GCConnector.getInstance())) {
            return String.valueOf(GCConstants.gccodeToGCId(geocode));
        }

        return cacheId;
    }

    public String getGuid() {
        return guid;
    }

    /**
     * Attention, calling this method may trigger a database access for the cache!
     */
    public String getLocation() {
        initializeCacheTexts();
        assertTextNotNull(location, "Location");
        return location;
    }

    public String getPersonalNote() {
        // non premium members have no personal notes, premium members have an empty string by default.
        // map both to null, so other code doesn't need to differentiate
        return StringUtils.defaultIfBlank(personalNote, null);
    }

    public boolean supportsCachesAround() {
        return getConnector() instanceof ISearchByCenter;
    }

    public void shareCache(final Activity fromActivity, final Resources res) {
        if (geocode == null) {
            return;
        }

        final Intent intent = getShareIntent();

        fromActivity.startActivity(Intent.createChooser(intent, res.getText(R.string.cache_menu_share)));
    }
    public Intent getShareIntent() {
        final StringBuilder subject = new StringBuilder("Geocache ");
        subject.append(geocode);
        if (StringUtils.isNotBlank(name)) {
            subject.append(" - ").append(name);
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
        intent.putExtra(Intent.EXTRA_TEXT, getUrl());

        return intent;
    }

    public String getUrl() {
        return getConnector().getCacheUrl(this);
    }

    public String getLongUrl() {
        return getConnector().getLongCacheUrl(this);
    }

    public String getCgeoUrl() { return getConnector().getCacheUrl(this); }

    public boolean supportsGCVote() {
        return StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isFound() {
        return BooleanUtils.isTrue(found);
    }

    /**
     *
     * @return {@code true} if the user has put a favorite point onto this cache
     */
    public boolean isFavorite() {
        return BooleanUtils.isTrue(favorite);
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    @Nullable
    public Date getHiddenDate() {
        return hidden;
    }

    public List<String> getAttributes() {
        return attributes.getUnderlyingList();
    }

    public List<Trackable> getInventory() {
        return inventory;
    }

    public void addSpoiler(final Image spoiler) {
        if (spoilers == null) {
            spoilers = new ArrayList<>();
        }
        spoilers.add(spoiler);
    }

    public List<Image> getSpoilers() {
        return ListUtils.unmodifiableList(ListUtils.emptyIfNull(spoilers));
    }

    /**
     * @return a statistic how often the caches has been found, disabled, archived etc.
     */
    public Map<LogType, Integer> getLogCounts() {
        return logCounts;
    }

    public int getFavoritePoints() {
        return favoritePoints;
    }

    /**
     * @return the normalized cached name to be used for sorting, taking into account the numerical parts in the name
     */
    public String getNameForSorting() {
        if (null == nameForSorting) {
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
        return cacheType.getValue().isVirtual();
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

    public int getListId() {
        return listId;
    }

    public void setListId(final int listId) {
        this.listId = listId;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(final boolean detailed) {
        this.detailed = detailed;
    }

    public void setHidden(final Date hidden) {
        if (hidden == null) {
            this.hidden = null;
        }
        else {
            this.hidden = new Date(hidden.getTime()); // avoid storing the external reference in this object
        }
    }

    public Float getDirection() {
        return direction;
    }

    public void setDirection(final Float direction) {
        this.direction = direction;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(final Float distance) {
        this.distance = distance;
    }

    @Override
    public Geopoint getCoords() {
        return coords.getValue();
    }

    public int getCoordZoomLevel() {
        return coords.getCertaintyLevel();
    }

    /**
     * Set reliable coordinates
     */
    public void setCoords(final Geopoint coords) {
        this.coords = new UncertainProperty<>(coords);
    }

    /**
     * Set unreliable coordinates from a certain map zoom level
     */
    public void setCoords(final Geopoint coords, final int zoomlevel) {
        this.coords = new UncertainProperty<>(coords, zoomlevel);
    }

    /**
     * @return true if the coords are from the cache details page and the user has been logged in
     */
    public boolean isReliableLatLon() {
        return getConnector().isReliableLatLon(reliableLatLon);
    }

    public void setReliableLatLon(final boolean reliableLatLon) {
        this.reliableLatLon = reliableLatLon;
    }

    public void setShortDescription(final String shortdesc) {
        this.shortdesc = shortdesc;
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

    public int getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(final int inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    /**
     * @return {@code true} if the cache is on the user's watchlist, {@code false} otherwise
     */
    public boolean isOnWatchlist() {
        return BooleanUtils.isTrue(onWatchlist);
    }

    public void setOnWatchlist(final boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    /**
     * return an immutable list of waypoints.
     *
     * @return always non <code>null</code>
     */
    public List<Waypoint> getWaypoints() {
        return waypoints.getUnderlyingList();
    }

    /**
     * @param waypoints
     *            List of waypoints to set for cache
     * @param saveToDatabase
     *            Indicates whether to add the waypoints to the database. Should be false if
     *            called while loading or building a cache
     * @return <code>true</code> if waypoints successfully added to waypoint database
     */
    public boolean setWaypoints(final List<Waypoint> waypoints, final boolean saveToDatabase) {
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
        return DataStore.loadLogs(geocode);
    }

    /**
     * @return only the logs of friends
     */
    @NonNull
    public List<LogEntry> getFriendsLogs() {
        final ArrayList<LogEntry> friendLogs = new ArrayList<>();
        for (final LogEntry log : getLogs()) {
            if (log.friend) {
                friendLogs.add(log);
            }
        }
        return Collections.unmodifiableList(friendLogs);
    }

    public boolean isLogOffline() {
        return BooleanUtils.isTrue(logOffline);
    }

    public void setLogOffline(final boolean logOffline) {
        this.logOffline = logOffline;
    }

    public boolean isStatusChecked() {
        return statusChecked;
    }

    public void setStatusChecked(final boolean statusChecked) {
        this.statusChecked = statusChecked;
    }

    public String getDirectionImg() {
        return directionImg;
    }

    public void setDirectionImg(final String directionImg) {
        this.directionImg = directionImg;
    }

    public void setGeocode(final String geocode) {
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

    public void setOwnerUserId(final String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public void setHint(final String hint) {
        this.hint = hint;
    }

    public void setSize(final CacheSize size) {
        if (size == null) {
            this.size = CacheSize.UNKNOWN;
        }
        else {
            this.size = size;
        }
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
        this.personalNote = StringUtils.trimToNull(personalNote);
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

    public void setAttributes(final List<String> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            this.attributes.addAll(attributes);
        }
    }

    public void setSpoilers(final List<Image> spoilers) {
        this.spoilers = spoilers;
    }

    public void setInventory(final List<Trackable> inventory) {
        this.inventory = inventory;
    }

    public void setLogCounts(final Map<LogType, Integer> logCounts) {
        this.logCounts = logCounts;
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.IBasicCache#getType()
     *
     * @returns Never null
     */
    public CacheType getType() {
        return cacheType.getValue();
    }

    public void setType(final CacheType cacheType) {
        if (cacheType == null || CacheType.ALL == cacheType) {
            throw new IllegalArgumentException("Illegal cache type");
        }
        this.cacheType = new UncertainProperty<>(cacheType);
    }

    public void setType(final CacheType cacheType, final int zoomlevel) {
        if (cacheType == null || CacheType.ALL == cacheType) {
            throw new IllegalArgumentException("Illegal cache type");
        }
        this.cacheType = new UncertainProperty<>(cacheType, zoomlevel);
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
    public EnumSet<StorageLocation> getStorageLocation() {
        return storageLocation;
    }

    /**
     * @param storageLocation
     *            the storageLocation to set
     */
    public void addStorageLocation(final StorageLocation storageLocation) {
        this.storageLocation.add(storageLocation);
    }

    /**
     * @param waypoint
     *            Waypoint to add to the cache
     * @param saveToDatabase
     *            Indicates whether to add the waypoint to the database. Should be false if
     *            called while loading or building a cache
     * @return <code>true</code> if waypoint successfully added to waypoint database
     */
    public boolean addOrChangeWaypoint(final Waypoint waypoint, final boolean saveToDatabase) {
        waypoint.setGeocode(geocode);

        if (waypoint.getId() < 0) { // this is a new waypoint
            assignUniquePrefix(waypoint);
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

    /*
     * Assigns a unique two-digit (compatibility with gc.com)
     * prefix within the scope of this cache.
     */
    private void assignUniquePrefix(final Waypoint waypoint) {
        // gather existing prefixes
        final Set<String> assignedPrefixes = new HashSet<>();
        for (final Waypoint wp : waypoints) {
            assignedPrefixes.add(wp.getPrefix());
        }

        for (int i = OWN_WP_PREFIX_OFFSET; i < 100; i++) {
            final String prefixCandidate = StringUtils.leftPad(String.valueOf(i), 2, '0');
            if (!assignedPrefixes.contains(prefixCandidate)) {
                waypoint.setPrefix(prefixCandidate);
                break;
            }
        }

    }

    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    public boolean hasFinalDefined() {
        return finalDefined;
    }

    // Only for loading
    public void setFinalDefined(final boolean finalDefined) {
        this.finalDefined = finalDefined;
    }

    /**
     * Reset <code>finalDefined</code> based on current list of stored waypoints
     */
    private void resetFinalDefined() {
        finalDefined = false;
        for (final Waypoint wp : waypoints) {
            if (wp.isFinalWithCoords()) {
                finalDefined = true;
                break;
            }
        }
    }

    public boolean hasUserModifiedCoords() {
        return userModifiedCoords;
    }

    public void setUserModifiedCoords(final boolean coordsChanged) {
        userModifiedCoords = coordsChanged;
    }

    /**
     * Duplicate a waypoint.
     *
     * @param original
     *            the waypoint to duplicate
     * @return <code>true</code> if the waypoint was duplicated, <code>false</code> otherwise (invalid index)
     */
    public boolean duplicateWaypoint(final Waypoint original) {
        if (original == null) {
            return false;
        }
        final int index = getWaypointIndex(original);
        final Waypoint copy = new Waypoint(original);
        copy.setUserDefined();
        copy.setName(CgeoApplication.getInstance().getString(R.string.waypoint_copy_of) + " " + copy.getName());
        waypoints.add(index + 1, copy);
        return DataStore.saveWaypoint(-1, geocode, copy);
    }

    /**
     * delete a user defined waypoint
     *
     * @param waypoint
     *            to be removed from cache
     * @return <code>true</code>, if the waypoint was deleted
     */
    public boolean deleteWaypoint(final Waypoint waypoint) {
        if (waypoint == null) {
            return false;
        }
        if (waypoint.getId() < 0) {
            return false;
        }
        if (waypoint.isUserDefined()) {
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
     * Find index of given <code>waypoint</code> in cache's <code>waypoints</code> list
     *
     * @param waypoint
     *            to find index for
     * @return index in <code>waypoints</code> if found, -1 otherwise
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
     * @param id
     *            the id of the waypoint to look for
     * @return waypoint or <code>null</code>
     */
    public Waypoint getWaypointById(final int id) {
        for (final Waypoint waypoint : waypoints) {
            if (waypoint.getId() == id) {
                return waypoint;
            }
        }
        return null;
    }

    /**
     * Detect coordinates in the personal note and convert them to user defined waypoints. Works by rule of thumb.
     */
    public void parseWaypointsFromNote() {
        for (final Waypoint waypoint : Waypoint.parseWaypointsFromNote(StringUtils.defaultString(getPersonalNote()))) {
            if (!hasIdenticalWaypoint(waypoint.getCoords())) {
                addOrChangeWaypoint(waypoint, false);
            }
        }
    }

    private boolean hasIdenticalWaypoint(final Geopoint point) {
        for (final Waypoint waypoint: waypoints) {
            // waypoint can have no coords such as a Final set by cache owner
            final Geopoint coords = waypoint.getCoords();
            if (coords != null && coords.equals(point)) {
                return true;
            }
        }
        return false;
    }

    /*
     * For working in the debugger
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
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

    public void store(final CancellableHandler handler) {
        store(StoredList.TEMPORARY_LIST.id, handler);
    }

    public void store(final int listId, final CancellableHandler handler) {
        final int newListId = listId < StoredList.STANDARD_LIST_ID
                ? Math.max(getListId(), StoredList.STANDARD_LIST_ID)
                : listId;
        storeCache(this, null, newListId, false, handler);
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public WaypointType getWaypointType() {
        return null;
    }

    @Override
    public String getCoordType() {
        return "cache";
    }

    public Subscription drop(final Handler handler) {
        return Schedulers.io().createWorker().schedule(new Action0() {
            @Override
            public void call() {
                try {
                    dropSynchronous();
                    handler.sendMessage(Message.obtain());
                } catch (final Exception e) {
                    Log.e("cache.drop: ", e);
                }
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
        warnIncorrectParsingIf(getSize() == null, "size");
        warnIncorrectParsingIf(getType() == null || getType() == CacheType.UNKNOWN, "type");
        warnIncorrectParsingIf(getCoords() == null, "coordinates");
        warnIncorrectParsingIfBlank(getLocation(), "location");
    }

    public Subscription refresh(final CancellableHandler handler, final Scheduler scheduler) {
        return scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                refreshSynchronous(handler);
            }
        });
    }

    public void refreshSynchronous(final CancellableHandler handler) {
        DataStore.removeCache(geocode, EnumSet.of(RemoveFlag.CACHE));
        storeCache(null, geocode, listId, true, handler);
    }

    public static void storeCache(final Geocache origCache, final String geocode, final int listId, final boolean forceRedownload, final CancellableHandler handler) {
        try {
            Geocache cache = null;
            // get cache details, they may not yet be complete
            if (origCache != null) {
                SearchResult search = null;
                // only reload the cache if it was already stored or doesn't have full details (by checking the description)
                if (origCache.isOffline() || StringUtils.isBlank(origCache.getDescription())) {
                    search = searchByGeocode(origCache.getGeocode(), null, listId, false, handler);
                }
                if (search != null) {
                    cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                } else {
                    cache = origCache;
                }
            } else if (StringUtils.isNotBlank(geocode)) {
                final SearchResult search = searchByGeocode(geocode, null, listId, forceRedownload, handler);
                if (search != null) {
                    cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                }
            }

            if (cache == null) {
                if (handler != null) {
                    handler.sendMessage(Message.obtain());
                }

                return;
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            final HtmlImage imgGetter = new HtmlImage(cache.getGeocode(), false, listId, true);

            // store images from description
            if (StringUtils.isNotBlank(cache.getDescription())) {
                Html.fromHtml(cache.getDescription(), imgGetter, null);
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            // store spoilers
            if (CollectionUtils.isNotEmpty(cache.getSpoilers())) {
                for (final Image oneSpoiler : cache.getSpoilers()) {
                    imgGetter.getDrawable(oneSpoiler.getUrl());
                }
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            // store images from logs
            if (Settings.isStoreLogImages()) {
                for (final LogEntry log : cache.getLogs()) {
                    if (log.hasLogImages()) {
                        for (final Image oneLogImg : log.getLogImages()) {
                            imgGetter.getDrawable(oneLogImg.getUrl());
                        }
                    }
                }
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            cache.setListId(listId);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            RxUtils.waitForCompletion(StaticMapsProvider.downloadMaps(cache), imgGetter.waitForEndObservable(handler));

            if (handler != null) {
                handler.sendEmptyMessage(CancellableHandler.DONE);
            }
        } catch (final Exception e) {
            Log.e("Geocache.storeCache", e);
        }
    }

    public static SearchResult searchByGeocode(final String geocode, final String guid, final int listId, final boolean forceReload, final CancellableHandler handler) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            Log.e("Geocache.searchByGeocode: No geocode nor guid given");
            return null;
        }

        if (!forceReload && listId == StoredList.TEMPORARY_LIST.id && (DataStore.isOffline(geocode, guid) || DataStore.isThere(geocode, guid, true, true))) {
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
        return listId >= StoredList.STANDARD_LIST_ID;
    }

    /**
     * guess an event start time from the description
     *
     * @return start time in minutes after midnight
     */
    public String guessEventTimeMinutes() {
        if (!isEventCache()) {
            return null;
        }

        final String hourLocalized = CgeoApplication.getInstance().getString(R.string.cache_time_full_hours);
        final ArrayList<Pattern> patterns = new ArrayList<>();

        // 12:34
        patterns.add(Pattern.compile("\\b(\\d{1,2})\\:(\\d\\d)\\b"));
        if (StringUtils.isNotBlank(hourLocalized)) {
            // 17 - 20 o'clock
            patterns.add(Pattern.compile("\\b(\\d{1,2})(?:\\.00)?" + "\\s*(?:-|[a-z]+)\\s*" + "(?:\\d{1,2})(?:\\.00)?" + "\\s+" + Pattern.quote(hourLocalized), Pattern.CASE_INSENSITIVE));
            // 12 o'clock, 12.00 o'clock
            patterns.add(Pattern.compile("\\b(\\d{1,2})(?:\\.00)?\\s+" + Pattern.quote(hourLocalized), Pattern.CASE_INSENSITIVE));
        }

        final String searchText = getShortDescription() + ' ' + getDescription();
        for (final Pattern pattern : patterns) {
            final MatcherWrapper matcher = new MatcherWrapper(pattern, searchText);
            while (matcher.find()) {
                try {
                    final int hours = Integer.parseInt(matcher.group(1));
                    int minutes = 0;
                    if (matcher.groupCount() >= 2) {
                        minutes = Integer.parseInt(matcher.group(2));
                    }
                    if (hours >= 0 && hours < 24 && minutes >= 0 && minutes < 60) {
                        return String.valueOf(hours * 60 + minutes);
                    }
                } catch (final NumberFormatException ignored) {
                    // cannot happen, but static code analysis doesn't know
                }
            }
        }
        return null;
    }

    public boolean hasStaticMap() {
        return StaticMapsProvider.hasStaticMap(this);
    }

    public static final Predicate<Geocache> hasStaticMap = new Predicate<Geocache>() {
        @Override
        public boolean evaluate(final Geocache cache) {
            return cache.hasStaticMap();
        }
    };

    public Collection<Image> getImages() {
        final LinkedList<Image> result = new LinkedList<>();
        result.addAll(getSpoilers());
        addLocalSpoilersTo(result);
        for (final LogEntry log : getLogs()) {
            result.addAll(log.getLogImages());
        }
        ImageUtils.addImagesFromHtml(result, getDescription(), geocode);
        return result;
    }

    /**
     * Add spoilers stored locally in <tt>/sdcard/GeocachePhotos</tt>. If a cache is named GC123ABC, the
     * directory will be <tt>/sdcard/GeocachePhotos/C/B/GC123ABC/</tt>.
     *
     * @param spoilers the list to add to
     */
    private void addLocalSpoilersTo(final List<Image> spoilers) {
        if (StringUtils.length(geocode) >= 2) {
            final String suffix = StringUtils.right(geocode, 2);
            final File baseDir = new File(Environment.getExternalStorageDirectory(), "GeocachePhotos");
            final File lastCharDir = new File(baseDir, suffix.substring(1));
            final File secondToLastCharDir = new File(lastCharDir, suffix.substring(0, 1));
            final File finalDir = new File(secondToLastCharDir, geocode);
            final File[] files = finalDir.listFiles();
            if (files != null) {
                for (final File image : files) {
                    spoilers.add(new Image("file://" + image.getAbsolutePath(), image.getName()));
                }
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
    public boolean hasOwnLog(final LogType logType) {
        for (final LogEntry logEntry : getLogs()) {
            if (logEntry.type == logType && logEntry.isOwn()) {
                return true;
            }
        }
        return false;
    }

    public int getMapMarkerId() {
        return getConnector().getCacheMapMarkerId(isDisabled() || isArchived());
    }

    public boolean isLogPasswordRequired() {
        return logPasswordRequired;
    }

    public void setLogPasswordRequired(final boolean required) {
        logPasswordRequired = required;
    }

    public String getWaypointGpxId(final String prefix) {
        return getConnector().getWaypointGpxId(prefix, geocode);
    }

    public String getWaypointPrefix(final String name) {
        return getConnector().getWaypointPrefix(name);
    }

    /**
     * Get number of overall finds for a cache, or 0 if the number of finds is not known.
     */
    public int getFindsCount() {
        if (getLogCounts().isEmpty()) {
            setLogCounts(DataStore.loadLogCounts(getGeocode()));
        }
        final Integer logged = getLogCounts().get(LogType.FOUND_IT);
        if (logged != null) {
            return logged;
        }
        return 0;
    }

    public boolean applyDistanceRule() {
        return (getType().applyDistanceRule() || hasUserModifiedCoords()) && getConnector() == GCConnector.getInstance();
    }

    public LogType getDefaultLogType() {
        if (isEventCache()) {
            final Date eventDate = getHiddenDate();
            final boolean expired = DateUtils.isPastEvent(this);

            if (hasOwnLog(LogType.WILL_ATTEND) || expired || (eventDate != null && DateUtils.daysSince(eventDate.getTime()) == 0)) {
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
    public static Set<String> getGeocodes(final Collection<Geocache> caches) {
        final Set<String> geocodes = new HashSet<>(caches.size());
        for (final Geocache cache : caches) {
            final String geocode = cache.getGeocode();
            if (StringUtils.isNotBlank(geocode)) {
                geocodes.add(geocode);
            }
        }
        return geocodes;
    }

}
