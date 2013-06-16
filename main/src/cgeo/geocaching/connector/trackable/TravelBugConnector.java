package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import java.util.regex.Pattern;

public class TravelBugConnector extends AbstractTrackableConnector {

    /**
     * TB codes really start with TB1, there is no padding or minimum length
     */
    private final static Pattern PATTERN_TB_CODE = Pattern.compile("TB[0-9A-Z]+", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandleTrackable(String geocode) {
        return TravelBugConnector.PATTERN_TB_CODE.matcher(geocode).matches();
    }

    @Override
    public String getUrl(Trackable trackable) {
        return "http://www.geocaching.com//track/details.aspx?tracker=" + trackable.getGeocode();
    }

    @Override
    public boolean isLoggable() {
        return true;
    }
}