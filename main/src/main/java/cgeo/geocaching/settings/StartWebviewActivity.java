package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.ProcessUtils.isChromeLaunchable;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;


public class StartWebviewActivity extends AbstractActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String url = getIntent().getDataString();
        if (url != null) {
            openUrl(url);
        }
        finish();
    }

    /**
     * Open the URL in a custom tab (Chrome) or in the first installed browser.
     * Never let an ActivityNotFoundException crash this activity (e.g. if no browser is able to handle the URI).
     */
    private void openUrl(final String url) {
        if (isChromeLaunchable()) {
            try {
                ShareUtils.openCustomTab(this, url);
                return;
            } catch (final ActivityNotFoundException e) {
                Log.w("Cannot open URL '" + url + "' in custom tab, trying other browsers", e);
            }
        }

        final List<ResolveInfo> browsers = ProcessUtils.getInstalledBrowsers(this);
        if (browsers.isEmpty()) {
            ActivityMixin.showShortToast(this, R.string.no_browser_found);
            return;
        }

        final ResolveInfo resolveInfo = browsers.get(0);
        final Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        launchIntent.setPackage(resolveInfo.activityInfo.packageName);
        try {
            startActivity(launchIntent);
        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable browser for URL '" + url + "'", e);
            ActivityMixin.showShortToast(this, R.string.err_application_no);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
