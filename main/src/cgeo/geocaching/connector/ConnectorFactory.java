package cgeo.geocaching.connector;

import cgeo.geocaching.ICache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByKeyword;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCApiConnector;
import cgeo.geocaching.connector.oc.OCApiConnector.ApiSupport;
import cgeo.geocaching.connector.oc.OCApiLiveConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.connector.ox.OXConnector;
import cgeo.geocaching.connector.trackable.GeokretyConnector;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.connector.trackable.UnknownTrackableConnector;
import cgeo.geocaching.geopoint.Viewport;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class ConnectorFactory {
    private static final UnknownConnector UNKNOWN_CONNECTOR = new UnknownConnector();
    private static final IConnector[] CONNECTORS = new IConnector[] {
            GCConnector.getInstance(),
            new OCApiLiveConnector("opencaching.de", "www.opencaching.de", "OC", "CC BY-NC-ND, alle Logeinträge © jeweiliger Autor",
                    R.string.oc_de_okapi_consumer_key, R.string.oc_de_okapi_consumer_secret,
                    R.string.pref_connectorOCActive, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, ApiSupport.current),
            new OCConnector("OpenCaching.CZ", "www.opencaching.cz", "OZ"),
            new OCApiConnector("OpenCaching.CO.UK", "www.opencaching.org.uk", "OK", "arU4okouc4GEjMniE2fq", "CC BY-NC-SA 2.5", ApiSupport.oldapi),
            new OCConnector("OpenCaching.ES", "www.opencachingspain.es", "OC"),
            new OCConnector("OpenCaching.IT", "www.opencaching.it", "OC"),
            new OCConnector("OpenCaching.JP", "www.opencaching.jp", "OJ"),
            new OCConnector("OpenCaching.NO/SE", "www.opencaching.se", "OS"),
            new OCApiConnector("OpenCaching.NL", "www.opencaching.nl", "OB", "PdzU8jzIlcfMADXaYN8j", "CC BY-SA 3.0", ApiSupport.current),
            new OCApiLiveConnector("opencaching.pl", "www.opencaching.pl", "OP", "CC BY-SA 3.0",
                    R.string.oc_pl_okapi_consumer_key, R.string.oc_pl_okapi_consumer_secret,
                    R.string.pref_connectorOCPLActive, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, ApiSupport.current),
            new OCApiConnector("OpenCaching.US", "www.opencaching.us", "OU", "pTsYAYSXFcfcRQnYE6uA", "CC BY-NC-SA 2.5", ApiSupport.oldapi),
            new OXConnector(),
            new GeocachingAustraliaConnector(),
            new GeopeitusConnector(),
            new WaymarkingConnector(),
            UNKNOWN_CONNECTOR // the unknown connector MUST be the last one
    };

    @NonNull public static final UnknownTrackableConnector UNKNOWN_TRACKABLE_CONNECTOR = new UnknownTrackableConnector();
    private static final TrackableConnector[] TRACKABLE_CONNECTORS = new TrackableConnector[] {
            new GeokretyConnector(), // GK must be first, as it overlaps with the secret codes of travel bugs
            TravelBugConnector.getInstance(),
            UNKNOWN_TRACKABLE_CONNECTOR // must be last
    };

    private static final ISearchByViewPort[] searchByViewPortConns;

    private static final ISearchByCenter[] searchByCenterConns;

    private static final ISearchByKeyword[] searchByKeywordConns;

    static {
        final List<ISearchByViewPort> vpConns = new ArrayList<ISearchByViewPort>();
        for (final IConnector conn : CONNECTORS) {
            if (conn instanceof ISearchByViewPort) {
                vpConns.add((ISearchByViewPort) conn);
            }
        }
        searchByViewPortConns = vpConns.toArray(new ISearchByViewPort[vpConns.size()]);

        final List<ISearchByCenter> centerConns = new ArrayList<ISearchByCenter>();
        for (final IConnector conn : CONNECTORS) {
            // GCConnector is handled specially, omit it here!
            if (conn instanceof ISearchByCenter && !(conn instanceof GCConnector)) {
                centerConns.add((ISearchByCenter) conn);
            }
        }
        searchByCenterConns = centerConns.toArray(new ISearchByCenter[centerConns.size()]);

        final List<ISearchByKeyword> keywordConns = new ArrayList<ISearchByKeyword>();
        for (final IConnector conn : CONNECTORS) {
            // GCConnector is handled specially, omit it here!
            if (conn instanceof ISearchByKeyword && !(conn instanceof GCConnector)) {
                keywordConns.add((ISearchByKeyword) conn);
            }
        }
        searchByKeywordConns = keywordConns.toArray(new ISearchByKeyword[keywordConns.size()]);
    }

    public static IConnector[] getConnectors() {
        return CONNECTORS;
    }

    public static ISearchByCenter[] getSearchByCenterConnectors() {
        return searchByCenterConns;
    }

    public static ISearchByKeyword[] getSearchByKeywordConnectors() {
        return searchByKeywordConns;
    }

    public static ILogin[] getActiveLiveConnectors() {
        final List<ILogin> liveConns = new ArrayList<ILogin>();
        for (final IConnector conn : CONNECTORS) {
            if (conn instanceof ILogin && conn.isActivated()) {
                liveConns.add((ILogin) conn);
            }
        }
        return liveConns.toArray(new ILogin[liveConns.size()]);
    }

    public static boolean canHandle(final String geocode) {
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

    public static IConnector getConnector(ICache cache) {
        return getConnector(cache.getGeocode());
    }

    public static TrackableConnector getConnector(Trackable trackable) {
        return getTrackableConnector(trackable.getGeocode());
    }

    @NonNull
    public static TrackableConnector getTrackableConnector(String geocode) {
        for (final TrackableConnector connector : TRACKABLE_CONNECTORS) {
            if (connector.canHandleTrackable(geocode)) {
                return connector;
            }
        }
        return UNKNOWN_TRACKABLE_CONNECTOR; // avoid null checks by returning a non implementing connector
    }

    public static IConnector getConnector(final String geocodeInput) {
        // this may come from user input
        final String geocode = StringUtils.trim(geocodeInput);
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

    private static boolean isInvalidGeocode(final String geocode) {
        return StringUtils.isBlank(geocode) || !Character.isLetterOrDigit(geocode.charAt(0));
    }

    /** @see ISearchByViewPort#searchByViewport */
    public static SearchResult searchByViewport(final Viewport viewport, final String[] tokens) {

        final SearchResult result = new SearchResult();
        for (final ISearchByViewPort vpconn : searchByViewPortConns) {
            if (vpconn.isActivated()) {
                result.addSearchResult(vpconn.searchByViewport(viewport, tokens));
            }
        }
        return result;
    }

    public static String getGeocodeFromURL(final String url) {
        for (final IConnector connector : CONNECTORS) {
            final String geocode = connector.getGeocodeFromUrl(url);
            if (StringUtils.isNotBlank(geocode)) {
                return geocode;
            }
        }
        return null;
    }

    public static TrackableConnector[] getTrackableConnectors() {
        return TRACKABLE_CONNECTORS;
    }

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
