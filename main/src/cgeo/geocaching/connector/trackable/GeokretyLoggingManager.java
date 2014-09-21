package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.AbstractLoggingActivity;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogTypeTrackable;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.tuple.ImmutablePair;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class GeokretyLoggingManager extends AbstractTrackableLoggingManager {

    public GeokretyLoggingManager(final AbstractLoggingActivity activity) {
        super(activity);
    }

    @Override
    public List<LogTypeTrackable> loadInBackground() {
        return getPossibleLogTypesTrackable();
    }

    @Override
    public LogResult postLog(final Geocache cache, final TrackableLog trackableLog, final Calendar date, final String log) {
        try {
            final ImmutablePair<StatusCode, ArrayList<String>> response = GeokretyConnector.postLogTrackable(
                    cache,
                    trackableLog,
                    date,
                    log);

            final String logs = (response.getRight().isEmpty() ? "" : response.getRight().toString());
            return new LogResult(response.getLeft(), logs);
        } catch (final Exception e) {
            Log.e("GeokretyLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    public ImageResult postLogImage(final String logId, final String imageCaption, final String imageDescription, final Uri imageUri) {
        // No support for images
        return null;
    }

    @Override
    public ArrayList<LogTypeTrackable> getPossibleLogTypesTrackable() {
        final ArrayList<LogTypeTrackable> list = new ArrayList<>();
        list.add(LogTypeTrackable.RETRIEVED_IT);
        list.add(LogTypeTrackable.DISCOVERED_IT);
        list.add(LogTypeTrackable.DROPPED_OFF);
        list.add(LogTypeTrackable.VISITED);
        list.add(LogTypeTrackable.NOTE);
        return list;
    }

    @Override
    public boolean canLogTime() {
        return true;
    }

    @Override
    public boolean canLogCoordinates() {
        return true;
    }

    @Override
    public boolean isRegistered() {
        return Settings.isRegisteredForGeokrety();
    }

    @Override
    public boolean postReady() {
        return true;
    }
}