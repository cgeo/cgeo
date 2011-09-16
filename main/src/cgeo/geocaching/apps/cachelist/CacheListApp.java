package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.App;

import android.app.Activity;
import android.content.res.Resources;

import java.util.List;
import java.util.UUID;

interface CacheListApp extends App {

    boolean invoke(final cgGeo geo, final List<cgCache> caches,
            final Activity activity, final Resources res,
            final UUID searchId);

}
