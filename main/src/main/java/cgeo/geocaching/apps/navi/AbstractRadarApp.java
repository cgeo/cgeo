package cgeo.geocaching.apps.navi;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

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
    public void navigate(@NonNull final Context context, @NonNull final Geopoint point) {
        context.startActivity(createIntent(point));
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final Intent intent = createIntent(cache.getCoords());
        addIntentExtras(intent, cache);
        context.startActivity(intent);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        final Intent intent = createIntent(waypoint.getCoords());
        addIntentExtras(intent, waypoint);
        context.startActivity(intent);
    }

    protected abstract void addCoordinates(Intent intent, Geopoint point);
}
