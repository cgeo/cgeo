package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.LogTypeTrackable;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.net.Uri;

import java.util.Calendar;
import java.util.List;

public interface TrackableLoggingManager {

    /**
     * Post a log for a trackable online
     */
    LogResult postLog(Geocache cache,
                      TrackableLog trackableLog,
                      Calendar date,
                      String log);

    @Nullable
    ImageResult postLogImage(String logId,
                             String imageCaption,
                             String imageDescription,
                             Uri imageUri);

    public boolean hasLoaderError();

    @NonNull
    public List<LogTypeTrackable> getPossibleLogTypesTrackable();

    public boolean canLogTime();

    public boolean canLogCoordinates();

    public void setGuid(final String guid);

    public boolean isRegistered();

    public boolean postReady();
}
