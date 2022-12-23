package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.log.AbstractLoggingActivity;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class TravelBugConnector extends AbstractTrackableConnector {

    /**
     * TB codes really start with TB1, there is no padding or minimum length
     */
    private static final Pattern PATTERN_TB_CODE = Pattern.compile("(TB[0-9A-Z&&[^ILOSU]]+)|([0-9A-Z]{6})|([0-9]{3}[A-Z]{6})|([0-9]{5})", Pattern.CASE_INSENSITIVE);

    @Override
    public int getPreferenceActivity() {
        return R.string.preference_screen_gc;
    }

    @Override
    public boolean canHandleTrackable(@Nullable final String geocode) {
        if (geocode == null) {
            return false;
        }
        return PATTERN_TB_CODE.matcher(geocode).matches();
    }

    @NonNull
    @Override
    public String getServiceTitle() {
        return CgeoApplication.getInstance().getString(R.string.settings_title_gc);
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        return getHostUrl() + "//track/details.aspx?tracker=" + trackable.getGeocode();
    }

    @Override
    public String getLogUrl(@NonNull final LogEntry logEntry) {
        if (StringUtils.isNotBlank(logEntry.serviceLogId)) {
            return "https://www.geocaching.com/track/log.aspx?LUID=" + logEntry.serviceLogId;
        }
        return null;
    }

    @WorkerThread
    static String getTravelbugViewstates(final String guid) {
        return Network.getResponseData(Network.getRequest("https://www.geocaching.com/track/log.aspx", new Parameters("wid", guid)));
    }

    @Override
    public boolean isLoggable() {
        return true;
    }

    @Override
    public boolean isRegistered() {
        return Settings.hasGCCredentials();
    }

    @Override
    @Nullable
    public Trackable searchTrackable(final String geocode, final String guid, final String id) {
        return GCParser.searchTrackable(geocode, guid, id);
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        private static final TravelBugConnector INSTANCE = new TravelBugConnector();
    }

    private TravelBugConnector() {
        // singleton
    }

    public static TravelBugConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    @Nullable
    public String getTrackableCodeFromUrl(@NonNull final String url) {
        // coord.info URLs
        final String code1 = StringUtils.substringAfterLast(url, "coord.info/");
        if (canHandleTrackable(code1)) {
            return code1;
        }
        final String code2 = StringUtils.substringAfterLast(url, "?tracker=");
        if (canHandleTrackable(code2)) {
            return code2;
        }
        return null;
    }

    @Override
    @NonNull
    public List<UserAction> getUserActions(final UserAction.UAContext user) {
        // travel bugs should have the same actions as GC caches
        return GCConnector.getInstance().getUserActions(user);
    }

    @Override
    @NonNull
    public TrackableBrand getBrand() {
        return TrackableBrand.TRAVELBUG;
    }

    @Override
    public int getTrackableLoggingManagerLoaderId() {
        return Loaders.LOGGING_TRAVELBUG.getLoaderId();
    }

    @Override
    public AbstractTrackableLoggingManager getTrackableLoggingManager(final AbstractLoggingActivity activity) {
        return new TravelBugLoggingManager(activity);
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geocaching.com";
    }

    @Override
    @NonNull
    public String getTestUrl() {
        return "https://" + getHost() + "/play";
    }
}
