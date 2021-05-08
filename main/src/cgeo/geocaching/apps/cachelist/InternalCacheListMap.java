package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.List;

class InternalCacheListMap extends AbstractApp implements CacheListApp {

    final Class<?> cls;

    InternalCacheListMap(final Class<?> cls, final int name) {
        super(getString(name), null);
        this.cls = cls;
    }

    InternalCacheListMap() {
        super(getString(R.string.cache_menu_map), null);
        cls = null;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void invoke(@NonNull final List<Geocache> caches, @NonNull final Activity activity, @NonNull final SearchResult search) {
        DefaultMap.startActivitySearch(activity, cls != null ? cls : Settings.getMapProvider().getMapClass(), search, null, StoredList.TEMPORARY_LIST.id);
    }
}
