package cgeo.geocaching.apps.cache;

import android.content.res.Resources;
import cgeo.geocaching.R;

class GccApp extends AbstractGeneralApp implements GeneralApp {
	GccApp(final Resources res) {
		super(res.getString(R.string.cache_menu_gcc), "eisbehr.gcc");
	}
}
