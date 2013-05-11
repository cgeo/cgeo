package cgeo.geocaching;

import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.Version;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends AbstractActivity {
    @InjectView(R.id.about_version_string) protected TextView version;
    @InjectView(R.id.contributors) protected TextView contributors;
    @InjectView(R.id.changelog) protected TextView changeLog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.about_activity);

        version.setText(Version.getVersionName(this));
        contributors.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
        changeLog.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void donate(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void support(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:support@cgeo.org")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void website(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void facebook(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/pages/cgeo/297269860090")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void twitter(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/android_gc")));
    }

    /**
     * @param view
     *            unused here but needed since this method is referenced from XML layout
     */
    public void nutshellmanual(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }
}
