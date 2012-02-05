package cgeo.geocaching.apps;

import android.view.MenuItem;

public abstract class AbstractAppFactory {

    protected static App getAppFromMenuItem(MenuItem item, final App[] availableApps) {
        final int id = item.getItemId();
        for (App app : availableApps) {
            if (app.getId() == id) {
                return app;
            }
        }
        return null;
    }
}
