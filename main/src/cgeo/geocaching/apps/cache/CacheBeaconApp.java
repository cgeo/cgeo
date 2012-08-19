package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheAttribute;

public class CacheBeaconApp extends AbstractGeneralApp {

    protected CacheBeaconApp() {
        super(getString(R.string.cache_menu_cachebeacon), "de.fun2code.android.cachebeacon");
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        if (cache == null) {
            return false;
        }
        return cache.hasAttribute(CacheAttribute.WIRELESS_BEACON, true);
    }

}
