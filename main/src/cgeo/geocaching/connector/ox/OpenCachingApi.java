package cgeo.geocaching.connector.ox;

import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpResponse;

import java.util.Collection;
import java.util.EnumSet;

public class OpenCachingApi {

    private static final String DEV_KEY = CryptUtils.rot13("PtqQnHo9RUTht3Np");

    public static cgCache searchByGeoCode(final String geocode) {
        final HttpResponse response = Network.getRequest("http://www.opencaching.com/api/geocache/" + geocode + ".gpx", new Parameters("Authorization", DEV_KEY));
        if (response == null) {
            return null;
        }
        Collection<cgCache> caches = null;
        try {
            caches = new GPX10Parser(StoredList.STANDARD_LIST_ID).parse(response.getEntity().getContent(), null);
        } catch (Exception e) {
            Log.e("Error importing from OpenCaching.com", e);
        }
        if (caches != null && CollectionUtils.isNotEmpty(caches)) {
            final cgCache cache = caches.iterator().next();
            cache.setUpdated(System.currentTimeMillis());
            cache.setDetailedUpdate(cache.getUpdated());
            cache.setDetailed(true);

            // save full detailed caches
            cgeoapplication.getInstance().saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
            return cache;
        }

        return null;
    }

}
