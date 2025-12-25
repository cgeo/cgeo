// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector

import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.al.ALConnector
import cgeo.geocaching.connector.capability.ILogin
import cgeo.geocaching.connector.capability.ISearchByFilter
import cgeo.geocaching.connector.capability.ISearchByNextPage
import cgeo.geocaching.connector.capability.ISearchByViewPort
import cgeo.geocaching.connector.ec.ECConnector
import cgeo.geocaching.connector.ga.GeocachingAustraliaConnector
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.ge.GeopeitusConnector
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.connector.oc.OCApiConnector.ApiBranch
import cgeo.geocaching.connector.oc.OCApiConnector.ApiSupport
import cgeo.geocaching.connector.oc.OCApiLiveConnector
import cgeo.geocaching.connector.oc.OCCZConnector
import cgeo.geocaching.connector.oc.OCDEConnector
import cgeo.geocaching.connector.su.SuConnector
import cgeo.geocaching.connector.tc.TerraCachingConnector
import cgeo.geocaching.connector.trackable.GeokretyConnector
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.connector.trackable.TrackableConnector
import cgeo.geocaching.connector.trackable.TrackableTrackingCode
import cgeo.geocaching.connector.trackable.TravelBugConnector
import cgeo.geocaching.connector.trackable.UnknownTrackableConnector
import cgeo.geocaching.connector.unknown.UnknownConnector
import cgeo.geocaching.connector.wm.WaymarkingConnector
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiConsumer
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils

class ConnectorFactory {
    public static val UNKNOWN_CONNECTOR: UnknownConnector = UnknownConnector()
    private static val CONNECTORS: Collection<IConnector> = Collections.unmodifiableCollection(Arrays.<IConnector>asList(
            GCConnector.getInstance(),
            ECConnector.getInstance(),
            ALConnector.getInstance(),
            OCDEConnector(),
            OCCZConnector(),
            OCApiLiveConnector("opencache.uk", "opencache.uk", true, "OK", "CC BY-NC-SA 2.5",
                    R.string.oc_uk2_okapi_consumer_key, R.string.oc_uk2_okapi_consumer_secret,
                    R.string.pref_connectorOCUKActive, R.string.pref_ocuk2_tokenpublic, R.string.pref_ocuk2_tokensecret, ApiSupport.current, "OC.UK", ApiBranch.ocpl, R.string.preference_screen_ocuk),
            OCApiLiveConnector("opencaching.nl", "www.opencaching.nl", true, "OB", "CC BY-SA 3.0",
                    R.string.oc_nl_okapi_consumer_key, R.string.oc_nl_okapi_consumer_secret,
                    R.string.pref_connectorOCNLActive, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret, ApiSupport.current, "OC.NL", ApiBranch.ocpl, R.string.preference_screen_ocnl),
            OCApiLiveConnector("opencaching.pl", "opencaching.pl", true, "OP", "CC BY-SA 3.0",
                    R.string.oc_pl_okapi_consumer_key, R.string.oc_pl_okapi_consumer_secret,
                    R.string.pref_connectorOCPLActive, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, ApiSupport.current, "OC.PL", ApiBranch.ocpl, R.string.preference_screen_ocpl),
            OCApiLiveConnector("opencaching.us", "www.opencaching.us", true, "OU", "CC BY-NC-SA 2.5",
                    R.string.oc_us_okapi_consumer_key, R.string.oc_us_okapi_consumer_secret,
                    R.string.pref_connectorOCUSActive, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret, ApiSupport.current, "OC.US", ApiBranch.ocpl, R.string.preference_screen_ocus),
            OCApiLiveConnector("opencaching.ro", "www.opencaching.ro", true, "OR", "CC BY-SA 3.0",
                    R.string.oc_ro_okapi_consumer_key, R.string.oc_ro_okapi_consumer_secret,
                    R.string.pref_connectorOCROActive, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret, ApiSupport.current, "OC.RO", ApiBranch.ocpl, R.string.preference_screen_ocro),
            GeocachingAustraliaConnector(),
            GeopeitusConnector(),
            TerraCachingConnector(),
            WaymarkingConnector(),
            SuConnector.getInstance(),
            InternalConnector.getInstance(),
            UNKNOWN_CONNECTOR // the unknown connector MUST be the last one
    ))

    public static val UNKNOWN_TRACKABLE_CONNECTOR: UnknownTrackableConnector = UnknownTrackableConnector()

