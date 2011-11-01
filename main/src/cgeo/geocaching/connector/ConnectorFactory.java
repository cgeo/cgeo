package cgeo.geocaching.connector;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.opencaching.ApiOpenCachingConnector;
import cgeo.geocaching.connector.opencaching.OpenCachingConnector;

import org.apache.commons.lang3.StringUtils;

public final class ConnectorFactory {
    private static final UnknownConnector UNKNOWN_CONNECTOR = new UnknownConnector();
    private static final IConnector[] connectors = new IConnector[] {
            new GCConnector(),
            new OpenCachingConnector("OpenCaching.DE", "www.opencaching.de", "OC"),
            new OpenCachingConnector("OpenCaching.CZ", "www.opencaching.cz", "OZ"),
            new ApiOpenCachingConnector("OpenCaching.CO.UK", "www.opencaching.org.uk", "OK", "arU4okouc4GEjMniE2fq"),
            new OpenCachingConnector("OpenCaching.ES", "www.opencachingspain.es", "OC"),
            new OpenCachingConnector("OpenCaching.IT", "www.opencaching.it", "OC"),
            new OpenCachingConnector("OpenCaching.JP", "www.opencaching.jp", "OJ"),
            new OpenCachingConnector("OpenCaching.NO/SE", "www.opencaching.no", "OS"),
            new OpenCachingConnector("OpenCaching.NL", "www.opencaching.nl", "OB"),
            new ApiOpenCachingConnector("OpenCaching.PL", "www.opencaching.pl", "OP", "GkxM47WkUkLQXXsZ9qSh"),
            new ApiOpenCachingConnector("OpenCaching.US", "www.opencaching.us", "OU", "pTsYAYSXFcfcRQnYE6uA"),
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

    public static IConnector getConnector(cgCache cache) {
        return getConnector(cache.getGeocode());
    }

    public static IConnector getConnector(String geocode) {
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
        return StringUtils.isBlank(geocode);
    }
}
