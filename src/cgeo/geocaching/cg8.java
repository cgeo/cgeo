package cgeo.geocaching;

import android.app.Activity;
import android.view.Display;

public class cg8 {
	private Activity  activity = null;

	public cg8(Activity activityIn) {
		activity = activityIn;
	}

	public int getRotation() {
		Display display = activity.getWindowManager().getDefaultDisplay();
		return display.getRotation();
	}
}
