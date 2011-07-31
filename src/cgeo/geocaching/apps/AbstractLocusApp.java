package cgeo.geocaching.apps;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import cgeo.geocaching.R;

public abstract class AbstractLocusApp extends AbstractApp {
	private static final String INTENT = Intent.ACTION_VIEW;

	protected AbstractLocusApp(final Resources res) {
		super(res.getString(R.string.caches_map_locus), INTENT);
	}

	@Override
	public boolean isInstalled(final Context context) {
		final Intent intentTest = new Intent(INTENT);
		intentTest.setData(Uri.parse("menion.points:x"));
		return isIntentAvailable(context, intentTest);
	}


}
