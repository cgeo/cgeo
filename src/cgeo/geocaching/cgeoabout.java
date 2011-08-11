package cgeo.geocaching;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import cgeo.geocaching.activity.AbstractActivity;

public class cgeoabout extends AbstractActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme();
		setContentView(R.layout.about);
		setTitle(res.getString(R.string.about));

		init();
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void init() {
		try {
			PackageManager manager = this.getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);

			setTitle(res.getString(R.string.about) + " (ver. " + info.versionName + ")");

			manager = null;

			((TextView)findViewById(R.id.contributors)).setMovementMethod(LinkMovementMethod.getInstance());
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgeoabout.init: Failed to obtain package version.");
		}
	}

	public void donateMore(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
		//activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=N2FKGNCPPRUVE")));
		//activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=2Z69QWLRCBE9N&lc=US&item_name=c%3ageo&currency_code=EUR&amount=15&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted")));
	}

	public void donateLess(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
		//activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=4PRD9CX4Y8XR6")));
		//activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=2Z69QWLRCBE9N&lc=US&item_name=c%3ageo&currency_code=EUR&amount=7&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted")));
	}

	public void author(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://carnero.cc/")));
	}

	public void support(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:support@cgeo.org")));
	}

	public void website(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
	}

	public void facebook(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/pages/cgeo/297269860090")));
	}

	public void twitter(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/android_gc")));
	}

	public void nutshellmanual(View view) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
	}
}
