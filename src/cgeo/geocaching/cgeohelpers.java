package cgeo.geocaching;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import java.util.Locale;

public class cgeohelpers extends Activity {

	private cgeoapplication app = null;
	private Resources res = null;
	private Activity activity = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private SharedPreferences prefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		prefs = getSharedPreferences(cgSettings.preferences, 0);
		settings = new cgSettings(this, prefs);
		base = new cgBase(app, settings, prefs);
		warning = new cgWarning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.helpers);
		base.setTitle(activity, res.getString(R.string.helpers));
	}

	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();
	}

	public void installManual(View view) {
		final Locale loc = Locale.getDefault();
		final String lng = loc.getLanguage();

		try {
			if (lng.equalsIgnoreCase("de")) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:gnu.android.app.cgeomanual.de")));
			} else {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:gnu.android.app.cgeomanual.en")));
			}
		} catch (Exception e) {
			// market not available in standard emulator
		}


		finish();
	}

	public void installLocus(View view) {
		try {
			activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:menion.android.locus")));
		} catch (Exception e) {
			// market not available in standard emulator
		}


		finish();
	}

	public void installGpsStatus(View view) {
		try {
			activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.eclipsim.gpsstatus2")));
		} catch (Exception e) {
			// market not available in standard emulator
		}

		finish();
	}

	public void installBluetoothGps(View view) {
		try {
			activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:googoo.android.btgps")));
		} catch (Exception e) {
			// market not available in standard emulator
		}

		finish();
	}

	public void goHome(View view) {
		base.goHome(activity);
	}
}
