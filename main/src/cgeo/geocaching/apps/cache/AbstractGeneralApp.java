package cgeo.geocaching.apps.cache;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.apps.navi.CacheNavigationApp;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

abstract class AbstractGeneralApp extends AbstractApp implements CacheNavigationApp {

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    protected AbstractGeneralApp(@NonNull final String name, @NonNull final String packageName) {
        super(name, null, packageName);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geocache cache) {
        final Intent intent = getLaunchIntent();
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
        }
    }
}
