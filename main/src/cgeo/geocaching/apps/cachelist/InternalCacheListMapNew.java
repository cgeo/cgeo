package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;
import android.support.annotation.NonNull;

import java.util.List;

class InternalCacheListMapNew extends AbstractApp implements CacheListApp {

    InternalCacheListMapNew() {
        super(getString(R.string.cache_menu_mfbeta), null);
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public boolean invoke(@NonNull final List<Geocache> caches, @NonNull final Activity activity, @NonNull final SearchResult search) {
        NewMap.startActivitySearch(activity, search, null);
        return true;
    }
}
