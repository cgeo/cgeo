package cgeo.geocaching.settings;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

class MapDownloadNotificationReceiver extends BroadcastReceiver {

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
                        final int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        switch (status) {
                            case DownloadManager.STATUS_SUCCESSFUL:
                                PendingDownload.remove(pendingDownload);
                                final Intent copyFileIntent = new Intent(context, ReceiveMapFileActivity.class);
                                final Uri uri = downloadManager.getUriForDownloadedFile(pendingDownload);
                                copyFileIntent.setData(uri);
                                copyFileIntent.putExtra(ReceiveMapFileActivity.EXTRA_FILENAME, p.getFilename());
                                copyFileIntent.putExtra(MapDownloadUtils.RESULT_CHOSEN_URL, p.getRemoteUrl());
                                copyFileIntent.putExtra(MapDownloadUtils.RESULT_DATE, p.getDate());
                                copyFileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(copyFileIntent);
                                Log.d("download #" + pendingDownload + " successful");
                                break;
                            case DownloadManager.STATUS_FAILED:
                                PendingDownload.remove(pendingDownload);
                                final int error = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                                ActivityMixin.showToast(context, "map download failed with error #" + error);
                                Log.d("download #" + pendingDownload + " failed with error #" + error);
                                break;
                            default:
                                // ignore unknown state by logging silently
                                Log.d("download #" + pendingDownload + ": unknown state #" + status);
                                break;
                        }
                    }
                }
            }
        }
    }
}
