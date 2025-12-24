package cgeo.geocaching.connector.al;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class ALSearchV4Query {
    @JsonProperty("Origin")
    Origin origin;
    @JsonProperty("RadiusInMeters")
    Integer radiusInMeters;
    @JsonProperty("RecentlyPublishedDays")
    Integer recentlyPublishedDays = null;
    @JsonProperty("Skip")
    Integer skip = 0;
    @JsonProperty("Take")
    Integer take;
    @JsonProperty("CompletionStatuses")
    List<Integer> completionStatuses = null;
    @JsonProperty("AdventureTypes")
    List<Integer> adventureTypes = null;
    @JsonProperty("MedianCompletionTimes")
    List<String> medianCompletionTimes = null;
    @JsonProperty("CallingUserPublicGuid")
    String callingUserPublicGuid;
    @JsonProperty("Themes")
    List<Integer> themes = null;

    static class Origin {
        @JsonProperty("Latitude")
        Double latitude;
        @JsonProperty("Longitude")
        Double longitude;
        @JsonProperty("Altitude")
        Double altitude;

        Origin(final Double latitude, final Double longitude, final Double altitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }
    }

    public void setRadiusInMeters(final Integer radiusInMeters) {
        this.radiusInMeters = radiusInMeters;
    }

    public void setRecentlyPublishedDays(final Integer recentlyPublishedDays) {
        this.recentlyPublishedDays = recentlyPublishedDays;
    }

    public void setTake(final Integer take) {
        this.take = take;
    }

    public void setSkip(final Integer skip) {
        this.skip = skip;
    }

    public void setOrigin(final Double latitude, final Double longitude, final Double altitude) {
        this.origin = new Origin(latitude, longitude, altitude);
    }

    public void setCompletionStatuses(final List<Integer> completionStatuses) {
        this.completionStatuses = completionStatuses;
    }

    public void setAdventureTypes(final List<Integer> adventureTypes) {
        this.adventureTypes = adventureTypes;
    }

    public void setMedianCompletionTimes(final List<String> medianCompletionTimes) {
        this.medianCompletionTimes = medianCompletionTimes;
    }

    public void setCallingUserPublicGuid(final String callingUserPublicGuid) {
        this.callingUserPublicGuid = callingUserPublicGuid;
    }

    public void setThemes(final List<Integer> themes) {
        this.themes = themes;
    }

}
