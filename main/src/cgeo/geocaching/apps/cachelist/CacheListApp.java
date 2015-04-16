package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.App;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

import java.util.List;

public interface CacheListApp extends App {

    boolean invoke(final @NonNull List<Geocache> caches,
            final @NonNull Activity activity, @NonNull final SearchResult search);

}
