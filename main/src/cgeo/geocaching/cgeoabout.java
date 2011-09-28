package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

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

            ((TextView) findViewById(R.id.contributors)).setMovementMethod(LinkMovementMethod.getInstance());
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgeoabout.init: Failed to obtain package version.");
        }
    }

    public void donateMore() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
    }

    public void donateLess() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
    }

    public void author() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://carnero.cc/")));
    }

    public void support() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:support@cgeo.org")));
    }

    public void website() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }

    public void facebook() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/pages/cgeo/297269860090")));
    }

    public void twitter() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/android_gc")));
    }

    public void nutshellmanual() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }
}
