package cgeo.geocaching.apps.cache;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.apps.AbstractApp;

import android.app.Activity;
import android.content.Intent;

abstract class AbstractGeneralApp extends AbstractApp implements GeneralApp {

    protected AbstractGeneralApp(String name, String packageName) {
        super(name, null, packageName);
    }

    @Override
    public boolean invoke(Activity activity, cgCache cache) {
        final Intent intent = getLaunchIntent();
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            return true;
        }
        return false;
    }
}
