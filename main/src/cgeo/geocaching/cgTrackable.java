package cgeo.geocaching;

import org.apache.commons.lang3.StringUtils;

import android.text.Html;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class cgTrackable implements ILogable {
    static final public int SPOTTED_UNSET = 0;
    static final public int SPOTTED_CACHE = 1;
    static final public int SPOTTED_USER = 2;
    static final public int SPOTTED_UNKNOWN = 3;
    static final public int SPOTTED_OWNER = 4;

    private String error = "";
    private String guid = "";
    private String geocode = "";
    private String iconUrl = "";
    private String name = "";
    private String type = null;
    private Date released = null;
    private Float distance = null;
    private String origin = null;
    private String owner = null;
    private String ownerGuid = null;
    private String spottedName = null;
    private int spottedType = SPOTTED_UNSET;
    private String spottedGuid = null;
    private String goal = null;
    private String details = null;
    private String image = null;
    private List<cgLog> logs = new ArrayList<cgLog>();

    public String getUrl() {
        if (StringUtils.startsWithIgnoreCase(geocode, "GK")) {
            String hex = geocode.substring(3);
            try {
                int id = Integer.parseInt(hex, 16);
                return "http://geokrety.org/konkret.php?id=" + id;
            } catch (NumberFormatException e) {
                Log.e(Settings.tag, "cgTrackable.getUrl", e);
                return null;
            }
        }
        return "http://coord.info/" + geocode.toUpperCase();
    }

    public String getError() {
        return error;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(String geocode) {
        this.geocode = geocode;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getReleased() {
        return released;
    }

    public void setReleased(Date released) {
        if (released == null) {
            this.released = null;
        }
        else {
            this.released = new Date(released.getTime()); // avoid storing external reference in this object
        }
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerGuid() {
        return ownerGuid;
    }

    public void setOwnerGuid(String ownerGuid) {
        this.ownerGuid = ownerGuid;
    }

    public String getSpottedName() {
        return spottedName;
    }

    public void setSpottedName(String spottedName) {
        this.spottedName = spottedName;
    }

    public int getSpottedType() {
        return spottedType;
    }

    public void setSpottedType(int spottedType) {
        this.spottedType = spottedType;
    }

    public String getSpottedGuid() {
        return spottedGuid;
    }

    public void setSpottedGuid(String spottedGuid) {
        this.spottedGuid = spottedGuid;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<cgLog> getLogs() {
        return logs;
    }

    public void setLogs(List<cgLog> logs) {
        this.logs = logs;
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
        if (StringUtils.startsWithIgnoreCase(geocode, "GK")) {
            return false;
        }
        return true;
    }
}
