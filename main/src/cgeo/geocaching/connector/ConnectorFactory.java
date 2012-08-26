package cgeo.geocaching.connector;

import cgeo.geocaching.ICache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCApiConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.connector.ox.OXConnector;
import cgeo.geocaching.geopoint.Viewport;

import org.apache.commons.lang3.StringUtils;

public final class ConnectorFactory {
    private static final UnknownConnector UNKNOWN_CONNECTOR = new UnknownConnector();
    private static final IConnector[] connectors = new IConnector[] {
            GCConnector.getInstance(),
            new OCConnector("OpenCaching.DE", "www.opencaching.de", "OC"),
            new OCConnector("OpenCaching.CZ", "www.opencaching.cz", "OZ"),
            new OCApiConnector("OpenCaching.CO.UK", "www.opencaching.org.uk", "OK", "arU4okouc4GEjMniE2fq"),
            new OCConnector("OpenCaching.ES", "www.opencachingspain.es", "OC"),
            new OCConnector("OpenCaching.IT", "www.opencaching.it", "OC"),
            new OCConnector("OpenCaching.JP", "www.opencaching.jp", "OJ"),
            new OCConnector("OpenCaching.NO/SE", "www.opencaching.no", "OS"),
            new OCConnector("OpenCaching.NL", "www.opencaching.nl", "OB"),
            new OCApiConnector("OpenCaching.PL", "www.opencaching.pl", "OP", "GkxM47WkUkLQXXsZ9qSh"),
            new OCApiConnector("OpenCaching.US", "www.opencaching.us", "OU", "pTsYAYSXFcfcRQnYE6uA"),
            new OXConnector(),
            new GeocachingAustraliaConnector(),
            new GeopeitusConnector(),
            UNKNOWN_CONNECTOR // the unknown connector MUST be the last one
    };

    public static IConnector[] getConnectors() {
        return connectors;
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

    /** @see IConnector#searchByViewport */
    public static SearchResult searchByViewport(final Viewport viewport, final String[] tokens) {
        // We have only connector capable of doing a 'searchByViewport()'
        // If there is a second connector the information has to be collected from all collectors
        return GCConnector.getInstance().searchByViewport(viewport, tokens);
    }

}
