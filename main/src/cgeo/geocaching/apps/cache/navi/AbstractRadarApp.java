package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;
import android.content.Intent;

abstract class AbstractRadarApp extends AbstractPointNavigationApp {

    protected static final String RADAR_EXTRA_LONGITUDE = "longitude";
    protected static final String RADAR_EXTRA_LATITUDE = "latitude";

    private final String intentAction;

    protected AbstractRadarApp(final String name, final int id, final String intent, final String packageName) {
        super(name, id, intent, packageName);
        this.intentAction = intent;
    }

    private Intent createIntent(final Geopoint point) {
        final Intent intent = new Intent(intentAction);
        addCoordinates(intent, point);
        return intent;
    }

    @Override
    public void navigate(final Activity activity, final Geopoint point) {
        activity.startActivity(createIntent(point));
    }

    @Override
    public void navigate(final Activity activity, final Geocache cache) {
        final Intent intent = createIntent(cache.getCoords());
        addIntentExtras(intent, cache);
        activity.startActivity(intent);
    }

    @Override
    public void navigate(final Activity activity, final Waypoint waypoint) {
        final Intent intent = createIntent(waypoint.getCoords());
        addIntentExtras(intent, waypoint);
        activity.startActivity(intent);
    }

    protected abstract void addCoordinates(final Intent intent, final Geopoint point);
}
