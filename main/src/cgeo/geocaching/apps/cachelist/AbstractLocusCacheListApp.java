package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.apps.AbstractLocusApp;
import cgeo.geocaching.models.Geocache;

import org.apache.commons.collections4.CollectionUtils;
import android.support.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;

import java.util.List;

abstract class AbstractLocusCacheListApp extends AbstractLocusApp implements CacheListApp {

    private final boolean export;

    AbstractLocusCacheListApp(final boolean export) {
        super(getString(export ? R.string.caches_map_locus_export : R.string.caches_map_locus), Intent.ACTION_VIEW);
        this.export = export;
    }

    /**
     * show caches in Locus
     *
     * @see AbstractLocusApp#showInLocus
     */
    @Override
    public boolean invoke(@NonNull final List<Geocache> cacheList, @NonNull final Activity activity, @NonNull final SearchResult search) {
        if (CollectionUtils.isEmpty(cacheList)) {
            return false;
        }

        showInLocus(cacheList, false, export, activity);

        return true;
    }

}
