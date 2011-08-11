package cgeo.geocaching.activity;

import gnu.android.app.appmanualclient.AppManualReaderClient;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cgeo.geocaching.R;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeo;

public final class ActivityMixin {
	public final static void goHome(final Activity fromActivity) {
		final Intent intent = new Intent(fromActivity, cgeo.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		fromActivity.startActivity(intent);
		fromActivity.finish();
	}

	public final static void goManual(final Context context, final String helpTopic) {
		if (helpTopic == null || helpTopic.length() == 0) {
			return;
		}
		try {
			AppManualReaderClient.openManual(
					"c-geo",
					helpTopic,
					context,
					"http://manual.cgeo.org/");
		} catch (Exception e) {
			// nothing
		}
	}

	public final static void setTitle(final Activity activity, final String text) {
		if (text == null) {
			return;
		}

		final TextView title = (TextView) activity.findViewById(R.id.actionbar_title);
		if (title != null) {
			title.setText(text);
		}
	}

	public final static void showProgress(final Activity activity, final boolean show) {
		if (activity == null) {
			return;
		}

		final ProgressBar progress = (ProgressBar) activity.findViewById(R.id.actionbar_progress);
		if (show) {
			progress.setVisibility(View.VISIBLE);
		} else {
			progress.setVisibility(View.GONE);
		}
	}

	public final static void setTheme(final Activity activity) {
		cgSettings settings = new cgSettings(activity, activity.getSharedPreferences(cgSettings.preferences, 0));
		if (settings.skin == 1) {
			activity.setTheme(R.style.light);
		} else {
			activity.setTheme(R.style.dark);
		}
	}

	public final static void showToast(final Activity activity, final String text) {
		if (text.length() > 0) {
			Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);

			toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 100);
			toast.show();
		}
	}

	public final static void showShortToast(final Activity activity, final String text) {
		if (text.length() > 0) {
			Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);

			toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 100);
			toast.show();
		}
	}

	public static final void helpDialog(final Activity activity, final String title, final String message) {
		if (message == null || message.length() == 0) {
			return;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setCancelable(true);
		dialog.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
           }
       });

	   AlertDialog alert = dialog.create();
	   alert.show();
	}

}
