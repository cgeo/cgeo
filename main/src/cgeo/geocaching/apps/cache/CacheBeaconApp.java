package cgeo.geocaching.apps.cache;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheAttribute;

public class CacheBeaconApp extends AbstractGeneralApp {

    public CacheBeaconApp() {
        super(getString(R.string.cache_menu_cachebeacon), "de.fun2code.android.cachebeacon");
    }

    @Override
    public boolean isEnabled(Geocache cache) {
        return cache.hasAttribute(CacheAttribute.WIRELESSBEACON, true);
    }

}
