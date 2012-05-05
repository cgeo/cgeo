package cgeo.geocaching.connector.ox;

import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

public class OpenCachingApi {

    private static final String DEV_KEY = CryptUtils.rot13("PtqQnHo9RUTht3Np");

    public static cgCache searchByGeoCode(final String geocode) {
        final HttpResponse response = Network.getRequest("http://www.opencaching.com/api/geocache/" + geocode + ".gpx",
                new Parameters(
                        "Authorization", DEV_KEY,
                        "log_limit", "30",
                        "hint", "true",
                        "description", "html"));
        final Collection<cgCache> caches = importCachesFromResponse(response, true);
        if (CollectionUtils.isNotEmpty(caches)) {
            return caches.iterator().next();
        }
        return null;
    }

    private static Collection<cgCache> importCachesFromResponse(final HttpResponse response, final boolean isDetailed) {
        if (response == null) {
            return Collections.emptyList();
        }
        Collection<cgCache> caches = null;
        try {
            caches = new GPX10Parser(StoredList.STANDARD_LIST_ID).parse(response.getEntity().getContent(), null);
        } catch (Exception e) {
            Log.e("Error importing from OpenCaching.com", e);
            return Collections.emptyList();
        }
        for (cgCache cache : caches) {
            cache.setUpdated(System.currentTimeMillis());
            if (isDetailed) {
                cache.setDetailedUpdate(cache.getUpdated());
                cache.setDetailed(true);
            }

            // save full detailed caches
            cgeoapplication.getInstance().saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
        }
        return caches;
    }

    public static Collection<cgCache> searchByCenter(final Geopoint center) {
        final HttpResponse response = Network.getRequest("http://www.opencaching.com/api/geocache/.gpx",
                new Parameters(
                        "Authorization", DEV_KEY,
                        "log_limit", "0",
                        "hint", "false",
                        "description", "none",
                        "limit", "10",
                        "center", center.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA)));
        return importCachesFromResponse(response, false);
    }


}
