package cgeo.geocaching.settings;

import cgeo.geocaching.utils.ShareUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShareBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String url = intent.getDataString();
        if (url != null) {
            ShareUtils.openUrl(context, url, true);
        }
    }
}
