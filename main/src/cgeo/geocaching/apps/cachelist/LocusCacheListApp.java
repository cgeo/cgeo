package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractLocusApp;

import org.apache.commons.collections.CollectionUtils;

import android.app.Activity;
import android.content.res.Resources;

import java.util.List;
import java.util.UUID;

class LocusCacheListApp extends AbstractLocusApp implements CacheListApp {

    LocusCacheListApp(Resources res) {
        super(res);
    }

    /**
     * show caches in Locus
     *
     * @see AbstractLocusApp#showInLocus
     * @author koem
     */
    @Override
    public boolean invoke(cgGeo geo, List<cgCache> cacheList, Activity activity, Resources res,
            final UUID searchId) {
        if (CollectionUtils.isEmpty(cacheList))
            return false;

        showInLocus((List<? extends Object>) cacheList, false, activity);

        return true;
    }

}
