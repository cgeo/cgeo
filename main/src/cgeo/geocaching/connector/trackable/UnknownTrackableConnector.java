package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import org.apache.commons.lang3.StringUtils;

public class UnknownTrackableConnector extends AbstractTrackableConnector {

    @Override
    public boolean canHandleTrackable(String geocode) {
        return false;
    }

    @Override
    public String getUrl(Trackable trackable) {
        return StringUtils.EMPTY;
    }

    @Override
    public Trackable searchTrackable(String geocode, String guid, String id) {
        return null;
    }

}