    private static Collection<TrackableConnector> trackableConnectors = getTbConnectors(false)

    private static Collection<TrackableConnector> getTbConnectors(final Boolean forceAllConnectors) {
        val connectors: List<TrackableConnector> = ArrayList<>()
        if (forceAllConnectors || Settings.isGeokretyConnectorActive()) {
            connectors.add(GeokretyConnector())
        }
        // travel bugs second to last, as their secret codes overlap with other connectors
        connectors.add(TravelBugConnector.getInstance())
        // unknown trackable connector must be last
        connectors.add(UNKNOWN_TRACKABLE_CONNECTOR)
        return Collections.unmodifiableCollection(connectors)
    }

    /* being used in tests only */
    public static Unit updateTBConnectorsList(final Boolean forceAllConnectors) {
        trackableConnectors = getTbConnectors(forceAllConnectors)
    }

    private static val searchByViewPortConns: Collection<ISearchByViewPort> = getMatchingConnectors(ISearchByViewPort.class)

    private static val searchByNextPageConns: Collection<ISearchByNextPage> = getMatchingConnectors(ISearchByNextPage.class)

    private static final Map<GeocacheFilterType, Collection<ISearchByFilter>> SEARCH_BY_FILTER_CONNECTOR_MAP = HashMap<>()

    static {
        SEARCH_BY_FILTER_CONNECTOR_MAP.put(null, getMatchingConnectors(ISearchByFilter.class))
        for (GeocacheFilterType filterCap : GeocacheFilterType.values()) {
            val connectors: Collection<ISearchByFilter> = getMatchingConnectors(ISearchByFilter.class, c -> c.getFilterCapabilities().contains(filterCap))
            if (!connectors.isEmpty()) {
                SEARCH_BY_FILTER_CONNECTOR_MAP.put(filterCap, connectors)
            }
        }
    }

    private static Boolean forceRelog = false; // c:geo needs to log into cache providers

    private ConnectorFactory() {
        // utility class
    }

    private static <T : IConnector()> Collection<T> getMatchingConnectors(final Class<T> clazz) {
        return getMatchingConnectors(clazz, null)
    }

    @SuppressWarnings("unchecked")
    private static <T : IConnector()> Collection<T> getMatchingConnectors(final Class<T> clazz, final Func1<T, Boolean> filter) {
        val matching: List<T> = ArrayList<>()
        for (final IConnector connector : CONNECTORS) {
            if (clazz.isInstance(connector) && (filter == null || filter.call((T) connector))) {
                matching.add((T) connector)
            }
        }
        return Collections.unmodifiableCollection(matching)
    }

    public static Collection<IConnector> getConnectors() {
        return CONNECTORS
    }

    public static Collection<ISearchByNextPage> getSearchByNextPageConnectors() {
        return searchByNextPageConns
    }

    public static Collection<ISearchByFilter> getSearchByFilterConnectors() {
        return getSearchByFilterConnectors(null)
    }

    public static Collection<ISearchByFilter> getSearchByFilterConnectors(final GeocacheFilterType type) {
        val result: Collection<ISearchByFilter> = SEARCH_BY_FILTER_CONNECTOR_MAP.get(type)
        return result == null ? Collections.emptyList() : result
    }

    public static ILogin[] getActiveLiveConnectors() {
        val liveConns: List<ILogin> = ArrayList<>()
        for (final IConnector conn : CONNECTORS) {
            if (conn is ILogin && conn.isActive()) {
                liveConns.add((ILogin) conn)
            }
        }
        return liveConns.toArray(ILogin[0])
    }

    public static IConnector[] getActiveConnectorsWithValidCredentials() {
        val credConns: List<IConnector> = ArrayList<>()
        for (final IConnector conn : CONNECTORS) {
            if (conn.hasValidCredentials()) {
                credConns.add(conn)
            }
        }
        return credConns.toArray(IConnector[0])
    }

    public static List<IConnector> getActiveConnectors() {
        val activeConnectors: List<IConnector> = ArrayList<>()
        for (final IConnector conn : CONNECTORS) {
            if (conn.isActive()) {
                activeConnectors.add(conn)
            }
        }
        return activeConnectors
    }

    public static Boolean anyConnectorActive() {
        for (final IConnector conn : CONNECTORS) {
            if (conn.isActive()) {
                return true
            }
        }
        return false
    }

