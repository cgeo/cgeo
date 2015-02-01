package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

public class OfflineLogFilter extends AbstractFilter {

    protected OfflineLogFilter() {
        super(R.string.caches_filter_offline_log);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.isLogOffline();
    }

}
