package cgeo.geocaching.apps.cachelist;

import android.app.Activity;
import android.support.annotation.NonNull;

import java.util.List;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.models.Geocache;

public interface CacheListApp extends App {

    void invoke(@NonNull final List<Geocache> caches,
            @NonNull final Activity activity, @NonNull final SearchResult search);

}
