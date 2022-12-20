package cgeo.geocaching.models;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.utils.HtmlUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class Trackable implements ILogable {
    private static final int SPOTTED_UNSET = 0;
    public static final int SPOTTED_CACHE = 1;
    public static final int SPOTTED_USER = 2;
    public static final int SPOTTED_UNKNOWN = 3;
    public static final int SPOTTED_OWNER = 4;
    public static final int SPOTTED_ARCHIVED = 5;

    private String guid = "";
    private String geocode = "";
    private String iconUrl = "";
    private String name = "";
    private String type = null;
    @Nullable
    private Date released = null;
    private Date logDate = null;
    private String logGuid;
    private LogType logType;
    private float distance = -1;
    private String origin = null;
    private String owner = null;
    private String ownerGuid = null;
    private String spottedName = null;
    private int spottedType = SPOTTED_UNSET;
    private String spottedGuid = null;
    private String goal = null;
    private String details = null;
    private String image = null;
    private final List<LogEntry> logs = new ArrayList<>();
    private String trackingcode = null;
    private TrackableBrand brand = null;
    private TrackableConnector trackableConnector = null;
    private Boolean missing = null;
    private boolean locked = false;

    /**
     * Merge data from another Trackable.
     * Squeeze existing data from the new one.
     *
     * @param newTrackable from which to pull informations
     */
    public void mergeTrackable(final Trackable newTrackable) {
        guid = StringUtils.defaultIfEmpty(newTrackable.guid, guid);
        geocode = StringUtils.defaultIfEmpty(newTrackable.geocode, geocode);
        iconUrl = StringUtils.defaultIfEmpty(newTrackable.iconUrl, iconUrl);
        name = StringUtils.defaultIfEmpty(newTrackable.name, name);

        type = ObjectUtils.defaultIfNull(newTrackable.type, type);
        released = ObjectUtils.defaultIfNull(newTrackable.released, released);
        logDate = ObjectUtils.defaultIfNull(newTrackable.logDate, logDate);
        logType = ObjectUtils.defaultIfNull(newTrackable.logType, logType);
        logGuid = ObjectUtils.defaultIfNull(newTrackable.logGuid, logGuid);
        distance = newTrackable.distance == -1 ? distance : newTrackable.distance;
        origin = ObjectUtils.defaultIfNull(newTrackable.origin, origin);
        owner = ObjectUtils.defaultIfNull(newTrackable.owner, owner);
        ownerGuid = ObjectUtils.defaultIfNull(newTrackable.ownerGuid, ownerGuid);
        spottedName = ObjectUtils.defaultIfNull(newTrackable.spottedName, spottedName);
        spottedType = newTrackable.spottedType == SPOTTED_UNSET ? spottedType : newTrackable.spottedType;
        spottedGuid = ObjectUtils.defaultIfNull(newTrackable.spottedGuid, spottedGuid);
        goal = ObjectUtils.defaultIfNull(newTrackable.goal, goal);
        details = ObjectUtils.defaultIfNull(newTrackable.details, details);
        image = ObjectUtils.defaultIfNull(newTrackable.image, image);
        mergeLogEntry(newTrackable.logs);
        trackingcode = ObjectUtils.defaultIfNull(newTrackable.trackingcode, trackingcode);
        brand = ObjectUtils.defaultIfNull(newTrackable.brand, brand);
        trackableConnector = ObjectUtils.defaultIfNull(newTrackable.trackableConnector, trackableConnector);
        missing = ObjectUtils.defaultIfNull(newTrackable.missing, missing);
    }

    /**
     * Merge another logEntry list into current logs list.
     * No duplicates.
     * LogEntry are then sorted by date.
     *
     * @param newLogs to merge
     */
    public void mergeLogEntry(final List<LogEntry> newLogs) {
        for (final LogEntry newLog : newLogs) {
            if (!logs.contains(newLog)) {
                logs.add(newLog);
            }
        }
        Collections.sort(logs, LogEntry.DESCENDING_DATE_COMPARATOR);
    }

    /**
     * Check whether this trackable has a corresponding URL.
     */
    public boolean hasUrl() {
        return getConnector().hasTrackableUrls();
    }

    @NonNull
    public String getUrl() {
        return getConnector().getUrl(this);
    }

    @NonNull
    private TrackableConnector getConnector() {
        if (trackableConnector == null) {
            trackableConnector = ConnectorFactory.getConnector(this);
        }
        return trackableConnector;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(final String guid) {
        this.guid = guid;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    public String getUniqueID() {
        if (StringUtils.isNotEmpty(guid)) {
            return guid;
        }
        if (StringUtils.isNotEmpty(geocode)) {
            return geocode;
        }
        throw new IllegalStateException("Trackable must have at least one of geocode or guid");
    }

    public void setGeocode(final String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
    }

    public String getIconUrl() {
        return iconUrl;
    }

    @DrawableRes
    public int getIconBrand() {
        return getBrand().getIconResource();
    }

    public void forceSetBrand(final TrackableBrand trackableBrand) {
        brand = trackableBrand;
    }

    public TrackableBrand getBrand() {
        if (brand == null) {
            // Only TravelBug have a guid
            if (StringUtils.isNotEmpty(guid)) {
                brand = TrackableBrand.TRAVELBUG;
                return brand;
            }
            // Consult all other Trackable connectors
            if (StringUtils.isNotEmpty(geocode)) {
                final TrackableConnector connector = ConnectorFactory.getTrackableConnector(geocode);
                brand = connector.getBrand();
                return brand;
            }
            // Fallback to Unknown
            brand = TrackableBrand.UNKNOWN;
        }
        return brand;
    }

    public void setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Nullable
    public Date getReleased() {
        if (released != null) {
            return new Date(released.getTime());
        }
        return null;
    }

    public void setReleased(@Nullable final Date released) {
        this.released = released == null ? null : new Date(released.getTime()); // avoid storing external reference in this object
    }

    @Nullable
    public Date getLogDate() {
        if (logDate != null) {
            return new Date(logDate.getTime());
        }
        return null;
    }

    public void setLogDate(@Nullable final Date logDate) {
        // avoid storing external reference in this object
        this.logDate = logDate != null ? new Date(logDate.getTime()) : null;
    }

    public LogType getLogType() {
        return logType;
    }

    public void setLogType(final LogType logType) {
        this.logType = logType;
    }

    public void setLogGuid(final String logGuid) {
        this.logGuid = logGuid;
    }

    public String getLogGuid() {
        return logGuid;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(final float distance) {
        this.distance = distance;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(final String origin) {
        this.origin = origin;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public String getOwnerGuid() {
        return ownerGuid;
    }

    public void setOwnerGuid(final String ownerGuid) {
        this.ownerGuid = ownerGuid;
    }

    public String getSpottedName() {
        return spottedName;
    }

    public void setSpottedName(final String spottedName) {
        this.spottedName = spottedName;
    }

    public int getSpottedType() {
        return spottedType;
    }

    public void setSpottedType(final int spottedType) {
        this.spottedType = spottedType;
    }

    public String getSpottedGuid() {
        return spottedGuid;
    }

    public void setSpottedGuid(final String spottedGuid) {
        this.spottedGuid = spottedGuid;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(final String goal) {
        this.goal = HtmlUtils.removeExtraTags(goal);
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = HtmlUtils.removeExtraTags(details);
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    /**
     * Get the trackable missing status.
     * False if missing status is unknown.
     *
     * @return true if missing from cache
     */
    public boolean isMissing() {
        return missing != null && missing;
    }

    /**
     * Set the trackable missing status
     *
     * @param missing the new missing status
     */
    public void setMissing(final Boolean missing) {
        this.missing = missing;
    }

    @NonNull
    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(final List<LogEntry> logs) {
        this.logs.clear();
        if (logs != null) {
            this.logs.addAll(logs);
        }
    }

    @Override
    @NonNull
    public String toString() {
        if (name != null) {
            return TextUtils.stripHtml(name);
        }

        if (guid != null) {
            return guid;
        }

        return "???";
    }

    public boolean canShareLog(final LogEntry logEntry) {
        return StringUtils.isNotBlank(getServiceSpecificLogUrl(logEntry));
    }

    public String getServiceSpecificLogUrl(final LogEntry logEntry) {
        if (logEntry == null) {
            return null;
        }
        return getConnector().getLogUrl(logEntry);
    }

    public boolean isLoggable() {
        return getConnector().isLoggable() && !locked;
    }

    public void setIsLocked() {
        locked = true;
    }

    public String getTrackingcode() {
        return trackingcode;
    }

    public void setTrackingcode(final String trackingcode) {
        this.trackingcode = trackingcode;
    }

    @NonNull
    public Collection<Image> getImages() {
        final List<Image> images = new LinkedList<>();
        if (StringUtils.isNotBlank(image)) {
            images.add(new Image.Builder().setUrl(image).setTitle(StringUtils.defaultIfBlank(name, geocode)).setCategory(Image.ImageCategory.LISTING).build());
        }
        ImageUtils.addImagesFromHtml(images, geocode, getDetails());
        for (final LogEntry log : getLogs()) {
            images.addAll(log.logImages);
        }
        return images;
    }

    @NonNull
    public static List<LogTypeTrackable> getPossibleLogTypes() {
        final List<LogTypeTrackable> logTypes = new ArrayList<>();
        logTypes.add(LogTypeTrackable.RETRIEVED_IT);
        logTypes.add(LogTypeTrackable.GRABBED_IT);
        logTypes.add(LogTypeTrackable.NOTE);
        logTypes.add(LogTypeTrackable.DISCOVERED_IT);

        return logTypes;
    }

}
