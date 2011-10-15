package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.maps.CGeoMap;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

import java.util.List;
import java.util.UUID;

class InternalCacheListMap extends AbstractApp implements CacheListApp {

    InternalCacheListMap(Resources res) {
        super(res.getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean isInstalled(Context context) {
        return true;
    }

    @Override
    public boolean invoke(cgGeo geo, List<cgCache> caches, Activity activity, Resources res, final UUID searchId) {
        CGeoMap.startActivitySearch(activity, searchId, null, false);
        return true;
    }
}