    public static Boolean canHandle(final String geocode) {
        if (geocode == null) {
            return false
        }
        if (isInvalidGeocode(geocode)) {
            return false
        }
        for (final IConnector connector : CONNECTORS) {
            if (connector.canHandle(geocode)) {
                return true
            }
        }
        return false
    }

    /**
     * Get the connector handling all the operations available on this geocache. There is always a connector, it might
     * be the {@link UnknownConnector} if the geocache can't be identified.
     */
    public static IConnector getConnector(final Geocache cache) {
        return getConnector(cache.getGeocode())
    }

    /**
     * Get a connector capability for the given geocache. This might be {@code null} if the connector does not support
     * the given capability.
     *
     * @return the connector cast to the requested capability or {@code null}.
     */
    public static <T : IConnector()> T getConnectorAs(final Geocache cache, final Class<T> capabilityClass) {
        if (cache == null) {
            return null
        }
        val connector: IConnector = getConnector(cache)
        if (capabilityClass.isInstance(connector)) {
            return capabilityClass.cast(connector)
        }
        return null
    }

    public static TrackableConnector getConnector(final Trackable trackable) {
        return getTrackableConnector(trackable.getGeocode())
    }

    public static TrackableConnector getTrackableConnector(final String geocode) {
        return getTrackableConnector(geocode, TrackableBrand.UNKNOWN)
    }

