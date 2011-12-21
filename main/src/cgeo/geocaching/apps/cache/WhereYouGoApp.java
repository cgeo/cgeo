package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

import android.content.res.Resources;

class WhereYouGoApp extends AbstractGeneralApp {
    WhereYouGoApp(Resources res) {
        super(res.getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache != null && cache.getType() == CacheType.WHERIGO;
    }
}
