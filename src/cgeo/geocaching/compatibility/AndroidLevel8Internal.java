package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.view.Display;

class AndroidLevel8Internal {

	public AndroidLevel8Internal() {
	}

	public static int getRotation(final Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		return display.getRotation();
	}
}
