package cgeo.geocaching.connector.oc;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class OCXMLClient {

    private static final String SERVICE_CACHE = "/xml/ocxml11.php";

    // Url for single cache requests
    // http://www.opencaching.de/xml/ocxml11.php?modifiedsince=20060320000000&user=0&cache=1&cachedesc=1&cachelog=1&picture=1&removedobject=0&session=0&doctype=0&charset=utf-8&wp=OCC9BE

    public static cgCache getCache(final String geoCode) {
        try {
            final Parameters params = new Parameters("modifiedsince", "20060320000000",
                    "user", "0", "cache", "1", "cachedesc", "1",
                    "cachelog", "1", "picture", "0", "removedobject", "0",
                    "session", "0", "doctype", "0", "charset", "utf-8", "zip", "gzip");
            params.put("wp", geoCode);
            final InputStream data = request(ConnectorFactory.getConnector(geoCode), SERVICE_CACHE, params);

            if (data == null) {
                return null;
            }

            Collection<cgCache> caches = OC11XMLParser.parseCaches(new GZIPInputStream(data));
            if (caches.iterator().hasNext()) {
                cgCache cache = caches.iterator().next();
                cache.setDetailed(true);
                cgData.saveCache(cache, LoadFlags.SAVE_ALL);
                return cache;
            }
            return null;
        } catch (IOException e) {
            Log.e("Error parsing cache '" + geoCode + "': " + e.toString());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<cgCache> getCachesAround(final Geopoint center, final double distance) {
        try {
            final Parameters params = new Parameters("modifiedsince", "20060320000000",
                    "user", "0", "cache", "1", "cachedesc", "0",
                    "cachelog", "0", "picture", "0", "removedobject", "0",
                    "session", "0", "doctype", "0", "charset", "utf-8", "zip", "gzip");
            params.put("lat", GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, center));
            params.put("lon", GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, center));
            params.put("distance", String.format(Locale.US, "%f", distance));
            final InputStream data = request(ConnectorFactory.getConnector("OCXXX"), SERVICE_CACHE, params);

            if (data == null) {
                return CollectionUtils.EMPTY_COLLECTION;
            }

            return OC11XMLParser.parseCaches(new GZIPInputStream(data));
        } catch (IOException e) {
            Log.e("Error parsing nearby search result: " + e.toString());
            return CollectionUtils.EMPTY_COLLECTION;
        }
    }

    private static InputStream request(final IConnector connector, final String service, final Parameters params) {
        if (connector == null) {
            return null;
        }
        if (!(connector instanceof OCXMLApiConnector)) {
            return null;
        }

        final String host = connector.getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }

        final String uri = "http://" + host + service;
        HttpResponse resp = Network.getRequest(uri, params);
        if (resp != null) {
            try {
                return resp.getEntity().getContent();
            } catch (IllegalStateException e) {
                // fall through and return null
            } catch (IOException e) {
                // fall through and return null
            }
        }
        return null;
    }
}
