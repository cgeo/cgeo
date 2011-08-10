package cgeo.geocaching;

import java.util.Locale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import cgeo.geocaching.activity.AbstractActivity;

public class cgeohelpers extends AbstractActivity {

	private Resources res = null;
	private cgSettings settings = null;
	private SharedPreferences prefs = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		res = this.getResources();
		prefs = getSharedPreferences(cgSettings.preferences, 0);
		settings = new cgSettings(this, prefs);

		setTheme();
		setContentView(R.layout.helpers);
		setTitle(res.getString(R.string.helpers));
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
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:gnu.android.app.cgeomanual.de")));
			} else {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:gnu.android.app.cgeomanual.en")));
			}
		} catch (Exception e) {
			// market not available in standard emulator
		}


		finish();
	}

	public void installLocus(View view) {
		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:menion.android.locus")));
		} catch (Exception e) {
			// market not available in standard emulator
		}


		finish();
	}

	public void installGpsStatus(View view) {
		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.eclipsim.gpsstatus2")));
		} catch (Exception e) {
			// market not available in standard emulator
		}

		finish();
	}

	public void installBluetoothGps(View view) {
		try {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:googoo.android.btgps")));
		} catch (Exception e) {
			// market not available in standard emulator
		}

		finish();
	}
}
