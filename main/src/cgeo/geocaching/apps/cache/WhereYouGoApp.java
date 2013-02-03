package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;

public class WhereYouGoApp extends AbstractGeneralApp {
    public WhereYouGoApp() {
        super(getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(Geocache cache) {
        return cache.getType() == CacheType.WHERIGO;
    }
}
