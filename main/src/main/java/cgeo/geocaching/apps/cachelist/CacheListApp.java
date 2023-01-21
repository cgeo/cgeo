package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.List;

public interface CacheListApp extends App {

    void invoke(@NonNull List<Geocache> caches,
                @NonNull Activity activity, @NonNull SearchResult search);

}
