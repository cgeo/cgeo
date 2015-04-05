package cgeo.geocaching.apps.cache;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.apps.cache.navi.CacheNavigationApp;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;

abstract class AbstractGeneralApp extends AbstractApp implements CacheNavigationApp {

    protected AbstractGeneralApp(@NonNull final String name, @NonNull final String packageName) {
        super(name, null, packageName);
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        final Intent intent = getLaunchIntent();
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
        }
    }
}
