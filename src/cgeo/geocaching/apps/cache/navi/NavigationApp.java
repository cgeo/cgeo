package cgeo.geocaching.apps.cache.navi;

import java.util.UUID;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.App;
import cgeo.geocaching.geopoint.Geopoint;

interface NavigationApp extends App {
	public boolean invoke(final cgGeo geo, final Activity activity,
			final Resources res,
			final cgCache cache,
			final UUID searchId, final cgWaypoint waypoint,
			final Geopoint coords);
}
