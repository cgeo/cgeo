package cgeo.geocaching.apps.cache.navi;

import android.content.Context;

abstract class AbstractInternalMap extends AbstractNavigationApp {

	protected AbstractInternalMap(String name, String intent) {
		super(name, intent);
	}

	@Override
	public boolean isInstalled(Context context) {
		return true;
	}
}
