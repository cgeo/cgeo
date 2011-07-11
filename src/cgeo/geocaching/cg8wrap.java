package cgeo.geocaching;

import android.app.Activity;

public class cg8wrap {
	static {
		try {
			Class.forName("cgeo.geocaching.cg8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private cg8 cg8;

	public static void check() {
		// nothing
	}

	public cg8wrap(Activity activityIn) {
		cg8 = new cg8(activityIn);
	}

	public int getRotation() {
		return cg8.getRotation();
	}
}
