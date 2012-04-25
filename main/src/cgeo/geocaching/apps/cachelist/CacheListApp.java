package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.IGeoData;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.cgCache;

import android.app.Activity;

import java.util.List;

interface CacheListApp extends App {

    boolean invoke(final IGeoData geo, final List<cgCache> caches,
            final Activity activity, final SearchResult search);

}
