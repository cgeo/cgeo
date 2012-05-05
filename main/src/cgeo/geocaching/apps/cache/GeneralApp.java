package cgeo.geocaching.apps.cache;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.apps.App;

import android.app.Activity;

interface GeneralApp extends App {

    public boolean invoke(Activity activity, cgCache cache);

}
