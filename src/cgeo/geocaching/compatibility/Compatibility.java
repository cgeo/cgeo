package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.view.Display;
import android.view.Surface;

public final class Compatibility {

	private static AndroidLevel8 level8;
	private static boolean initialized = false;

	private static AndroidLevel8 getLevel8() {
		if (!initialized) {
			try {
				final int sdk = new Integer(Build.VERSION.SDK).intValue();
				if (sdk >= 8) {
					level8 = new AndroidLevel8();
				}
			} catch (Exception e) {
				// nothing
			}
			initialized = true;
		}
		return level8;
	}

	public static Double getDirectionNow(final Double directionNowPre,
			final Activity activity) {
		AndroidLevel8 level8 = getLevel8();

		if (level8 != null) {
			final int rotation = level8.getRotation(activity);
			if (rotation == Surface.ROTATION_90) {
				return directionNowPre + 90;
			} else if (rotation == Surface.ROTATION_180) {
				return directionNowPre + 180;
			} else if (rotation == Surface.ROTATION_270) {
				return directionNowPre + 270;
			}
		} else {
			final Display display = activity.getWindowManager()
					.getDefaultDisplay();
			final int rotation = display.getOrientation();
			if (rotation == Configuration.ORIENTATION_LANDSCAPE) {
				return directionNowPre + 90;
			}
		}
		return directionNowPre;
	}

	public static Uri getCalendarProviderURI() {
		final int sdk = new Integer(Build.VERSION.SDK).intValue();
		if (sdk >= 8) {
			return Uri.parse("content://com.android.calendar/calendars");
		} else {
			return Uri.parse("content://calendar/calendars");
		}
	}

	public static Uri getCalenderEventsProviderURI() {
		final int sdk = new Integer(Build.VERSION.SDK).intValue();
		if (sdk >= 8) {
			return Uri.parse("content://com.android.calendar/events");
		} else {
			return Uri.parse("content://calendar/events");
		}
	}

}
