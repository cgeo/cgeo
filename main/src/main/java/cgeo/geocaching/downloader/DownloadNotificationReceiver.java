package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.utils.Log;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class DownloadNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            final long pendingDownload = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            final PendingDownload p = PendingDownload.load(pendingDownload);
            if (null != p) {
                final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                if (null != downloadManager) {
                    final DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(pendingDownload);
                    final Cursor cursor = downloadManager.query(query);
                    if (cursor.moveToFirst()) {
                        try {
                            final int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            switch (status) {
                                case DownloadManager.STATUS_SUCCESSFUL:
                                    DownloaderUtils.startReceive(context, downloadManager, pendingDownload, p);
                                    Log.d("download #" + pendingDownload + " successful");
                                    break;
                                case DownloadManager.STATUS_FAILED:
                                    PendingDownload.remove(pendingDownload);
                                    final int idx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                    if (idx >= 0) {
                                        final int error = cursor.getInt(idx);
                                        ActivityMixin.showToast(context, String.format(context.getString(R.string.download_error), error));
                                        Log.d("download #" + pendingDownload + " failed with error #" + error);
                                    } else {
                                        Log.e("download #" + pendingDownload + " failed with unknown error");
                                    }
                                    // remove file from system's download manager, which will also delete the broken file from storage
                                    downloadManager.remove(pendingDownload);
                                    break;
                                default:
                                    // ignore unknown state by logging silently
                                    Log.d("download #" + pendingDownload + ": unknown state #" + status);
                                    break;
                            }
                        } catch (final IllegalArgumentException e) {
                            Log.e("no download state", e);
                        }
                    }
                }
            }
        }
    }
}
