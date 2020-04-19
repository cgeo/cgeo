package cgeo.geocaching.connector;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByFinder;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByNextPage;
import cgeo.geocaching.connector.capability.ISearchByOwner;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.ga.GeocachingAustraliaConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.ge.GeopeitusConnector;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.connector.oc.OCApiConnector.ApiSupport;
import cgeo.geocaching.connector.oc.OCApiLiveConnector;
import cgeo.geocaching.connector.oc.OCCZConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.connector.oc.OCDEConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.connector.tc.TerraCachingConnector;
import cgeo.geocaching.connector.trackable.GeokretyConnector;
import cgeo.geocaching.connector.trackable.GeolutinsConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TrackableTrackingCode;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.connector.trackable.UnknownTrackableConnector;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

public final class ConnectorFactory {
    @NonNull public static final UnknownConnector UNKNOWN_CONNECTOR = new UnknownConnector();
    @NonNull private static final Collection<IConnector> CONNECTORS = Collections.unmodifiableCollection(Arrays.<IConnector> asList(
            GCConnector.getInstance(),
            ECConnector.getInstance(),
            new OCDEConnector(),
            new OCCZConnector(),
            new OCApiLiveConnector("opencache.uk", "opencache.uk", false, "OK", "CC BY-NC-SA 2.5",
                    R.string.oc_uk2_okapi_consumer_key, R.string.oc_uk2_okapi_consumer_secret,
                    R.string.pref_connectorOCUKActive, R.string.pref_ocuk2_tokenpublic, R.string.pref_ocuk2_tokensecret, ApiSupport.current, "OC.UK"), new OCConnector("OpenCaching.NO/SE", "www.opencaching.se", false, "OS", "OC.NO"),
            new OCApiLiveConnector("opencaching.nl", "www.opencaching.nl", false, "OB", "CC BY-SA 3.0",
                    R.string.oc_nl_okapi_consumer_key, R.string.oc_nl_okapi_consumer_secret,
                    R.string.pref_connectorOCNLActive, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret, ApiSupport.current, "OC.NL"),
            new OCApiLiveConnector("opencaching.pl", "opencaching.pl", false, "OP", "CC BY-SA 3.0",
                    R.string.oc_pl_okapi_consumer_key, R.string.oc_pl_okapi_consumer_secret,
                    R.string.pref_connectorOCPLActive, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, ApiSupport.current, "OC.PL"),
            new OCApiLiveConnector("opencaching.us", "www.opencaching.us", true, "OU", "CC BY-NC-SA 2.5",
                    R.string.oc_us_okapi_consumer_key, R.string.oc_us_okapi_consumer_secret,
                    R.string.pref_connectorOCUSActive, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret, ApiSupport.current, "OC.US"),
            new OCApiLiveConnector("opencaching.ro", "www.opencaching.ro", false, "OR", "CC BY-SA 3.0",
                    R.string.oc_ro_okapi_consumer_key, R.string.oc_ro_okapi_consumer_secret,
                    R.string.pref_connectorOCROActive, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret, ApiSupport.current, "OC.RO"),
            new GeocachingAustraliaConnector(),
            new GeopeitusConnector(),
            new TerraCachingConnector(),
            new WaymarkingConnector(),
            SuConnector.getInstance(),
            InternalConnector.getInstance(),
            UNKNOWN_CONNECTOR // the unknown connector MUST be the last one
    ));

    @NonNull public static final UnknownTrackableConnector UNKNOWN_TRACKABLE_CONNECTOR = new UnknownTrackableConnector();

