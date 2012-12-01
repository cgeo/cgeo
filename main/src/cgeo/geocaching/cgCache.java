package cgeo.geocaching;

import cgeo.geocaching.cgData.StorageLocation;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.GPXParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.LazyInitializedList;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.LogTemplateProvider;
import cgeo.geocaching.utils.LogTemplateProvider.LogContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spannable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal c:geo representation of a "cache"
 */
public class cgCache implements ICache, IWaypoint {

    private long updated = 0;
    private long detailedUpdate = 0;
    private long visitedDate = 0;
    private int listId = StoredList.TEMPORARY_LIST_ID;
    private boolean detailed = false;
    private String geocode = "";
    private String cacheId = "";
    private String guid = "";
    private CacheType cacheType = CacheType.UNKNOWN;
    private String name = "";
    private Spannable nameSp = null;
    private String ownerDisplayName = "";
    private String ownerUserId = "";
    private Date hidden = null;
    private String hint = "";
    private CacheSize size = CacheSize.UNKNOWN;
    private float difficulty = 0;
    private float terrain = 0;
    private Float direction = null;
    private Float distance = null;
    private String latlon = "";
    private String location = "";
    private Geopoint coords = null;
    private boolean reliableLatLon = false;
    private Double elevation = null;
    private String personalNote = null;
    private String shortdesc = "";
    private String description = null;
    private boolean disabled = false;
    private boolean archived = false;
    private boolean premiumMembersOnly = false;
    private boolean found = false;
    private boolean favorite = false;
    private boolean own = false;
    private int favoritePoints = 0;
    private float rating = 0; // valid ratings are larger than zero
    private int votes = 0;
    private float myVote = 0; // valid ratings are larger than zero
    private int inventoryItems = 0;
    private boolean onWatchlist = false;
    private LazyInitializedList<String> attributes = new LazyInitializedList<String>() {
        @Override
        protected List<String> loadFromDatabase() {
            return cgData.loadAttributes(geocode);
        }
    };
    private LazyInitializedList<cgWaypoint> waypoints = new LazyInitializedList<cgWaypoint>() {
        @Override
        protected List<cgWaypoint> loadFromDatabase() {
            return cgData.loadWaypoints(geocode);
        }
    };
    private List<cgImage> spoilers = null;
    private LazyInitializedList<LogEntry> logs = new LazyInitializedList<LogEntry>() {
        @Override
        protected List<LogEntry> loadFromDatabase() {
            return cgData.loadLogs(geocode);
        }
    };
    private List<cgTrackable> inventory = null;
    private Map<LogType, Integer> logCounts = new HashMap<LogType, Integer>();
    private boolean logOffline = false;
    private boolean userModifiedCoords = false;
    // temporary values
    private boolean statusChecked = false;
    private String directionImg = "";
    private String nameForSorting;
    private final EnumSet<StorageLocation> storageLocation = EnumSet.of(StorageLocation.HEAP);
    private boolean finalDefined = false;
    private int zoomlevel = Tile.ZOOMLEVEL_MAX + 1;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private Handler changeNotificationHandler = null;

    /**
     * Create a new cache. To be used everywhere except for the GPX parser
     */
    public cgCache() {
        // empty
    }

    /**
     * Cache constructor to be used by the GPX parser only. This constructor explicitly sets several members to empty
     * lists.
     *
     * @param gpxParser
     */
    public cgCache(GPXParser gpxParser) {
        setReliableLatLon(true);
        setAttributes(Collections.<String> emptyList());
        setWaypoints(Collections.<cgWaypoint> emptyList(), false);
        setLogs(Collections.<LogEntry> emptyList());
    }

