package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

/**
 * Methods to be implemented by any connector for handling trackables
 *
 */
public interface TrackableConnector {

    public boolean canHandleTrackable(final String geocode);

    public String getUrl(final Trackable trackable);

    public boolean isLoggable();

}
