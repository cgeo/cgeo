package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;

import android.content.res.Resources;

class GccApp extends AbstractGeneralApp {
    GccApp(final Resources res) {
        super(res.getString(R.string.cache_menu_gcc), "eisbehr.gcc");
    }
}
