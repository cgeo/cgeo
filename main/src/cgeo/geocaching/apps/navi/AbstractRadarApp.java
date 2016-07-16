package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.location.Geopoint;

import android.support.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;

abstract class AbstractRadarApp extends AbstractPointNavigationApp {

    protected static final String RADAR_EXTRA_LONGITUDE = "longitude";
    protected static final String RADAR_EXTRA_LATITUDE = "latitude";

    private final String intentAction;

    protected AbstractRadarApp(final String name, final String intent, final String packageName) {
        super(name, intent, packageName);
        this.intentAction = intent;
    }

    private Intent createIntent(final Geopoint point) {
        final Intent intent = new Intent(intentAction);
        addCoordinates(intent, point);
        return intent;
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geopoint point) {
        activity.startActivity(createIntent(point));
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geocache cache) {
        final Intent intent = createIntent(cache.getCoords());
        addIntentExtras(intent, cache);
        activity.startActivity(intent);
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint) {
        final Intent intent = createIntent(waypoint.getCoords());
        addIntentExtras(intent, waypoint);
        activity.startActivity(intent);
    }

    protected abstract void addCoordinates(final Intent intent, final Geopoint point);
}
