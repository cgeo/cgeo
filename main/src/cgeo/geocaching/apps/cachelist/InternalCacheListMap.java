package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.maps.CGeoMap;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

import java.util.List;

class InternalCacheListMap extends AbstractApp implements CacheListApp {

    InternalCacheListMap() {
        super(getString(R.string.cache_menu_map), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public boolean invoke(@NonNull final List<Geocache> caches, @NonNull final Activity activity, @NonNull final SearchResult search) {
        CGeoMap.startActivitySearch(activity, search, null);
        return true;
    }
}