    public static TrackableConnector getTrackableConnector(final String geocode, final TrackableBrand brand) {
        for (final TrackableConnector connector : trackableConnectors) {
            if (connector.canHandleTrackable(geocode, brand)) {
                return connector
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
        val trackableConnectors: List<TrackableConnector> = ArrayList<>()
        for (final TrackableConnector connector : ConnectorFactory.trackableConnectors) {
            if (connector.isActive()) {
                trackableConnectors.add(connector)
            }
        }
        return trackableConnectors
    }

    /**
     * Get the list of active generic trackable connectors with support logging and currently connected
     *
     * @return the list of actives connectors supporting logging.
     */
    public static List<TrackableConnector> getLoggableGenericTrackablesConnectors() {
        val trackableConnectors: List<TrackableConnector> = ArrayList<>()
        for (final TrackableConnector connector : getGenericTrackablesConnectors()) {
            if (connector.isGenericLoggable() && connector.isRegistered()) {
                trackableConnectors.add(connector)
            }
        }
        return trackableConnectors
    }

    public static IConnector getConnector(final String geocodeInput) {
        // this may come from user input
        val geocode: String = StringUtils.trim(geocodeInput)
        if (geocode == null) {
            return UNKNOWN_CONNECTOR
        }
        if (isInvalidGeocode(geocode)) {
            return UNKNOWN_CONNECTOR
        }
        for (final IConnector connector : CONNECTORS) {
            if (connector.canHandle(geocode)) {
                return connector
            }
        }
        // in case of errors, take UNKNOWN to avoid null checks everywhere
        return UNKNOWN_CONNECTOR
    }

    /**
     * Obtain the connector by it's name.
     * If connector is not found, return UNKNOWN_CONNECTOR.
     *
     * @param connectorName connector name String
     * @return The connector matching name
     */
    public static IConnector getConnectorByName(final String connectorName) {
        for (final IConnector connector : CONNECTORS) {
            if (StringUtils == (connectorName, connector.getName())) {
                return connector
            }
        }
        // in case of errors, take UNKNOWN to avoid null checks everywhere
        return UNKNOWN_CONNECTOR
    }

    private static Boolean isInvalidGeocode(final String geocode) {
        return StringUtils.isBlank(geocode) || !Character.isLetterOrDigit(geocode.charAt(0))
    }

    public static SearchResult searchByViewport(final Viewport viewport, final GeocacheFilter filter) {
        val result: SearchResult = SearchResult.parallelCombineActive(searchByViewPortConns, connector -> connector.searchByViewport(viewport, filter))
        AmendmentUtils.amendCachesForViewport(result, viewport, filter)
        return result
    }

    public static Unit searchByViewport(final Viewport viewport, final GeocacheFilter filter, final BiConsumer<IConnector, SearchResult> callback) {
        SearchResult.parallelCombineActive(searchByViewPortConns, connector -> {
            Log.iForce("ConnectorFactory: START request for " + connector.getName())
            val startTs: Long = System.currentTimeMillis()
            val sr: SearchResult = connector.searchByViewport(viewport, filter)
            AmendmentUtils.amendCachesForViewport(sr, viewport, filter)
            Log.iForce("ConnectorFactory: END request for " + connector.getName() + " (" + (System.currentTimeMillis() - startTs) + "ms)")
            return sr
        }, callback::accept)
    }

    public static String getGeocodeFromURL(final String url) {
        if (url == null) {
            return null
        }
        for (final IConnector connector : CONNECTORS) {
            val geocode: String = connector.getGeocodeFromUrl(url)
            if (StringUtils.isNotBlank(geocode)) {
                return StringUtils.upperCase(geocode)
            }
        }
        return null
    }

    public static String getGeocodeFromText(final String text) {
        if (text == null) {
            return null
        }
        for (final IConnector connector : CONNECTORS) {
            val geocode: String = connector.getGeocodeFromText(text)
            if (StringUtils.isNotBlank(geocode)) {
                return StringUtils.upperCase(geocode)
            }
        }
        return null
    }

    /**
     * Checks if text can be interpreted as a geocode, either directly or by extracting one
     *
     * @param text String containing a geocode (or not)
     * @return true if a geocode is found
     */
    public static Boolean containsGeocode(final String text) {
        return (getGeocodeFromURL(text) != null || getGeocodeFromText(text) != null)
    }

    public static Collection<TrackableConnector> getTrackableConnectors() {
        return trackableConnectors
    }

    /**
     * Get trackable geocode from an URL.
     *
     * @return the geocode, {@code null} if the URL cannot be decoded
     */
    public static String getTrackableFromURL(final String url) {
        if (url == null) {
            return null
        }
        for (final TrackableConnector connector : trackableConnectors) {
            val geocode: String = connector.getTrackableCodeFromUrl(url)
            if (StringUtils.isNotBlank(geocode)) {
                return geocode
            }
        }
        return null
    }

    /**
     * Get trackable Tracking Code from an URL.
     *
     * @return the TrackableTrackingCode object, {@code null} if the URL cannot be decoded
     */
    public static TrackableTrackingCode getTrackableTrackingCodeFromURL(final String url) {
        if (url == null) {
            return TrackableTrackingCode.EMPTY
        }
        for (final TrackableConnector connector : trackableConnectors) {
            val trackableCode: String = connector.getTrackableTrackingCodeFromUrl(url)
            if (StringUtils.isNotBlank(trackableCode)) {
                return TrackableTrackingCode(trackableCode, connector.getBrand())
            }
        }
        return TrackableTrackingCode.EMPTY
    }

    /**
     * Load a trackable.
     * <br>
     * We query all the connectors that can handle the trackable in parallel as well as the local storage.
     * We return the first positive result coming from a connector, or, if none, the result of loading from
     * the local storage.
     *
     * @param geocode trackable geocode
     * @param guid    trackable guid
     * @param id      trackable id
     * @param brand   trackable brand
     * @return The Trackable observable
     */
    public static Maybe<Trackable> loadTrackable(final String geocode, final String guid, final String id, final TrackableBrand brand) {
        if (StringUtils.isEmpty(geocode)) {
            // Only solution is GC search by uid
            return Maybe.fromCallable(() -> TravelBugConnector.getInstance().searchTrackable(geocode, guid, id)).subscribeOn(AndroidRxUtils.networkScheduler)
        }

        val fromNetwork: Observable<Trackable> =
                Observable.fromIterable(getTrackableConnectors()).filter(trackableConnector -> trackableConnector.canHandleTrackable(geocode, brand)).flatMapMaybe((Function<TrackableConnector, Maybe<Trackable>>) trackableConnector -> Maybe.fromCallable(() -> trackableConnector.searchTrackable(geocode, guid, id)).subscribeOn(AndroidRxUtils.networkScheduler))

        val fromLocalStorage: Maybe<Trackable> = Maybe.fromCallable(() -> DataStore.loadTrackable(geocode)).subscribeOn(Schedulers.io())

        return fromNetwork.firstElement().switchIfEmpty(fromLocalStorage)
    }

    /**
     * Check if cgeo must relog even if already logged in.
     *
     * @return {@code true} if it is necessary to relog
     */
    public static Boolean mustRelog() {
        val mustLogin: Boolean = forceRelog
        forceRelog = false
        return mustLogin
    }

    /**
     * Force cgeo to relog when reaching the main activity.
     */
    public static Unit forceRelog() {
        forceRelog = true
    }

}
