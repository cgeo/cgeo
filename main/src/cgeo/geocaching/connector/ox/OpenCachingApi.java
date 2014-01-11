package cgeo.geocaching.connector.ox;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;
import org.apache.commons.collections4.CollectionUtils;
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

    private static HttpResponse getRequest(String string, Parameters parameters) {
        parameters.add("Authorization", DEV_KEY);
        return Network.getRequest(string, parameters);
    }

    private static Collection<Geocache> importCachesFromResponse(final HttpResponse response, final boolean isDetailed) {
        if (response == null) {
            return Collections.emptyList();
        }
        Collection<Geocache> caches;
        try {
            caches = new OXGPXParser(StoredList.TEMPORARY_LIST_ID, isDetailed).parse(response.getEntity().getContent(), null);
        } catch (Exception e) {
            Log.e("Error importing from OpenCaching.com", e);
            return Collections.emptyList();
        }
        return caches;
    }

    public static Collection<Geocache> searchByCenter(final @NonNull Geopoint center) {
        final HttpResponse response = getRequest(API_URL_CACHES_GPX,
                new Parameters(
                        "log_limit", "0",
                        "hint", "false",
                        "description", "none",
                        "limit", "20",
                        "center", center.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA)));
        return importCachesFromResponse(response, false);
    }


    public static Collection<Geocache> searchByBoundingBox(final @NonNull Viewport viewport) {
        final String bbox = viewport.bottomLeft.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA) + "," + viewport.topRight.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA);
        final HttpResponse response = getRequest(API_URL_CACHES_GPX,
                new Parameters(
                        "log_limit", "0",
                        "hint", "false",
                        "description", "none",
                        "limit", "100",
                        "bbox", bbox));
        return importCachesFromResponse(response, false);
    }

    public static Collection<Geocache> searchByKeyword(final @NonNull String name) {
        final HttpResponse response = getRequest(API_URL_CACHES_GPX,
                new Parameters(
                        "log_limit", "5",
                        "hint", "false",
                        "description", "none",
                        "limit", "100",
                        "name", name));
        return importCachesFromResponse(response, false);
    }

}
