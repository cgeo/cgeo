package cgeo.geocaching.apps.cache;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.apps.cache.navi.CacheNavigationApp;

import android.app.Activity;
import android.content.Intent;

abstract class AbstractGeneralApp extends AbstractApp implements CacheNavigationApp {

    protected AbstractGeneralApp(final String name, final int id, final String packageName) {
        super(name, id, null, packageName);
    }

    @Override
    public void navigate(Activity activity, Geocache cache) {
        final Intent intent = getLaunchIntent();
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
        }
    }
}
