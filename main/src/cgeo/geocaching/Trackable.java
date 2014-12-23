package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.ImageUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.text.Html;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Trackable implements ILogable {
    static final private int SPOTTED_UNSET = 0;
    static final public int SPOTTED_CACHE = 1;
    static final public int SPOTTED_USER = 2;
    static final public int SPOTTED_UNKNOWN = 3;
    static final public int SPOTTED_OWNER = 4;

    private String guid = "";
    private String geocode = "";
    private String iconUrl = "";
    private String name = "";
    private String type = null;
    private Date released = null;
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
    private List<LogEntry> logs = new ArrayList<>();
    private String trackingcode = null;

    public String getUrl() {
        return getConnector().getUrl(this);
    }

    private TrackableConnector getConnector() {
        return ConnectorFactory.getConnector(this);
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

    public void setGeocode(final String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
    }

    public String getIconUrl() {
        return iconUrl;
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

    public Date getReleased() {
        return released;
    }

    public void setReleased(final Date released) {
        if (released == null) {
            this.released = null;
        }
        else {
            this.released = new Date(released.getTime()); // avoid storing external reference in this object
        }
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
        this.goal = goal;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public String getImage() {
        return image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    @NonNull
    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(final List<LogEntry> logs) {
        this.logs = logs != null ? logs : new ArrayList<LogEntry>();
    }

    @Override
    public String toString() {
        if (null != name) {
            return Html.fromHtml(name).toString();
        }

        if (guid != null) {
            return guid;
        }

        return "???";
    }

    public boolean isLoggable() {
        return getConnector().isLoggable();
    }

    public String getTrackingcode() {
        return trackingcode;
    }

    public void setTrackingcode(final String trackingcode) {
        this.trackingcode = trackingcode;
    }

    public Collection<Image> getImages() {
        final List<Image> images = new LinkedList<>();
        if (StringUtils.isNotBlank(image)) {
            images.add(new Image(image, StringUtils.defaultIfBlank(name, geocode)));
        }
        ImageUtils.addImagesFromHtml(images, getDetails(), geocode);
        for (final LogEntry log : getLogs()) {
            images.addAll(log.getLogImages());
        }
        return images;
    }

    static List<LogType> getPossibleLogTypes() {
        final List<LogType> logTypes = new ArrayList<>();
        logTypes.add(LogType.RETRIEVED_IT);
        logTypes.add(LogType.GRABBED_IT);
        logTypes.add(LogType.NOTE);
        logTypes.add(LogType.DISCOVERED_IT);

        return logTypes;
    }
}
