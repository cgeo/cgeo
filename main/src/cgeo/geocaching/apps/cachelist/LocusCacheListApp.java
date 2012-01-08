package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractLocusApp;

import org.apache.commons.collections.CollectionUtils;

import android.app.Activity;

import java.util.List;

class LocusCacheListApp extends AbstractLocusApp implements CacheListApp {

    /**
     * show caches in Locus
     *
     * @see AbstractLocusApp#showInLocus
     * @author koem
     */
    @Override
    public boolean invoke(cgGeo geo, List<cgCache> cacheList, Activity activity, final SearchResult search) {
        if (CollectionUtils.isEmpty(cacheList)) {
            return false;
        }

        showInLocus(cacheList, false, activity);

        return true;
    }

}
