package cgeo.geocaching.apps.cache;

import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.apps.navi.CacheNavigationApp;
import cgeo.geocaching.models.Geocache;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

abstract class AbstractGeneralApp extends AbstractApp implements CacheNavigationApp {

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    protected AbstractGeneralApp(@NonNull final String name, @NonNull final String packageName) {
        super(name, null, packageName);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final Intent intent = getLaunchIntent();
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(intent);
        }
    }
}
