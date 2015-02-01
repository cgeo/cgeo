package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

public class OfflineLogFilter extends AbstractFilter {

    protected OfflineLogFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_offline_log));
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.isLogOffline();
    }

}
