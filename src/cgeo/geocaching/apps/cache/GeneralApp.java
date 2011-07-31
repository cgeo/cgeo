package cgeo.geocaching.apps.cache;

import android.app.Activity;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.apps.App;

interface GeneralApp extends App {

	boolean isEnabled(final cgCache cache);

	public boolean invoke(Activity activity, cgCache cache);

}
