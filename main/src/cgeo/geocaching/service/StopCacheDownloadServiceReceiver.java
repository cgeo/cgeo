package cgeo.geocaching.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopCacheDownloadServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        CacheDownloaderService.requestStopService();
    }
}