    public void setChangeNotificationHandler(Handler newNotificationHandler) {
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
     * Gather missing information from another cache object.
     *
     * @param other
     *            the other version, or null if non-existent
     * @return true if this cache is "equal" to the other version
     */
    public boolean gatherMissingFrom(final cgCache other) {
        if (other == null) {
            return false;
        }

        updated = System.currentTimeMillis();
        if (!detailed && (other.detailed || zoomlevel < other.zoomlevel)) {
            detailed = other.detailed;
            detailedUpdate = other.detailedUpdate;
            coords = other.coords;
            cacheType = other.cacheType;
            zoomlevel = other.zoomlevel;
            // boolean values must be enumerated here. Other types are assigned outside this if-statement
            premiumMembersOnly = other.premiumMembersOnly;
            reliableLatLon = other.reliableLatLon;
            archived = other.archived;
            found = other.found;
            own = other.own;
            disabled = other.disabled;
            favorite = other.favorite;
            onWatchlist = other.onWatchlist;
            logOffline = other.logOffline;
            finalDefined = other.finalDefined;
        }

        /*
         * No gathering for boolean members if other cache is not-detailed
         * and does not have information with higher reliability (denoted by zoomlevel)
         * - found
         * - own
         * - disabled
         * - favorite
         * - onWatchlist
         * - logOffline
         */
        if (visitedDate == 0) {
            visitedDate = other.visitedDate;
        }
        if (listId == StoredList.TEMPORARY_LIST_ID) {
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
        if (null == cacheType || CacheType.UNKNOWN == cacheType) {
            cacheType = other.cacheType;
        }
        if (StringUtils.isBlank(name)) {
            name = other.name;
        }
        if (StringUtils.isBlank(nameSp)) {
            nameSp = other.nameSp;
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
        if (StringUtils.isBlank(hint)) {
            hint = other.hint;
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
        if (StringUtils.isBlank(latlon)) {
            latlon = other.latlon;
        }
        if (StringUtils.isBlank(location)) {
            location = other.location;
        }
        if (coords == null) {
            coords = other.coords;
        }
        if (elevation == null) {
            elevation = other.elevation;
        }
        if (personalNote == null) { // don't use StringUtils.isBlank here. Otherwise we cannot recognize a note which was deleted on GC
            personalNote = other.personalNote;
        }
        if (StringUtils.isBlank(shortdesc)) {
            shortdesc = other.shortdesc;
        }
        if (StringUtils.isBlank(description)) {
            description = other.description;
        }
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
        if (attributes.isEmpty()) {
            attributes.set(other.attributes);
        }
        if (waypoints.isEmpty()) {
            this.setWaypoints(other.waypoints.asList(), false);
        }
        else {
            ArrayList<cgWaypoint> newPoints = new ArrayList<cgWaypoint>(waypoints.asList());
            cgWaypoint.mergeWayPoints(newPoints, other.waypoints.asList(), false);
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
        if (logs.isEmpty()) { // keep last known logs if none
            logs.set(other.logs);
        }
        if (logCounts.size() == 0) {
            logCounts = other.logCounts;
        }
        if (!userModifiedCoords) {
            userModifiedCoords = other.userModifiedCoords;
        }
        if (!reliableLatLon) {
            reliableLatLon = other.reliableLatLon;
        }
        if (zoomlevel == -1) {
            zoomlevel = other.zoomlevel;
        }

        boolean isEqual = isEqualTo(other);

        if (!isEqual) {
            notifyChange();
        }

        return isEqual;
    }

    /**
     * Compare two caches quickly. For map and list fields only the references are compared !
     *
     * @param other the other cache to compare this one to
     * @return true if both caches have the same content
     */
    private boolean isEqualTo(final cgCache other) {
        return detailed == other.detailed &&
                StringUtils.equalsIgnoreCase(geocode, other.geocode) &&
                StringUtils.equalsIgnoreCase(name, other.name) &&
                cacheType == other.cacheType &&
                size == other.size &&
                found == other.found &&
                own == other.own &&
                premiumMembersOnly == other.premiumMembersOnly &&
                difficulty == other.difficulty &&
                terrain == other.terrain &&
                (coords != null ? coords.equals(other.coords) : null == other.coords) &&
                reliableLatLon == other.reliableLatLon &&
                disabled == other.disabled &&
                archived == other.archived &&
                listId == other.listId &&
                StringUtils.equalsIgnoreCase(ownerDisplayName, other.ownerDisplayName) &&
                StringUtils.equalsIgnoreCase(ownerUserId, other.ownerUserId) &&
                StringUtils.equalsIgnoreCase(description, other.description) &&
                StringUtils.equalsIgnoreCase(personalNote, other.personalNote) &&
                StringUtils.equalsIgnoreCase(shortdesc, other.shortdesc) &&
                StringUtils.equalsIgnoreCase(latlon, other.latlon) &&
                StringUtils.equalsIgnoreCase(location, other.location) &&
                favorite == other.favorite &&
                favoritePoints == other.favoritePoints &&
                onWatchlist == other.onWatchlist &&
                (hidden != null ? hidden.equals(other.hidden) : null == other.hidden) &&
                StringUtils.equalsIgnoreCase(guid, other.guid) &&
                StringUtils.equalsIgnoreCase(hint, other.hint) &&
                StringUtils.equalsIgnoreCase(cacheId, other.cacheId) &&
                (direction != null ? direction.equals(other.direction) : null == other.direction) &&
                (distance != null ? distance.equals(other.distance) : null == other.distance) &&
                (elevation != null ? elevation.equals(other.elevation) : null == other.elevation) &&
                nameSp == other.nameSp &&
                rating == other.rating &&
                votes == other.votes &&
                myVote == other.myVote &&
                inventoryItems == other.inventoryItems &&
                attributes == other.attributes &&
                waypoints == other.waypoints &&
                spoilers == other.spoilers &&
                logs == other.logs &&
                inventory == other.inventory &&
                logCounts == other.logCounts &&
                logOffline == other.logOffline &&
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

    /**
     * Checks if a page contains the guid of a cache
     *
     * @param page
     *            the page to search in, may be null
     * @return true if the page contains the guid of the cache, false otherwise
     */
    public boolean isGuidContainedInPage(final String page) {
        if (StringUtils.isBlank(page) || StringUtils.isBlank(guid)) {
            return false;
        }
        final Boolean found = Pattern.compile(guid, Pattern.CASE_INSENSITIVE).matcher(page).find();
        Log.i("cgCache.isGuidContainedInPage: guid '" + guid + "' " + (found ? "" : "not ") + "found");
        return found;
    }

    public boolean isEventCache() {
        return cacheType.isEvent();
    }

    public void logVisit(final IAbstractActivity fromActivity) {
        if (StringUtils.isBlank(cacheId)) {
            fromActivity.showToast(((Activity) fromActivity).getResources().getString(R.string.err_cannot_log_visit));
            return;
        }
        Intent logVisitIntent = new Intent((Activity) fromActivity, VisitCacheActivity.class);
        logVisitIntent.putExtra(VisitCacheActivity.EXTRAS_ID, cacheId);
        logVisitIntent.putExtra(VisitCacheActivity.EXTRAS_GEOCODE, geocode);
        logVisitIntent.putExtra(VisitCacheActivity.EXTRAS_FOUND, found);

        ((Activity) fromActivity).startActivity(logVisitIntent);
    }

    public void logOffline(final Activity fromActivity, final LogType logType) {
        final boolean mustIncludeSignature = StringUtils.isNotBlank(Settings.getSignature()) && Settings.isAutoInsertSignature();
        final String initial = mustIncludeSignature ? LogTemplateProvider.applyTemplates(Settings.getSignature(), new LogContext(this, true)) : "";
        logOffline(fromActivity, initial, Calendar.getInstance(), logType);
    }

    void logOffline(final Activity fromActivity, final String log, Calendar date, final LogType logType) {
        if (logType == LogType.UNKNOWN) {
            return;
        }
        final boolean status = cgData.saveLogOffline(geocode, date.getTime(), logType, log);

        Resources res = fromActivity.getResources();
        if (status) {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.info_log_saved));
            cgData.saveVisitDate(geocode);
            logOffline = true;

            notifyChange();
        } else {
            ActivityMixin.showToast(fromActivity, res.getString(R.string.err_log_post_failed));
        }
    }

    public List<LogType> getPossibleLogTypes() {
        boolean isOwner = getOwnerUserId() != null && getOwnerUserId().equalsIgnoreCase(Settings.getUsername());
        List<LogType> logTypes = new ArrayList<LogType>();
        if (isEventCache()) {
            logTypes.add(LogType.WILL_ATTEND);
            logTypes.add(LogType.NOTE);
            logTypes.add(LogType.ATTENDED);
            logTypes.add(LogType.NEEDS_ARCHIVE);
            if (isOwner) {
                logTypes.add(LogType.ANNOUNCEMENT);
            }
        } else if (CacheType.WEBCAM == cacheType) {
            logTypes.add(LogType.WEBCAM_PHOTO_TAKEN);
            logTypes.add(LogType.DIDNT_FIND_IT);
            logTypes.add(LogType.NOTE);
            logTypes.add(LogType.NEEDS_ARCHIVE);
            logTypes.add(LogType.NEEDS_MAINTENANCE);
        } else {
            logTypes.add(LogType.FOUND_IT);
            logTypes.add(LogType.DIDNT_FIND_IT);
            logTypes.add(LogType.NOTE);
            logTypes.add(LogType.NEEDS_ARCHIVE);
            logTypes.add(LogType.NEEDS_MAINTENANCE);
        }
        if (isOwner) {
            logTypes.add(LogType.OWNER_MAINTENANCE);
            logTypes.add(LogType.TEMP_DISABLE_LISTING);
            logTypes.add(LogType.ENABLE_LISTING);
            logTypes.add(LogType.ARCHIVE);
            logTypes.remove(LogType.UPDATE_COORDINATES);
        }
        return logTypes;
    }

    public void openInBrowser(Activity fromActivity) {
        fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getCacheUrl())));
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
        return getConnector().supportsFavoritePoints();
    }

    public boolean supportsLogging() {
        return getConnector().supportsLogging();
    }

    @Override
    public float getDifficulty() {
        return difficulty;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    @Override
    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    @Override
    public CacheSize getSize() {
        if (size == null) {
            return CacheSize.UNKNOWN;
        }
        return size;
    }

    @Override
    public float getTerrain() {
        return terrain;
    }

    @Override
    public boolean isArchived() {
        return archived;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public boolean isPremiumMembersOnly() {
        return premiumMembersOnly;
    }

    public void setPremiumMembersOnly(boolean members) {
        this.premiumMembersOnly = members;
    }

    @Override
    public boolean isOwn() {
        return own;
    }

    @Override
    public String getOwnerUserId() {
        return ownerUserId;
    }

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public String getDescription() {
        if (description == null) {
            description = StringUtils.defaultString(cgData.getCacheDescription(geocode));
        }
        return description;
    }

    @Override
    public String getShortDescription() {
        return shortdesc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCacheId() {
        if (StringUtils.isBlank(cacheId) && getConnector().equals(GCConnector.getInstance())) {
            return String.valueOf(GCConstants.gccodeToGCId(geocode));
        }

        return cacheId;
    }

    @Override
    public String getGuid() {
        return guid;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public String getPersonalNote() {
        // non premium members have no personal notes, premium members have an empty string by default.
        // map both to null, so other code doesn't need to differentiate
        if (StringUtils.isBlank(personalNote)) {
            return null;
        }
        return personalNote;
    }

    public boolean supportsUserActions() {
        return getConnector().supportsUserActions();
    }

    public boolean supportsCachesAround() {
        return getConnector() instanceof ISearchByCenter;
    }

    public void shareCache(Activity fromActivity, Resources res) {
        if (geocode == null) {
            return;
        }

        StringBuilder subject = new StringBuilder("Geocache ");
        subject.append(geocode);
        if (StringUtils.isNotBlank(name)) {
            subject.append(" - ").append(name);
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject.toString());
        intent.putExtra(Intent.EXTRA_TEXT, getUrl());

        fromActivity.startActivity(Intent.createChooser(intent, res.getText(R.string.action_bar_share_title)));
    }

    public String getUrl() {
        return getConnector().getCacheUrl(this);
    }

    public boolean supportsGCVote() {
        return StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean isFound() {
        return found;
    }

    @Override
    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favourite) {
        this.favorite = favourite;
    }


    @Override
    public boolean isWatchlist() {
        return onWatchlist;
    }

    @Override
    public Date getHiddenDate() {
        return hidden;
    }

    @Override
    public LazyInitializedList<String> getAttributes() {
        return attributes;
    }

    @Override
    public List<cgTrackable> getInventory() {
        return inventory;
    }

    public void addSpoiler(final cgImage spoiler) {
        if (spoilers == null) {
            spoilers = new ArrayList<cgImage>();
        }
        spoilers.add(spoiler);
    }

    @Override
    public List<cgImage> getSpoilers() {
        if (spoilers == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(spoilers);
    }

    @Override
    public Map<LogType, Integer> getLogCounts() {
        return logCounts;
    }

    @Override
    public int getFavoritePoints() {
        return favoritePoints;
    }

    @Override
    public String getNameForSorting() {
        if (null == nameForSorting) {
            final Matcher matcher = NUMBER_PATTERN.matcher(name);
            if (matcher.find()) {
                nameForSorting = name.replace(matcher.group(), StringUtils.leftPad(matcher.group(), 6, '0'));
            }
            else {
                nameForSorting = name;
            }
        }
        return nameForSorting;
    }

    public boolean isVirtual() {
        return CacheType.VIRTUAL == cacheType || CacheType.WEBCAM == cacheType
                || CacheType.EARTH == cacheType;
    }

    public boolean showSize() {
        return !((isEventCache() || isVirtual()) && size == CacheSize.NOT_CHOSEN);
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public long getDetailedUpdate() {
        return detailedUpdate;
    }

    public void setDetailedUpdate(long detailedUpdate) {
        this.detailedUpdate = detailedUpdate;
    }

    public long getVisitedDate() {
        return visitedDate;
    }

    public void setVisitedDate(long visitedDate) {
        this.visitedDate = visitedDate;
    }

    public int getListId() {
        return listId;
    }

    public void setListId(int listId) {
        this.listId = listId;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public void setDetailed(boolean detailed) {
        this.detailed = detailed;
    }

    public Spannable getNameSp() {
        return nameSp;
    }

    public void setNameSp(Spannable nameSp) {
        this.nameSp = nameSp;
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

    public void setDirection(Float direction) {
        this.direction = direction;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public String getLatlon() {
        return latlon;
    }

    public void setLatlon(String latlon) {
        this.latlon = latlon;
    }

    @Override
    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(Geopoint coords) {
        this.coords = coords;
    }

    /**
     * @return true if the coords are from the cache details page and the user has been logged in
     */
    public boolean isReliableLatLon() {
        return getConnector().isReliableLatLon(reliableLatLon);
    }

    public void setReliableLatLon(boolean reliableLatLon) {
        this.reliableLatLon = reliableLatLon;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public String getShortdesc() {
        return shortdesc;
    }

    public void setShortdesc(String shortdesc) {
        this.shortdesc = shortdesc;
    }

    public void setFavoritePoints(int favoriteCnt) {
        this.favoritePoints = favoriteCnt;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public float getMyVote() {
        return myVote;
    }

    public void setMyVote(float myVote) {
        this.myVote = myVote;
    }

    public int getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(int inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public boolean isOnWatchlist() {
        return onWatchlist;
    }

    public void setOnWatchlist(boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    /**
     * return an immutable list of waypoints.
     *
     * @return always non <code>null</code>
     */
    public List<cgWaypoint> getWaypoints() {
        return waypoints.asList();
    }

    /**
     * @param waypoints
     *            List of waypoints to set for cache
     * @param saveToDatabase
     *            Indicates whether to add the waypoints to the database. Should be false if
     *            called while loading or building a cache
     * @return <code>true</code> if waypoints successfully added to waypoint database
     */
    public boolean setWaypoints(List<cgWaypoint> waypoints, boolean saveToDatabase) {
        this.waypoints.set(waypoints);
        finalDefined = false;
        if (waypoints != null) {
            for (cgWaypoint waypoint : waypoints) {
                waypoint.setGeocode(geocode);
                if (waypoint.isFinalWithCoords()) {
                    finalDefined = true;
                }
            }
        }
        return saveToDatabase && cgData.saveWaypoints(this);
    }

    /**
     * @return never <code>null</code>
     */
    public LazyInitializedList<LogEntry> getLogs() {
        return logs;
    }

    /**
     * @return only the logs of friends, never <code>null</code>
     */
    public List<LogEntry> getFriendsLogs() {
        ArrayList<LogEntry> friendLogs = new ArrayList<LogEntry>();
        for (LogEntry log : logs) {
            if (log.friend) {
                friendLogs.add(log);
            }
        }
        return Collections.unmodifiableList(friendLogs);
    }

    /**
     * @param logs
     *            the log entries
     */
    public void setLogs(List<LogEntry> logs) {
        this.logs.set(logs);
    }

    public boolean isLogOffline() {
        return logOffline;
    }

    public void setLogOffline(boolean logOffline) {
        this.logOffline = logOffline;
    }

    public boolean isStatusChecked() {
        return statusChecked;
    }

    public void setStatusChecked(boolean statusChecked) {
        this.statusChecked = statusChecked;
    }

    public String getDirectionImg() {
        return directionImg;
    }

    public void setDirectionImg(String directionImg) {
        this.directionImg = directionImg;
    }

    public void setGeocode(String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public void setSize(CacheSize size) {
        if (size == null) {
            this.size = CacheSize.UNKNOWN;
        }
        else {
            this.size = size;
        }
    }

    public void setDifficulty(float difficulty) {
        this.difficulty = difficulty;
    }

    public void setTerrain(float terrain) {
        this.terrain = terrain;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPersonalNote(String personalNote) {
        this.personalNote = personalNote;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public void setOwn(boolean own) {
        this.own = own;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes.set(attributes);
    }

    public void setSpoilers(List<cgImage> spoilers) {
        this.spoilers = spoilers;
    }

    public void setInventory(List<cgTrackable> inventory) {
        this.inventory = inventory;
    }

    public void setLogCounts(Map<LogType, Integer> logCounts) {
        this.logCounts = logCounts;
    }

    /*
     * (non-Javadoc)
     *
     * @see cgeo.geocaching.IBasicCache#getType()
     *
     * @returns Never null
     */
    @Override
    public CacheType getType() {
        return cacheType;
    }

    public void setType(CacheType cacheType) {
        if (cacheType == null || CacheType.ALL == cacheType) {
            throw new IllegalArgumentException("Illegal cache type");
        }
        this.cacheType = cacheType;
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
    public boolean addOrChangeWaypoint(final cgWaypoint waypoint, boolean saveToDatabase) {
        waypoint.setGeocode(geocode);

        if (waypoint.getId() <= 0) { // this is a new waypoint
            waypoints.add(waypoint);
            if (waypoint.isFinalWithCoords()) {
                finalDefined = true;
            }
        } else { // this is a waypoint being edited
            final int index = getWaypointIndex(waypoint);
            if (index >= 0) {
                waypoints.remove(index);
            }
            waypoints.add(waypoint);
            // when waypoint was edited, finalDefined may have changed
            resetFinalDefined();
        }
        return saveToDatabase && cgData.saveWaypoint(waypoint.getId(), geocode, waypoint);
    }

    public boolean hasWaypoints() {
        return !waypoints.isEmpty();
    }

    public boolean hasFinalDefined() {
        return finalDefined;
    }

    // Only for loading
    public void setFinalDefined(boolean finalDefined) {
        this.finalDefined = finalDefined;
    }

    /**
     * Reset <code>finalDefined</code> based on current list of stored waypoints
     */
    private void resetFinalDefined() {
        finalDefined = false;
        for (cgWaypoint wp : waypoints) {
            if (wp.isFinalWithCoords()) {
                finalDefined = true;
                break;
            }
        }
    }

    public boolean hasUserModifiedCoords() {
        return userModifiedCoords;
    }

    public void setUserModifiedCoords(boolean coordsChanged) {
        this.userModifiedCoords = coordsChanged;
    }

    /**
     * Duplicate a waypoint.
     *
     * @param index the waypoint to duplicate
     * @return <code>true</code> if the waypoint was duplicated, <code>false</code> otherwise (invalid index)
     */
    public boolean duplicateWaypoint(final int index) {
        final cgWaypoint original = getWaypoint(index);
        if (original == null) {
            return false;
        }
        final cgWaypoint copy = new cgWaypoint(original);
        copy.setUserDefined();
        copy.setName(cgeoapplication.getInstance().getString(R.string.waypoint_copy_of) + " " + copy.getName());
        waypoints.add(index + 1, copy);
        return cgData.saveWaypoint(-1, geocode, copy);
    }

    /**
     * delete a user defined waypoint
     *
     * @param index
     *            of the waypoint in cache's waypoint list
     * @return <code>true</code>, if the waypoint was deleted
     */
    public boolean deleteWaypoint(final int index) {
        final cgWaypoint waypoint = getWaypoint(index);
        if (waypoint == null) {
            return false;
        }
        if (waypoint.isUserDefined()) {
            waypoints.remove(index);
            cgData.deleteWaypoint(waypoint.getId());
            cgData.removeCache(geocode, EnumSet.of(RemoveFlag.REMOVE_CACHE));
            // Check status if Final is defined
            if (waypoint.isFinalWithCoords()) {
                resetFinalDefined();
            }
            return true;
        }
        return false;
    }

    /**
     * delete a user defined waypoint
     *
     * @param waypoint
     *            to be removed from cache
     * @return <code>true</code>, if the waypoint was deleted
     */
    public boolean deleteWaypoint(final cgWaypoint waypoint) {
        if (waypoint.getId() <= 0) {
            return false;
        }

        final int index = getWaypointIndex(waypoint);
        return index >= 0 && deleteWaypoint(index);
    }

    /**
     * Find index of given <code>waypoint</code> in cache's <code>waypoints</code> list
     *
     * @param waypoint
     *            to find index for
     * @return index in <code>waypoints</code> if found, -1 otherwise
     */
    private int getWaypointIndex(final cgWaypoint waypoint) {
        final int id = waypoint.getId();
        for (int index = 0; index < waypoints.size(); index++) {
            if (waypoints.get(index).getId() == id) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Retrieve a given waypoint.
     *
     * @param index the index of the waypoint
     * @return waypoint or <code>null</code> if index is out of range
     */
    public cgWaypoint getWaypoint(final int index) {
        return index >= 0 && index < waypoints.size() ? waypoints.get(index) : null;
    }

    /**
     * Lookup a waypoint by its id.
     *
     * @param id the id of the waypoint to look for
     * @return waypoint or <code>null</code>
     */
    public cgWaypoint getWaypointById(final int id) {
        for (final cgWaypoint waypoint : waypoints) {
            if (waypoint.getId() == id) {
                return waypoint;
            }
        }
        return null;
    }

    public void parseWaypointsFromNote() {
        try {
            if (StringUtils.isBlank(getPersonalNote())) {
                return;
            }
            final Pattern coordPattern = Pattern.compile("\\b[nNsS]{1}\\s*\\d"); // begin of coordinates
            int count = 1;
            String note = getPersonalNote();
            Matcher matcher = coordPattern.matcher(note);
            while (matcher.find()) {
                try {
                    final Geopoint point = new Geopoint(note.substring(matcher.start()));
                    // coords must have non zero latitude and longitude and at least one part shall have fractional degrees
                    if (point.getLatitudeE6() != 0 && point.getLongitudeE6() != 0 && ((point.getLatitudeE6() % 1000) != 0 || (point.getLongitudeE6() % 1000) != 0)) {
                        final String name = cgeoapplication.getInstance().getString(R.string.cache_personal_note) + " " + count;
                        final cgWaypoint waypoint = new cgWaypoint(name, WaypointType.WAYPOINT, false);
                        waypoint.setCoords(point);
                        addOrChangeWaypoint(waypoint, false);
                        count++;
                    }
                } catch (Geopoint.ParseException e) {
                    // ignore
                }

                note = note.substring(matcher.start() + 1);
                matcher = coordPattern.matcher(note);
            }
        } catch (Exception e) {
            Log.e("cgCache.parseWaypointsFromNote: " + e.toString());
        }
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
        return geocode.hashCode() * name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof cgCache)) {
            return false;
        }
        // just compare the geocode even if that is not what "equals" normally does
        return StringUtils.isNotBlank(geocode) && geocode.equals(((cgCache) obj).geocode);
    }

    public void store(CancellableHandler handler) {
        final int listId = Math.max(getListId(), StoredList.STANDARD_LIST_ID);
        storeCache(this, null, listId, false, handler);
    }

    public void setZoomlevel(int zoomlevel) {
        this.zoomlevel = zoomlevel;
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

    public void drop(Handler handler) {
        try {
            cgData.markDropped(Collections.singletonList(this));
            cgData.removeCache(getGeocode(), EnumSet.of(RemoveFlag.REMOVE_CACHE));

            handler.sendMessage(Message.obtain());
        } catch (Exception e) {
            Log.e("cache.drop: ", e);
        }
    }

    public void checkFields() {
        if (StringUtils.isBlank(getGeocode())) {
            Log.e("geo code not parsed correctly");
        }
        if (StringUtils.isBlank(getName())) {
            Log.e("name not parsed correctly");
        }
        if (StringUtils.isBlank(getGuid())) {
            Log.e("guid not parsed correctly");
        }
        if (getTerrain() == 0.0) {
            Log.e("terrain not parsed correctly");
        }
        if (getDifficulty() == 0.0) {
            Log.e("difficulty not parsed correctly");
        }
        if (StringUtils.isBlank(getOwnerDisplayName())) {
            Log.e("owner display name not parsed correctly");
        }
        if (StringUtils.isBlank(getOwnerUserId())) {
            Log.e("owner user id real not parsed correctly");
        }
        if (getHiddenDate() == null) {
            Log.e("hidden not parsed correctly");
        }
        if (getFavoritePoints() < 0) {
            Log.e("favoriteCount not parsed correctly");
        }
        if (getSize() == null) {
            Log.e("size not parsed correctly");
        }
        if (getType() == null || getType() == CacheType.UNKNOWN) {
            Log.e("type not parsed correctly");
        }
        if (getCoords() == null) {
            Log.e("coordinates not parsed correctly");
        }
        if (StringUtils.isBlank(getLocation())) {
            Log.e("location not parsed correctly");
        }
    }

    public void refresh(int newListId, CancellableHandler handler) {
        cgData.removeCache(geocode, EnumSet.of(RemoveFlag.REMOVE_CACHE));
        storeCache(null, geocode, newListId, true, handler);
    }

    public static void storeCache(cgCache origCache, String geocode, int listId, boolean forceRedownload, CancellableHandler handler) {
        try {
            cgCache cache;
            // get cache details, they may not yet be complete
            if (origCache != null) {
                // only reload the cache if it was already stored or doesn't have full details (by checking the description)
                if (origCache.isOffline() || StringUtils.isBlank(origCache.getDescription())) {
                    final SearchResult search = searchByGeocode(origCache.getGeocode(), null, listId, false, handler);
                    cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                } else {
                    cache = origCache;
                }
            } else if (StringUtils.isNotBlank(geocode)) {
                final SearchResult search = searchByGeocode(geocode, null, listId, forceRedownload, handler);
                cache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            } else {
                cache = null;
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
                for (cgImage oneSpoiler : cache.getSpoilers()) {
                    imgGetter.getDrawable(oneSpoiler.getUrl());
                }
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            // store images from logs
            if (Settings.isStoreLogImages()) {
                for (LogEntry log : cache.getLogs()) {
                    if (log.hasLogImages()) {
                        for (cgImage oneLogImg : log.getLogImages()) {
                            imgGetter.getDrawable(oneLogImg.getUrl());
                        }
                    }
                }
            }

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            cache.setListId(listId);
            cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));

            if (CancellableHandler.isCancelled(handler)) {
                return;
            }

            StaticMapsProvider.downloadMaps(cache);

            if (handler != null) {
                handler.sendMessage(Message.obtain());
            }
        } catch (Exception e) {
            Log.e("cgBase.storeCache");
        }
    }

    public static SearchResult searchByGeocode(final String geocode, final String guid, final int listId, final boolean forceReload, final CancellableHandler handler) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            Log.e("cgCache.searchByGeocode: No geocode nor guid given");
            return null;
        }

        if (!forceReload && listId == StoredList.TEMPORARY_LIST_ID && (cgData.isOffline(geocode, guid) || cgData.isThere(geocode, guid, true, true))) {
            final SearchResult search = new SearchResult();
            final String realGeocode = StringUtils.isNotBlank(geocode) ? geocode : cgData.getGeocodeForGuid(guid);
            search.addGeocode(realGeocode);
            return search;
        }

        // if we have no geocode, we can't dynamically select the handler, but must explicitly use GC
        if (geocode == null && guid != null) {
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
        // 12:34
        final Pattern time = Pattern.compile("\\b(\\d{1,2})\\:(\\d\\d)\\b");
        final Matcher matcher = time.matcher(getDescription());
        while (matcher.find()) {
            try {
                final int hours = Integer.valueOf(matcher.group(1));
                final int minutes = Integer.valueOf(matcher.group(2));
                if (hours >= 0 && hours < 24 && minutes >= 0 && minutes < 60) {
                    return String.valueOf(hours * 60 + minutes);
                }
            } catch (NumberFormatException e) {
                // cannot happen, but static code analysis doesn't know
            }
        }
        // 12 o'clock
        final String hourLocalized = cgeoapplication.getInstance().getString(R.string.cache_time_full_hours);
        if (StringUtils.isNotBlank(hourLocalized)) {
            final Pattern fullHours = Pattern.compile("\\b(\\d{1,2})\\s+" + Pattern.quote(hourLocalized), Pattern.CASE_INSENSITIVE);
            final Matcher matcherHours = fullHours.matcher(getDescription());
            if (matcherHours.find()) {
                try {
                    final int hours = Integer.valueOf(matcherHours.group(1));
                    if (hours >= 0 && hours < 24) {
                        return String.valueOf(hours * 60);
                    }
                } catch (NumberFormatException e) {
                    // cannot happen, but static code analysis doesn't know
                }
            }
        }
        return null;
    }

    /**
     * check whether the cache has a given attribute
     *
     * @param attribute
     * @param yes
     *            true if we are looking for the attribute_yes version, false for the attribute_no version
     * @return
     */
    public boolean hasAttribute(CacheAttribute attribute, boolean yes) {
        cgCache fullCache = cgData.loadCache(getGeocode(), EnumSet.of(LoadFlag.LOAD_ATTRIBUTES));
        if (fullCache == null) {
            fullCache = this;
        }
        return fullCache.getAttributes().contains(attribute.getAttributeName(yes));
    }

    public boolean hasStaticMap() {
        return StaticMapsProvider.hasStaticMap(this);
    }

    public List<cgImage> getImages() {
        List<cgImage> result = new ArrayList<cgImage>();
        result.addAll(getSpoilers());
        for (LogEntry log : getLogs()) {
            result.addAll(log.getLogImages());
        }
        return result;
    }
}
