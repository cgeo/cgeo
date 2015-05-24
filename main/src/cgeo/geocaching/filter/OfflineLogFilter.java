package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

public class OfflineLogFilter extends AbstractFilter {

    private static final long serialVersionUID = -1770444119861730428L;

    protected OfflineLogFilter() {
        super(R.string.caches_filter_offline_log);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.isLogOffline();
    }

}
