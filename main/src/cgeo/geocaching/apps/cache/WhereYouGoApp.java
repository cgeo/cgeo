package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheType;

public class WhereYouGoApp extends AbstractGeneralApp {
    public WhereYouGoApp() {
        super(getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache.getType() == CacheType.WHERIGO;
    }
}
