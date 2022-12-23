package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.utils.ProcessUtils;
import cgeo.geocaching.utils.ShareUtils;
import static cgeo.geocaching.utils.ProcessUtils.isChromeLaunchable;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;


public class StartWebviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String url = getIntent().getDataString();
        if (url != null) {
            if (isChromeLaunchable()) {
                ShareUtils.openCustomTab(this, url);
            } else {
                final List<ResolveInfo> browsers = ProcessUtils.getInstalledBrowsers(this);
                if (browsers.size() > 0) {
                    final ResolveInfo resolveInfo = browsers.get(0);

                    final Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    launchIntent.setPackage(resolveInfo.activityInfo.packageName);

                    startActivity(launchIntent);
                } else {
                    ActivityMixin.showShortToast(this, R.string.no_browser_found);
                }
            }
        }
        finish();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
