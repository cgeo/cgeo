package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.App;

import android.app.Activity;

import java.util.List;

public interface CacheListApp extends App {

    boolean invoke(final List<Geocache> caches,
            final Activity activity, final SearchResult search);

}
