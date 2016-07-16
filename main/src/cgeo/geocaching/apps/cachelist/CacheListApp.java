package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.App;

import android.support.annotation.NonNull;

import android.app.Activity;

import java.util.List;

public interface CacheListApp extends App {

    boolean invoke(@NonNull final List<Geocache> caches,
            @NonNull final Activity activity, @NonNull final SearchResult search);

}
