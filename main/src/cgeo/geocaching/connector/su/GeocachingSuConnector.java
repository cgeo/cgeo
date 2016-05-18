package cgeo.geocaching.connector.su;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.io.InputStream;

public class GeocachingSuConnector extends AbstractConnector implements ISearchByCenter, ISearchByGeocode, ISearchByViewPort {

    private static final CharSequence PREFIX_MULTISTEP_VIRTUAL = "MV";
    private static final CharSequence PREFIX_TRADITIONAL = "TR";
    private static final CharSequence PREFIX_VIRTUAL = "VI";
    private static final CharSequence PREFIX_MULTISTEP = "MS";
    private static final CharSequence PREFIX_EVENT = "EV";
    private static final CharSequence PREFIX_CONTEST = "CT";
    private static final String API_URL = "http://www.geocaching.su/site/api.php?";

    private GeocachingSuConnector() {
        // singleton
    }

    /**
     * initialization on demand holder pattern
     */
    private static class Holder {
        @NonNull private static final GeocachingSuConnector INSTANCE = new GeocachingSuConnector();
    }

    public static GeocachingSuConnector getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    @NonNull
    public String getName() {
        return "Geocaching.su";
    }

    @Override
    @NonNull
    public String getCacheUrl(@NonNull final Geocache cache) {
        return getCacheUrlPrefix() + "&cid=" + getCacheId(cache.getGeocode());
    }

    private static String getCacheId(final String geocode) {
        if (Character.isDigit(geocode.charAt(0))) {
            return geocode;
        }
        return StringUtils.substring(geocode, 2);
    }

    @Override
    @NonNull
    public String getHost() {
        return "www.geocaching.su";
    }

    @Override
    public boolean isOwner(@NonNull final Geocache cache) {
        return false;
    }

    @Override
    public boolean canHandle(@NonNull final String geocode) {
        return (StringUtils.startsWithAny(StringUtils.upperCase(geocode), "SU", PREFIX_TRADITIONAL, PREFIX_MULTISTEP_VIRTUAL, PREFIX_VIRTUAL, PREFIX_MULTISTEP, PREFIX_EVENT, PREFIX_CONTEST)) && isNumericId(geocode.substring(2));
    }

    @Override
    @NonNull
    protected String getCacheUrlPrefix() {
        return getHostUrl() + "/?pn=101";
    }

    @Override
    public boolean isActive() {
        return Settings.isSUConnectorActive();
    }

    @Override
    public SearchResult searchByCenter(@NonNull final Geopoint center, @NonNull final RecaptchaReceiver recaptchaReceiver) {
        return searchCaches("cache", "rtype=1&radius=20&clng=" + center.getLongitude() + "&clat=" + center.getLatitude());
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final CancellableHandler handler) {
        final String id = StringUtils.substring(geocode, 2);
        return searchCaches("data", "rtype=2&cid=" + id + "&istr=abcdefghiklmnos");
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final MapTokens tokens) {
        return searchCaches("cache", "rtype=0&lngmax=" + viewport.getLongitudeMax() + "&lngmin=" + viewport.getLongitudeMin() + "&latmax=" + viewport.getLatitudeMax() + "&latmin=" + viewport.getLatitudeMin());
    }

    private static SearchResult searchCaches(@NonNull final String endTag, @NonNull final String url) {
        return parseCaches(endTag, Network.getResponseStream(Network.getRequest(API_URL + url)));
    }

    private static SearchResult parseCaches(@NonNull final String endTag, final InputStream inputStream) {
        return GeocachingSuParser.parseCaches(endTag, inputStream);
    }

}
