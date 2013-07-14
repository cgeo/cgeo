package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.ui.AnchorAwareLinkMovementMethod;
import cgeo.geocaching.utils.Version;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;

import org.apache.commons.lang3.StringUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

@EActivity
public class AboutActivity extends AbstractActivity {
    @ViewById(R.id.about_version_string) protected TextView version;
    @ViewById(R.id.contributors) protected TextView contributors;
    @ViewById(R.id.changelog_master) protected TextView changeLogMaster;
    @ViewById(R.id.changelog_release) protected TextView changeLogRelease;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO remove this after the theme has been fixed
        super.onCreate(savedInstanceState, R.layout.about_activity);
    }

    @AfterViews
    void initializeViews() {
        version.setText(Version.getVersionName(this));
        contributors.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
        changeLogRelease.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
        final String changeLogMasterString = getString(R.string.changelog_master);
        if (StringUtils.isBlank(changeLogMasterString)) {
            changeLogMaster.setVisibility(View.GONE);
        } else {
            changeLogMaster.setMovementMethod(AnchorAwareLinkMovementMethod.getInstance());
        }
    }

    @Click(R.id.donate)
    public void donate() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=FMLNN8GXZKJEE")));
    }

    @Click(R.id.support)
    public void support() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:support@cgeo.org")));
    }


    @Click(R.id.website)
    void webSite() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.cgeo.org/")));
    }

    @Click(R.id.facebook)
    public void facebook() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/pages/cgeo/297269860090")));
    }

    @Click(R.id.twitter)
    public void twitter() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/android_gc")));
    }

    @Click(R.id.nutshellmanual)
    public void nutshellmanual() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://manual.cgeo.org/")));
    }

    @Click(R.id.market)
    public void market() {
        final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(marketIntent);
    }
}
