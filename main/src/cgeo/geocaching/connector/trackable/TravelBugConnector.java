package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.AbstractLoggingActivity;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.Loaders;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public class TravelBugConnector extends AbstractTrackableConnector {

    /**
     * TB codes really start with TB1, there is no padding or minimum length
     */
    private final static Pattern PATTERN_TB_CODE = Pattern.compile("(TB[0-9A-Z]+)|([0-9A-Z]{6})", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandleTrackable(final String geocode) {
        return PATTERN_TB_CODE.matcher(geocode).matches() && !StringUtils.startsWithIgnoreCase(geocode, "GC");
    }

    @Override
    @NonNull
    public String getUrl(@NonNull final Trackable trackable) {
        return "http://www.geocaching.com//track/details.aspx?tracker=" + trackable.getGeocode();
    }

    static String getTravelbugViewstates(final String guid) {
        return Network.getResponseData(Network.getRequest("http://www.geocaching.com/track/log.aspx", new Parameters("wid", guid)));
    }

    @Override
    public boolean isLoggable() {
        return true;
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
    public @Nullable
    String getTrackableCodeFromUrl(@NonNull final String url) {
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
    public List<UserAction> getUserActions() {
        // travel bugs should have the same actions as GC caches
        return GCConnector.getInstance().getUserActions();
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
}