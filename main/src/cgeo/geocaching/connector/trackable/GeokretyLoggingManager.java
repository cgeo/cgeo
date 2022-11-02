package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.AbstractLoggingActivity;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.TrackableLog;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

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
            LastTrackableAction.setAction(trackableLog);
            final ImmutablePair<StatusCode, List<String>> response = GeokretyConnector.postLogTrackable(
                    getContext(),
                    cache,
                    trackableLog,
                    date,
                    log);

            final String logs = response.getRight().isEmpty() ? "" : StringUtils.join(response.getRight(), "\n");
            return new LogResult(response.getLeft(), logs);
        } catch (final Exception e) {
            Log.e("GeokretyLoggingManager.postLog", e);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @Override
    @NonNull
    public List<LogTypeTrackable> getPossibleLogTypesTrackable() {
        final List<LogTypeTrackable> list = new ArrayList<>();
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
    public void setGuid(final String guid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTrackingCodeNeededToPostNote() {
        return true;
    }

    @Override
    public boolean postReady() {
        return true;
    }
}
