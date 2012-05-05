package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.apps.App;

import android.app.Activity;

import java.util.List;

interface CacheListApp extends App {

    boolean invoke(final List<cgCache> caches,
            final Activity activity, final SearchResult search);

}
