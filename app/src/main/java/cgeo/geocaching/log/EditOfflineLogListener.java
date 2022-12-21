package cgeo.geocaching.log;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.models.Geocache;

import android.view.View;
import android.view.View.OnClickListener;

class EditOfflineLogListener implements OnClickListener {

    private final Geocache cache;
    private final CacheDetailActivity activity;

    EditOfflineLogListener(final Geocache cache, final CacheDetailActivity activity) {
        this.cache = cache;
        this.activity = activity;
    }

    @Override
    public void onClick(final View v) {
        activity.setNeedsRefresh();
        cache.logVisit(activity);
    }

}
