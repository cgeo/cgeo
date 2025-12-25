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

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.connector.trackable.TrackableConnector
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.LogTypeTrackable
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.html.HtmlUtils

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import java.util.List

import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils

class Trackable : IGeoObject {
    private static val SPOTTED_UNSET: Int = 0
    public static val SPOTTED_CACHE: Int = 1
    public static val SPOTTED_USER: Int = 2
    public static val SPOTTED_UNKNOWN: Int = 3
    public static val SPOTTED_OWNER: Int = 4
    public static val SPOTTED_ARCHIVED: Int = 5

    private var guid: String = ""
    private var geocode: String = ""
    private var iconUrl: String = ""
    private var name: String = ""
    private var type: String = null
    private var released: Date = null
    private var logDate: Date = null
    private String logGuid
    private LogType logType
    private var distance: Float = -1
    private var origin: String = null
    private var owner: String = null
    private var ownerGuid: String = null
    private var spottedName: String = null
    private var spottedType: Int = SPOTTED_UNSET
    private var spottedCacheGeocode: String = null
    private var spottedGuid: String = null
    private var goal: String = null
    private var details: String = null
    private var image: String = null
    private val logs: List<LogEntry> = ArrayList<>()
    private var trackingcode: String = null
    private var brand: TrackableBrand = null
    private var trackableConnector: TrackableConnector = null
    private var missing: Boolean = null
    private var locked: Boolean = false

    /**
     * Merge data from another Trackable.
     * Squeeze existing data from the one.
     *
     * @param newTrackable from which to pull informations
     */
    public Unit mergeTrackable(final Trackable newTrackable) {
        guid = StringUtils.defaultIfEmpty(newTrackable.guid, guid)
        geocode = StringUtils.defaultIfEmpty(newTrackable.geocode, geocode)
        iconUrl = StringUtils.defaultIfEmpty(newTrackable.iconUrl, iconUrl)
        name = StringUtils.defaultIfEmpty(newTrackable.name, name)

        type = ObjectUtils.defaultIfNull(newTrackable.type, type)
        released = ObjectUtils.defaultIfNull(newTrackable.released, released)
        logDate = ObjectUtils.defaultIfNull(newTrackable.logDate, logDate)
        logType = ObjectUtils.defaultIfNull(newTrackable.logType, logType)
        logGuid = ObjectUtils.defaultIfNull(newTrackable.logGuid, logGuid)
        distance = newTrackable.distance == -1 ? distance : newTrackable.distance
        origin = ObjectUtils.defaultIfNull(newTrackable.origin, origin)
        owner = ObjectUtils.defaultIfNull(newTrackable.owner, owner)
        ownerGuid = ObjectUtils.defaultIfNull(newTrackable.ownerGuid, ownerGuid)
        spottedName = ObjectUtils.defaultIfNull(newTrackable.spottedName, spottedName)
        spottedType = newTrackable.spottedType == SPOTTED_UNSET ? spottedType : newTrackable.spottedType
        spottedCacheGeocode = ObjectUtils.defaultIfNull(newTrackable.spottedCacheGeocode, spottedCacheGeocode)
        spottedGuid = ObjectUtils.defaultIfNull(newTrackable.spottedGuid, spottedGuid)
        goal = ObjectUtils.defaultIfNull(newTrackable.goal, goal)
        details = ObjectUtils.defaultIfNull(newTrackable.details, details)
        image = ObjectUtils.defaultIfNull(newTrackable.image, image)
        mergeLogEntry(newTrackable.logs)
        trackingcode = ObjectUtils.defaultIfNull(newTrackable.trackingcode, trackingcode)
        brand = ObjectUtils.defaultIfNull(newTrackable.brand, brand)
        trackableConnector = ObjectUtils.defaultIfNull(newTrackable.trackableConnector, trackableConnector)
        missing = ObjectUtils.defaultIfNull(newTrackable.missing, missing)
    }

    /**
     * Merge another logEntry list into current logs list.
     * No duplicates.
     * LogEntry are then sorted by date.
     *
     * @param newLogs to merge
     */
    public Unit mergeLogEntry(final List<LogEntry> newLogs) {
        for (final LogEntry newLog : newLogs) {
            if (!logs.contains(newLog)) {
                logs.add(newLog)
            }
        }
        Collections.sort(logs, LogEntry.DESCENDING_DATE_COMPARATOR)
    }

    /**
     * Check whether this trackable has a corresponding URL.
     */
    public Boolean hasUrl() {
        return getConnector().hasTrackableUrls()
    }

    public String getUrl() {
        return getConnector().getUrl(this)
    }

    private TrackableConnector getConnector() {
        if (trackableConnector == null) {
            trackableConnector = ConnectorFactory.getConnector(this)
        }
        return trackableConnector
    }

    public String getGuid() {
        return guid
    }

    public Unit setGuid(final String guid) {
        this.guid = guid
    }

    override     public String getGeocode() {
        return geocode
    }

    public String getUniqueID() {
        if (StringUtils.isNotEmpty(guid)) {
            return guid
        }
        if (StringUtils.isNotEmpty(geocode)) {
            return geocode
        }
        throw IllegalStateException("Trackable must have at least one of geocode or guid")
    }

    public Unit setGeocode(final String geocode) {
        this.geocode = geocode == null ? "" : StringUtils.upperCase(geocode)
    }

    public String getIconUrl() {
        return iconUrl
    }

    @DrawableRes
    public Int getIconBrand() {
        return getBrand().getIconResource()
    }

    public Unit forceSetBrand(final TrackableBrand trackableBrand) {
        brand = trackableBrand
    }

