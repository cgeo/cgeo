package cgeo.geocaching.connector.su;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AbstractConnector;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

public class GeocachingSuConnector extends AbstractConnector implements ISearchByCenter, ISearchByGeocode, ISearchByViewPort {

    static final CharSequence PREFIX_MULTISTEP_VIRTUAL = "MV";
    static final CharSequence PREFIX_TRADITIONAL = "TR";
    static final CharSequence PREFIX_VIRTUAL = "VI";
    static final CharSequence PREFIX_MULTISTEP = "MS";
    static final CharSequence PREFIX_EVENT = "EV";
    static final CharSequence PREFIX_CONTEST = "CT";

    /**
     * base URL for all API operations
     */
    private static final String API_URL = "http://www.geocaching.su/site/api.php?";

    /**
     * kind of request to the server
     */
    private static final String PARAMETER_REQUEST_TYPE = "rtype";
    private static final String REQUEST_TYPE_BOUNDING_BOX = "0";
    private static final String REQUEST_TYPE_CENTER = "1";
    private static final String REQUEST_TYPE_CACHE = "2";

    /**
     * level of detail for the result to be returned
     */
    private static final String PARAMETER_RESULT_FIELDS = "istr";
    private static final String RESULT_FIELDS_SEARCH = "ms";
    private static final String RESULT_FIELDS_DETAILED = "abcdefghiklmnops";

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
        return StringUtils.startsWithAny(StringUtils.upperCase(geocode), PREFIX_TRADITIONAL, PREFIX_MULTISTEP_VIRTUAL, PREFIX_VIRTUAL, PREFIX_MULTISTEP, PREFIX_EVENT, PREFIX_CONTEST) && isNumericId(geocode.substring(2));
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
    public SearchResult searchByCenter(@NonNull final Geopoint center) {
        return searchCaches("cache", new Parameters(PARAMETER_REQUEST_TYPE, REQUEST_TYPE_CENTER, "radius", "40", "clng", GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, center), "clat", GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, center), PARAMETER_RESULT_FIELDS, RESULT_FIELDS_SEARCH));
    }

    @Override
    public SearchResult searchByGeocode(@Nullable final String geocode, @Nullable final String guid, final CancellableHandler handler) {
        final String id = StringUtils.substring(geocode, 2);
        return searchCaches("data", new Parameters(PARAMETER_REQUEST_TYPE, REQUEST_TYPE_CACHE, "cid", id, PARAMETER_RESULT_FIELDS, RESULT_FIELDS_DETAILED));
    }

    @Override
    @NonNull
    public SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final MapTokens tokens) {
        final Geopoint min = viewport.bottomLeft;
        final Geopoint max = viewport.topRight;
        return searchCaches("cache", new Parameters(PARAMETER_REQUEST_TYPE, REQUEST_TYPE_BOUNDING_BOX, "lngmax", GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, max), "lngmin", GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, min), "latmax", GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, max), "latmin", GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, min), PARAMETER_RESULT_FIELDS, RESULT_FIELDS_SEARCH));
    }

    private static SearchResult searchCaches(@NonNull final String endTag, @NonNull final Parameters parameters) {
        return parseCaches(endTag, Network.getResponseStream(Network.getRequest(API_URL, parameters)));
    }

    private static SearchResult parseCaches(@NonNull final String endTag, final InputStream inputStream) {
        return GeocachingSuParser.parseCaches(endTag, inputStream);
    }

}
