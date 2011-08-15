package cgeo.geocaching.apps.cache.navi;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.apps.App;

interface NavigationApp extends App {
	public boolean invoke(final cgGeo geo, final Activity activity,
			final Resources res,
			final cgCache cache,
			final Long searchId, final cgWaypoint waypoint,
			final Double latitude, final Double longitude);
}