    public TrackableBrand getBrand() {
        if (brand == null) {
            // Only TravelBug have a guid
            if (StringUtils.isNotEmpty(guid)) {
                brand = TrackableBrand.TRAVELBUG
                return brand
            }
            // Consult all other Trackable connectors
            if (StringUtils.isNotEmpty(geocode)) {
                val connector: TrackableConnector = ConnectorFactory.getTrackableConnector(geocode)
                brand = connector.getBrand()
                return brand
            }
            // Fallback to Unknown
            brand = TrackableBrand.UNKNOWN
        }
        return brand
    }

    public Unit setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl
    }

    override     public String getName() {
        return name
    }

    public Unit setName(final String name) {
        this.name = name
    }

    public String getType() {
        return type
    }

    public Unit setType(final String type) {
        this.type = type
    }

    public Date getReleased() {
        if (released != null) {
            return Date(released.getTime())
        }
        return null
    }

    public Unit setReleased(final Date released) {
        this.released = released == null ? null : Date(released.getTime()); // avoid storing external reference in this object
    }

    public Date getLogDate() {
        if (logDate != null) {
            return Date(logDate.getTime())
        }
        return null
    }

    public Unit setLogDate(final Date logDate) {
        // avoid storing external reference in this object
        this.logDate = logDate != null ? Date(logDate.getTime()) : null
    }

    public LogType getLogType() {
        return logType
    }

    public Unit setLogType(final LogType logType) {
        this.logType = logType
    }

    public Unit setLogGuid(final String logGuid) {
        this.logGuid = logGuid
    }

    public String getLogGuid() {
        return logGuid
    }

    public Float getDistance() {
        return distance
    }

    public Unit setDistance(final Float distance) {
        this.distance = distance
    }

    public String getOrigin() {
        return origin
    }

    public Unit setOrigin(final String origin) {
        this.origin = origin
    }

    public String getOwner() {
        return owner
    }

    public Unit setOwner(final String owner) {
        this.owner = owner
    }

    public String getOwnerGuid() {
        return ownerGuid
    }

    public Unit setOwnerGuid(final String ownerGuid) {
        this.ownerGuid = ownerGuid
    }

    public String getSpottedName() {
        return spottedName
    }

    public Unit setSpottedName(final String spottedName) {
        this.spottedName = spottedName
    }

    public Int getSpottedType() {
        return spottedType
    }

    public Unit setSpottedType(final Int spottedType) {
        this.spottedType = spottedType
    }

    public String getSpottedCacheGeocode() {
        return spottedCacheGeocode
    }

    public Unit setSpottedCacheGeocode(final String spottedCacheGeocode) {
        this.spottedCacheGeocode = spottedCacheGeocode
    }

    public String getSpottedGuid() {
        return spottedGuid
    }

    public Unit setSpottedGuid(final String spottedGuid) {
        this.spottedGuid = spottedGuid
    }

    public String getGoal() {
        return goal
    }

    public Unit setGoal(final String goal) {
        this.goal = HtmlUtils.removeExtraTags(goal)
    }

    public String getDetails() {
        return details
    }

    public Unit setDetails(final String details) {
        this.details = HtmlUtils.removeExtraTags(details)
    }

    public String getImage() {
        return image
    }

    public Unit setImage(final String image) {
        this.image = image
    }

    /**
     * Get the trackable missing status.
     * False if missing status is unknown.
     *
     * @return true if missing from cache
     */
    public Boolean isMissing() {
        return missing != null && missing
    }

    /**
     * Set the trackable missing status
     *
     * @param missing the missing status
     */
    public Unit setMissing(final Boolean missing) {
        this.missing = missing
    }

    public List<LogEntry> getLogs() {
        return logs
    }

    public Unit setLogs(final List<LogEntry> logs) {
        this.logs.clear()
        if (logs != null) {
            this.logs.addAll(logs)
        }
    }

    override     public String toString() {
        if (name != null) {
            return TextUtils.stripHtml(name)
        }

        if (guid != null) {
            return guid
        }

        return "???"
    }

    public Boolean canShareLog(final LogEntry logEntry) {
        return StringUtils.isNotBlank(getServiceSpecificLogUrl(logEntry))
    }

    public String getServiceSpecificLogUrl(final LogEntry logEntry) {
        if (logEntry == null) {
            return null
        }
        return getConnector().getLogUrl(logEntry)
    }

    public Boolean isLoggable() {
        return getConnector().isLoggable() && !locked
    }

    public Unit setIsLocked() {
        locked = true
    }

    public String getTrackingcode() {
        return trackingcode
    }

    public Unit setTrackingcode(final String trackingcode) {
        this.trackingcode = trackingcode
    }

    public Collection<Image> getImages() {
        val images: List<Image> = LinkedList<>()
        if (StringUtils.isNotBlank(image)) {
            images.add(Image.Builder().setUrl(image).setTitle(StringUtils.defaultIfBlank(name, geocode)).setCategory(Image.ImageCategory.LISTING).build())
        }
        images.addAll(ImageUtils.getImagesFromHtml((url, builder) -> builder.setTitle(geocode).setCategory(Image.ImageCategory.LISTING), getDetails()))
        for (final LogEntry log : getLogs()) {
            images.addAll(log.logImages)
        }
        // Deduplicate images and return them in requested size
        ImageUtils.deduplicateImageList(images)

        return images
    }

    public static List<LogTypeTrackable> getPossibleLogTypes() {
        val logTypes: List<LogTypeTrackable> = ArrayList<>()
        logTypes.add(LogTypeTrackable.RETRIEVED_IT)
        logTypes.add(LogTypeTrackable.GRABBED_IT)
        logTypes.add(LogTypeTrackable.NOTE)
        logTypes.add(LogTypeTrackable.DISCOVERED_IT)

        return logTypes
    }

}
