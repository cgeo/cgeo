package cgeo.geocaching.connector;

import cgeo.geocaching.ICache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgTrackable;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.connector.capability.ISearchByViewPort;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCApiConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.connector.oc.OCXMLApiConnector;
import cgeo.geocaching.connector.ox.OXConnector;
import cgeo.geocaching.geopoint.Viewport;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ConnectorFactory {
    private static final UnknownConnector UNKNOWN_CONNECTOR = new UnknownConnector();
    private static final IConnector[] connectors = new IConnector[] {
            GCConnector.getInstance(),
            new OCXMLApiConnector("OpenCaching.DE", "www.opencaching.de", "OC"),
            new OCConnector("OpenCaching.CZ", "www.opencaching.cz", "OZ"),
            new OCApiConnector("OpenCaching.CO.UK", "www.opencaching.org.uk", "OK", "arU4okouc4GEjMniE2fq"),
            new OCConnector("OpenCaching.ES", "www.opencachingspain.es", "OC"),
            new OCConnector("OpenCaching.IT", "www.opencaching.it", "OC"),
            new OCConnector("OpenCaching.JP", "www.opencaching.jp", "OJ"),
            new OCConnector("OpenCaching.NO/SE", "www.opencaching.se", "OS"),
            new OCApiConnector("OpenCaching.NL", "www.opencaching.nl", "OB", "PdzU8jzIlcfMADXaYN8j"),
            new OCApiConnector("OpenCaching.PL", "www.opencaching.pl", "OP", "GkxM47WkUkLQXXsZ9qSh"),
            new OCApiConnector("OpenCaching.US", "www.opencaching.us", "OU", "pTsYAYSXFcfcRQnYE6uA"),
            new OXConnector(),
            new GeocachingAustraliaConnector(),
            new GeopeitusConnector(),
            UNKNOWN_CONNECTOR // the unknown connector MUST be the last one
    };

    private static final ISearchByViewPort[] searchByViewPortConns;

    private static final ISearchByCenter[] searchByCenterConns;

    static {
        List<ISearchByViewPort> vpConns = new ArrayList<ISearchByViewPort>();
        for (IConnector conn : connectors) {
            if (conn instanceof ISearchByViewPort) {
                vpConns.add((ISearchByViewPort) conn);
            }
        }
        searchByViewPortConns = vpConns.toArray(new ISearchByViewPort[] {});

        List<ISearchByCenter> centerConns = new ArrayList<ISearchByCenter>();
        for (IConnector conn : connectors) {
            // GCConnector is handled specially, omit it here!
            if (conn instanceof ISearchByCenter && !(conn instanceof GCConnector)) {
                centerConns.add((ISearchByCenter) conn);
            }
        }
        searchByCenterConns = centerConns.toArray(new ISearchByCenter[] {});
    }

    public static IConnector[] getConnectors() {
        return connectors;
    }

    public static ISearchByCenter[] getSearchByCenterConnectors() {
        return searchByCenterConns;
    }

    public static boolean canHandle(final String geocode) {
        if (isInvalidGeocode(geocode)) {
            return false;
        }
        for (IConnector connector : connectors) {
            if (connector.canHandle(geocode)) {
                return true;
            }
        }
        return false;
    }

    public static IConnector getConnector(ICache cache) {
        return getConnector(cache.getGeocode());
    }

    public static IConnector getConnector(cgTrackable trackable) {
        return getConnector(trackable.getGeocode());
    }

    public static IConnector getConnector(final String geocodeInput) {
        // this may come from user input
        final String geocode = StringUtils.trim(geocodeInput);
        if (isInvalidGeocode(geocode)) {
            return UNKNOWN_CONNECTOR;
        }
        for (IConnector connector : connectors) {
            if (connector.canHandle(geocode)) {
                return connector;
            }
        }
        // in case of errors, take UNKNOWN
        return UNKNOWN_CONNECTOR;
    }

    private static boolean isInvalidGeocode(final String geocode) {
        return StringUtils.isBlank(geocode) || !Character.isLetterOrDigit(geocode.charAt(0));
    }

    /** @see ISearchByViewPort#searchByViewport */
    public static SearchResult searchByViewport(final Viewport viewport, final String[] tokens) {

        SearchResult result = new SearchResult();
        for (ISearchByViewPort vpconn : searchByViewPortConns) {
            if (vpconn.isActivated()) {
                SearchResult temp = vpconn.searchByViewport(viewport, tokens);
                if (temp != null) {
                    result.addGeocodes(temp.getGeocodes());
                }
            }
        }
        return result;
    }

    public static String getGeocodeFromURL(final String url) {
        for (IConnector connector : connectors) {
            String geocode = connector.getGeocodeFromUrl(url);
            if (StringUtils.isNotBlank(geocode)) {
                return geocode;
            }
        }
        return null;
    }

}
