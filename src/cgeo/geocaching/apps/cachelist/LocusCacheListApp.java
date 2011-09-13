package cgeo.geocaching.apps.cachelist;

import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.content.res.Resources;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgGeo;
import cgeo.geocaching.apps.AbstractLocusApp;

class LocusCacheListApp extends AbstractLocusApp implements CacheListApp {

	LocusCacheListApp(Resources res) {
		super(res);
	}

	/**
	 * show caches in Locus
	 * 
     * @see AbstractLocusApp#showInLocus
     * @author koem
	 */
	@Override
	public boolean invoke(cgGeo geo, List<cgCache> cacheList, Activity activity, Resources res, 
			final UUID searchId) {
		if (cacheList == null || cacheList.isEmpty()) return false;
		
		this.showInLocus((List<? extends Object>) cacheList, false, activity);

		return true;
	}

}