    @NonNull
    private static final Collection<TrackableConnector> TRACKABLE_CONNECTORS = Collections.unmodifiableCollection(Arrays.<TrackableConnector> asList(
            new GeokretyConnector(),
            new GeolutinsConnector(),
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

    private static boolean forceRelog = false; // c:geo needs to log into cache providers

    // for viewport searchResult caching
    private static int counterSearchByViewport = 0;     // @todo delete this line after testing
    private static Viewport lastViewportUsed = null;
    private static SearchResult lastSearchResult = null;

    private ConnectorFactory() {
        // utility class
    }

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

    @NonNull
    public static List<IConnector> getActiveConnectors() {
        final List<IConnector> activeConnectors = new ArrayList<>();
        for (final IConnector conn : CONNECTORS) {
            if (conn.isActive()) {
                activeConnectors.add(conn);
            }
        }
        return activeConnectors;
    }

    public static boolean anyConnectorActive() {
        for (final IConnector conn : CONNECTORS) {
            if (conn.isActive()) {
                return true;
            }
        }
        return false;
    }

    public static boolean anyTrackableConnectorActive() {
        for (final TrackableConnector conn : TRACKABLE_CONNECTORS) {
            if (conn.isActive()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canHandle(@Nullable final String geocode) {
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

    /**
     * Get the connector handling all the operations available on this geocache. There is always a connector, it might
     * be the {@link UnknownConnector} if the geocache can't be identified.
     */
    @NonNull
    public static IConnector getConnector(final Geocache cache) {
        return getConnector(cache.getGeocode());
    }

    /**
     * Get a connector capability for the given geocache. This might be {@code null} if the connector does not support
     * the given capability.
     *
     * @return the connector cast to the requested capability or {@code null}.
     */
    @Nullable
    public static <T extends IConnector> T getConnectorAs(@Nullable final Geocache cache, @NonNull final Class<T> capabilityClass) {
        if (cache == null) {
            return null;
        }
        final IConnector connector = getConnector(cache);
        if (capabilityClass.isInstance(connector)) {
            return capabilityClass.cast(connector);
        }
        return null;
    }

    @NonNull
    public static TrackableConnector getConnector(final Trackable trackable) {
        return getTrackableConnector(trackable.getGeocode());
    }

    @NonNull
    public static TrackableConnector getTrackableConnector(final String geocode) {
        return getTrackableConnector(geocode, TrackableBrand.UNKNOWN);
    }

    @NonNull
    public static TrackableConnector getTrackableConnector(final String geocode, final TrackableBrand brand) {
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            if (connector.canHandleTrackable(geocode, brand)) {
                return connector;
            }
        }
        return UNKNOWN_TRACKABLE_CONNECTOR; // avoid null checks by returning a non implementing connector
    }

    /**
     * Get the list of active generic trackable connectors
     *
     * @return the list of actives connectors.
     */
    public static List<TrackableConnector> getGenericTrackablesConnectors() {
        final List<TrackableConnector> trackableConnectors = new ArrayList<>();
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            if (connector.isActive()) {
                trackableConnectors.add(connector);
            }
        }
        return trackableConnectors;
    }

    /**
     * Get the list of active generic trackable connectors with support logging and currently connected
     *
     * @return the list of actives connectors supporting logging.
     */
    public static List<TrackableConnector> getLoggableGenericTrackablesConnectors() {
        final List<TrackableConnector> trackableConnectors = new ArrayList<>();
        for (final TrackableConnector connector : getGenericTrackablesConnectors()) {
            if (connector.isGenericLoggable() && connector.isRegistered()) {
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

    /**
     * @see ISearchByViewPort#searchByViewport
     * @param forceByZoomlevel: reset cache if true and cached searchResult >= 400 results and requested viewport changed
     *  */
    @NonNull
    public static SearchResult searchByViewport(@NonNull final Viewport viewport, final boolean forceByZoomlevel) {
        final boolean viewportChanged = (null == lastViewportUsed) || !viewport.equals(lastViewportUsed);
        final boolean skipCache = forceByZoomlevel && null != lastSearchResult && lastSearchResult.getCount() >= 400 && viewportChanged;
        if (null != lastViewportUsed && lastViewportUsed.includes(viewport) && !skipCache) {
            Log.d("searchByViewport: saved searchResult reused");            // @todo delete this line after testing
            return lastSearchResult;
        }

        // @todo delete next two lines after testing
        counterSearchByViewport++;
        Log.d("called searchByViewport #" + counterSearchByViewport);

        lastViewportUsed = viewport.resize(3);
        lastSearchResult = SearchResult.parallelCombineActive(searchByViewPortConns, connector -> connector.searchByViewport(lastViewportUsed));
        return lastSearchResult;
    }

    @Nullable
    public static String getGeocodeFromURL(@Nullable final String url) {
        if (url == null) {
            return null;
        }
        for (final IConnector connector : CONNECTORS) {
            final String geocode = connector.getGeocodeFromUrl(url);
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
     * Get trackable geocode from an URL.
     *
     * @return
     *          the geocode, {@code null} if the URL cannot be decoded
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

    /**
     * Get trackable Tracking Code from an URL.
     *
     * @return
     *          the TrackableTrackingCode object, {@code null} if the URL cannot be decoded
     */
    @NonNull
    public static TrackableTrackingCode getTrackableTrackingCodeFromURL(final String url) {
        if (url == null) {
            return TrackableTrackingCode.EMPTY;
        }
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            final String trackableCode = connector.getTrackableTrackingCodeFromUrl(url);
            if (StringUtils.isNotBlank(trackableCode)) {
                return new TrackableTrackingCode(trackableCode, connector.getBrand());
            }
        }
        return TrackableTrackingCode.EMPTY;
    }

    /**
     * Load a trackable.
     *
     * We query all the connectors that can handle the trackable in parallel as well as the local storage.
     * We return the first positive result coming from a connector, or, if none, the result of loading from
     * the local storage.
     *
     * @param geocode
     *          trackable geocode
     * @param guid
     *          trackable guid
     * @param id
     *          trackable id
     * @param brand
     *          trackable brand
     * @return
     *          The Trackable observable
     */
    public static Maybe<Trackable> loadTrackable(final String geocode, final String guid, final String id, final TrackableBrand brand) {
        if (StringUtils.isEmpty(geocode)) {
            // Only solution is GC search by uid
            return Maybe.fromCallable(() -> TravelBugConnector.getInstance().searchTrackable(geocode, guid, id)).subscribeOn(AndroidRxUtils.networkScheduler);
        }

        final Observable<Trackable> fromNetwork =
                Observable.fromIterable(getTrackableConnectors()).filter(trackableConnector -> trackableConnector.canHandleTrackable(geocode, brand)).flatMapMaybe((Function<TrackableConnector, Maybe<Trackable>>) trackableConnector -> Maybe.fromCallable(() -> trackableConnector.searchTrackable(geocode, guid, id)).subscribeOn(AndroidRxUtils.networkScheduler));

        final Maybe<Trackable> fromLocalStorage = Maybe.fromCallable(() -> DataStore.loadTrackable(geocode)).subscribeOn(Schedulers.io());

        return fromNetwork.firstElement().switchIfEmpty(fromLocalStorage);
    }

    /**
     * Check if cgeo must relog even if already logged in.
     *
     * @return {@code true} if it is necessary to relog
     */
    public static boolean mustRelog() {
        final boolean mustLogin = forceRelog;
        forceRelog = false;
        return mustLogin;
    }

    /**
     * Force cgeo to relog when reaching the main activity.
     */
    public static void forceRelog() {
        forceRelog = true;
    }

}
