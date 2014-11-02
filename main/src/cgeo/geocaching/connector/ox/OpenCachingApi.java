package cgeo.geocaching.connector.ox;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;

public class OpenCachingApi {

    private static final String API_URL_CACHES_GPX = "http://www.opencaching.com/api/geocache.gpx";
    private static final String DEV_KEY = CryptUtils.rot13("PtqQnHo9RUTht3Np");

    public static Geocache searchByGeoCode(final @NonNull String geocode) {
        final HttpResponse response = getRequest("http://www.opencaching.com/api/geocache/" + geocode + ".gpx",
                new Parameters(
                        "log_limit", "50",
                        "hint", "true",
                        "description", "html"));
        final Collection<Geocache> caches = importCachesFromResponse(response, true);
        if (CollectionUtils.isNotEmpty(caches)) {
            return caches.iterator().next();
        }
        return null;
    }

    private static HttpResponse getRequest(final String uri, final Parameters parameters) {
        parameters.add("Authorization", DEV_KEY);
        return Network.getRequest(uri, parameters);
    }

    private static Collection<Geocache> importCachesFromResponse(final HttpResponse response, final boolean isDetailed) {
        if (response == null) {
            return Collections.emptyList();
        }
        Collection<Geocache> caches;
        try {
            caches = new OXGPXParser(StoredList.TEMPORARY_LIST.id, isDetailed).parse(response.getEntity().getContent(), null);
        } catch (Exception e) {
            Log.e("Error importing from OpenCaching.com", e);
            return Collections.emptyList();
        }
        return caches;
    }

    public static Collection<Geocache> searchByCenter(final @NonNull Geopoint center) {
        final Parameters queryParameters = new Parameters(
                "log_limit", "0",
                "hint", "false",
                "description", "none",
                "limit", "20",
                "center", center.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA));
        if (addTypeFilter(queryParameters)) {
            final HttpResponse response = getRequest(API_URL_CACHES_GPX, queryParameters);

            return importCachesFromResponse(response, false);
        }
        return Collections.emptyList();
    }

    /**
     * Adds filter for the global cache type to the query parameters.
     * Returns false if the type doesn't exist on OX and thus would exclude all caches.
     *
     * @param queryParameters
     *            Parameters to modify
     * @return True - query possible, False - no query, all caches excluded
     */
    private static boolean addTypeFilter(Parameters queryParameters) {
        boolean doQuery = true;
        if (Settings.getCacheType() != CacheType.ALL) {
            String typeFilter;
            switch (Settings.getCacheType()) {
                case TRADITIONAL:
                    typeFilter = "Traditional Cache";
                    break;
                case MULTI:
                    typeFilter = "Multi-cache";
                    break;
                case MYSTERY:
                    typeFilter = "Unknown Cache";
                    break;
                case VIRTUAL:
                    typeFilter = "Virtual Cache";
                    break;
                default:
                    typeFilter = StringUtils.EMPTY;
                    doQuery = false;
            }
            queryParameters.add("type", typeFilter);
        }
        return doQuery;
    }

    public static Collection<Geocache> searchByBoundingBox(final @NonNull Viewport viewport) {
        final String bbox = viewport.bottomLeft.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA) + "," + viewport.topRight.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA);
        final Parameters queryParameters = new Parameters(
                "log_limit", "0",
                "hint", "false",
                "description", "none",
                "limit", "100",
                "bbox", bbox);
        if (addTypeFilter(queryParameters)) {
            final HttpResponse response = getRequest(API_URL_CACHES_GPX, queryParameters);
            return importCachesFromResponse(response, false);
        }
        return Collections.emptyList();
    }

    public static Collection<Geocache> searchByKeyword(final @NonNull String name) {
        final Parameters queryParameters = new Parameters(
                "log_limit", "5",
                "hint", "false",
                "description", "none",
                "limit", "100",
                "name", name);
        if (addTypeFilter(queryParameters)) {
            final HttpResponse response = getRequest(API_URL_CACHES_GPX, queryParameters);
            return importCachesFromResponse(response, false);
        }
        return Collections.emptyList();
    }

}
