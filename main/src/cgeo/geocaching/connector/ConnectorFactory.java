package cgeo.geocaching.connector;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByNextPage;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.connector.oc.OCApiConnector.ApiSupport;
import cgeo.geocaching.connector.oc.OCApiLiveConnector;
import cgeo.geocaching.connector.oc.OCCZConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.connector.ox.OXConnector;
import cgeo.geocaching.connector.trackable.GeokretyConnector;
import cgeo.geocaching.connector.trackable.SwaggieConnector;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.connector.trackable.UnknownTrackableConnector;
import cgeo.geocaching.location.Viewport;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ConnectorFactory {
    @NonNull private static final UnknownConnector UNKNOWN_CONNECTOR = new UnknownConnector();
    @NonNull private static final Collection<IConnector> CONNECTORS = Collections.unmodifiableCollection(Arrays.<IConnector> asList(
            GCConnector.getInstance(),
            ECConnector.getInstance(),
            new OCApiLiveConnector("opencaching.de", "www.opencaching.de", "OC", "CC BY-NC-ND, alle Logeinträge © jeweiliger Autor",
                    R.string.oc_de_okapi_consumer_key, R.string.oc_de_okapi_consumer_secret,
                    R.string.pref_connectorOCActive, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, ApiSupport.current),
            new OCCZConnector(),
            new OCApiLiveConnector("opencaching.org.uk", "www.opencaching.org.uk", "OK", "CC BY-NC-SA 2.5",
                    R.string.oc_uk_okapi_consumer_key, R.string.oc_uk_okapi_consumer_secret,
                    R.string.pref_connectorOCUKActive, R.string.pref_ocuk_tokenpublic, R.string.pref_ocuk_tokensecret, ApiSupport.oldapi),
            new OCConnector("OpenCaching.ES", "www.opencachingspain.es", "OC"),
            new OCConnector("OpenCaching.IT", "www.opencaching.it", "OC"),
            new OCConnector("OpenCaching.JP", "www.opencaching.jp", "OJ"),
            new OCConnector("OpenCaching.NO/SE", "www.opencaching.se", "OS"),
            new OCApiLiveConnector("opencaching.nl", "www.opencaching.nl", "OB", "CC BY-SA 3.0",
                    R.string.oc_nl_okapi_consumer_key, R.string.oc_nl_okapi_consumer_secret,
                    R.string.pref_connectorOCNLActive, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret, ApiSupport.current),
            new OCApiLiveConnector("opencaching.pl", "www.opencaching.pl", "OP", "CC BY-SA 3.0",
                    R.string.oc_pl_okapi_consumer_key, R.string.oc_pl_okapi_consumer_secret,
                    R.string.pref_connectorOCPLActive, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, ApiSupport.current),
            new OCApiLiveConnector("opencaching.us", "www.opencaching.us", "OU", "CC BY-NC-SA 2.5",
                    R.string.oc_us_okapi_consumer_key, R.string.oc_us_okapi_consumer_secret,
                    R.string.pref_connectorOCUSActive, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret, ApiSupport.current),
            new OCApiLiveConnector("opencaching.ro", "www.opencaching.ro", "OR", "CC BY-SA 3.0",
                    R.string.oc_ro_okapi_consumer_key, R.string.oc_ro_okapi_consumer_secret,
                    R.string.pref_connectorOCROActive, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret, ApiSupport.current),
            new OXConnector(),
            new GeocachingAustraliaConnector(),
            new GeopeitusConnector(),
            new TerraCachingConnector(),
            new WaymarkingConnector(),
            UNKNOWN_CONNECTOR // the unknown connector MUST be the last one
    ));

    @NonNull public static final UnknownTrackableConnector UNKNOWN_TRACKABLE_CONNECTOR = new UnknownTrackableConnector();

    @NonNull
    private static final Collection<TrackableConnector> TRACKABLE_CONNECTORS = Collections.unmodifiableCollection(Arrays.<TrackableConnector> asList(
            new GeokretyConnector(),
            new SwaggieConnector(),
            TravelBugConnector.getInstance(), // travel bugs last, as their secret codes overlap with other connectors
            UNKNOWN_TRACKABLE_CONNECTOR // must be last
    ));

    @NonNull
    private static final Collection<ISearchByViewPort> searchByViewPortConns = getMatchingConnectors(ISearchByViewPort.class);

    @NonNull
    private static final Collection<ISearchByCenter> searchByCenterConns = getMatchingConnectors(ISearchByCenter.class);

    @NonNull
    private static final Collection<ISearchByNextPage> searchByNextPageConns = getMatchingConnectors(ISearchByNextPage.class);

    @NonNull
    private static final Collection<ISearchByKeyword> searchByKeywordConns = getMatchingConnectors(ISearchByKeyword.class);

    @NonNull
    private static final Collection<ISearchByOwner> SEARCH_BY_OWNER_CONNECTORS = getMatchingConnectors(ISearchByOwner.class);

    @NonNull
    private static final Collection<ISearchByFinder> SEARCH_BY_FINDER_CONNECTORS = getMatchingConnectors(ISearchByFinder.class);

    @NonNull
    @SuppressWarnings("unchecked")
    private static <T extends IConnector> Collection<T> getMatchingConnectors(final Class<T> clazz) {
        final List<T> matching = new ArrayList<>();
        for (final IConnector connector : CONNECTORS) {
            if (clazz.isInstance(connector)) {
                matching.add((T) connector);
            }
        }
        return Collections.unmodifiableCollection(matching);
    }

    @NonNull
    public static Collection<IConnector> getConnectors() {
        return CONNECTORS;
    }

    @NonNull
    public static Collection<ISearchByCenter> getSearchByCenterConnectors() {
        return searchByCenterConns;
    }

    @NonNull
    public static Collection<ISearchByNextPage> getSearchByNextPageConnectors() {
        return searchByNextPageConns;
    }

    @NonNull
    public static Collection<ISearchByKeyword> getSearchByKeywordConnectors() {
        return searchByKeywordConns;
    }

    @NonNull
    public static Collection<ISearchByOwner> getSearchByOwnerConnectors() {
        return SEARCH_BY_OWNER_CONNECTORS;
    }

    @NonNull
    public static Collection<ISearchByFinder> getSearchByFinderConnectors() {
        return SEARCH_BY_FINDER_CONNECTORS;
    }

    @NonNull
    public static ILogin[] getActiveLiveConnectors() {
        final List<ILogin> liveConns = new ArrayList<>();
        for (final IConnector conn : CONNECTORS) {
            if (conn instanceof ILogin && conn.isActive()) {
                liveConns.add((ILogin) conn);
            }
        }
        return liveConns.toArray(new ILogin[liveConns.size()]);
    }

    public static boolean canHandle(final @Nullable String geocode) {
        if (geocode == null) {
            return false;
        }
        if (isInvalidGeocode(geocode)) {
            return false;
        }
        for (final IConnector connector : CONNECTORS) {
            if (connector.canHandle(geocode)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static IConnector getConnector(final Geocache cache) {
        return getConnector(cache.getGeocode());
    }

    @NonNull
    public static TrackableConnector getConnector(final Trackable trackable) {
        return getTrackableConnector(trackable.getGeocode());
    }

    @NonNull
    public static TrackableConnector getTrackableConnector(final String geocode) {
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            if (connector.canHandleTrackable(geocode)) {
                return connector;
            }
        }
        return UNKNOWN_TRACKABLE_CONNECTOR; // avoid null checks by returning a non implementing connector
    }

    public static List<TrackableConnector> getGenericTrackablesConnectors() {
        final List<TrackableConnector> trackableConnectors = new ArrayList<>();
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            if (connector.isGenericLoggable() && connector.isActive()) {
                trackableConnectors.add(connector);
            }
        }
        return trackableConnectors;
    }

    @NonNull
    public static IConnector getConnector(final String geocodeInput) {
        // this may come from user input
        final String geocode = StringUtils.trim(geocodeInput);
        if (geocode == null) {
            return UNKNOWN_CONNECTOR;
        }
        if (isInvalidGeocode(geocode)) {
            return UNKNOWN_CONNECTOR;
        }
        for (final IConnector connector : CONNECTORS) {
            if (connector.canHandle(geocode)) {
                return connector;
            }
        }
        // in case of errors, take UNKNOWN to avoid null checks everywhere
        return UNKNOWN_CONNECTOR;
    }

    /**
     * Obtain the connector by it's name.
     * If connector is not found, return UNKNOWN_CONNECTOR.
     *
     * @param connectorName
     *          connector name String
     * @return
     *          The connector matching name
     */
    @NonNull
    public static IConnector getConnectorByName(final String connectorName) {
        for (final IConnector connector : CONNECTORS) {
            if (StringUtils.equals(connectorName, connector.getName())) {
                return connector;
            }
        }
        // in case of errors, take UNKNOWN to avoid null checks everywhere
        return UNKNOWN_CONNECTOR;
    }

    private static boolean isInvalidGeocode(final String geocode) {
        return StringUtils.isBlank(geocode) || !Character.isLetterOrDigit(geocode.charAt(0));
    }

    /** @see ISearchByViewPort#searchByViewport */
    @NonNull
    public static SearchResult searchByViewport(final @NonNull Viewport viewport, @NonNull final MapTokens tokens) {
        return SearchResult.parallelCombineActive(searchByViewPortConns, new Func1<ISearchByViewPort, SearchResult>() {
            @Override
            public SearchResult call(final ISearchByViewPort connector) {
                return connector.searchByViewport(viewport, tokens);
            }
        });
    }

    @Nullable
    public static String getGeocodeFromURL(@Nullable final String url) {
        if (url == null) {
            return null;
        }
        for (final IConnector connector : CONNECTORS) {
            @Nullable final String geocode = connector.getGeocodeFromUrl(url);
            if (StringUtils.isNotBlank(geocode)) {
                return StringUtils.upperCase(geocode);
            }
        }
        return null;
    }

    @NonNull
    public static Collection<TrackableConnector> getTrackableConnectors() {
        return TRACKABLE_CONNECTORS;
    }

    /**
     * Get the geocode of a trackable from a URL.
     *
     * @return {@code null} if the URL cannot be decoded
     */
    @Nullable
    public static String getTrackableFromURL(final String url) {
        if (url == null) {
            return null;
        }
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            final String geocode = connector.getTrackableCodeFromUrl(url);
            if (StringUtils.isNotBlank(geocode)) {
                return geocode;
            }
        }
        return null;
    }

}
