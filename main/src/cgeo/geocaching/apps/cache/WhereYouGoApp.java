package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

class WhereYouGoApp extends AbstractGeneralApp {
    WhereYouGoApp() {
        super(getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache != null && cache.getType() == CacheType.WHERIGO;
    }
}
