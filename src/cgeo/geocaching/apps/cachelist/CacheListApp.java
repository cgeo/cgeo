package cgeo.geocaching.apps.cachelist;

import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.App;

interface CacheListApp extends App {

	boolean invoke(final cgGeo geo, final List<cgCache> caches,
			final Activity activity, final Resources res,
			final UUID searchId);

}
