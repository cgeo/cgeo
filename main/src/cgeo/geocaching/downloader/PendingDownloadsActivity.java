package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.utils.Formatter.formatBytes;
import static cgeo.geocaching.utils.Formatter.formatDateForFilename;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.material.button.MaterialButton;
import io.noties.markwon.Markwon;

public class PendingDownloadsActivity extends AbstractActionBarActivity {

    RecyclerView recyclerView;
    PendingDownloadsAdapter adapter;
    DownloadManager downloadManager;
    ArrayList<PendingDownload.PendingDownloadDescriptor> pendingDownloads;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_recyclerview);
        setTitle(R.string.debug_current_downloads);
        fillAdapter();
    }

    private String formatStatus(final int statusCode) {
        int value = 0;
        if (statusCode == DownloadManager.STATUS_FAILED) {
            value = R.string.asdm_status_failed;
        } else if (statusCode == DownloadManager.STATUS_PAUSED) {
            value = R.string.asdm_status_paused;
        } else if (statusCode == DownloadManager.STATUS_PENDING) {
            value = R.string.asdm_status_pending;
        } else if (statusCode == DownloadManager.STATUS_RUNNING) {
            value = R.string.asdm_status_running;
        } else if (statusCode == DownloadManager.STATUS_SUCCESSFUL) {
            value = R.string.asdm_status_successful;
        }
        return (value > 0 ? getString(value) + " (" : "(") + statusCode + ")";
    }

    private String formatReason(final int reasonCode) {
        int value = 0;
        if (reasonCode == DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
            value = R.string.asdm_paused_queued_for_wifi;
        } else if (reasonCode == DownloadManager.PAUSED_UNKNOWN) {
            value = R.string.asdm_paused_unknown;
        } else if (reasonCode == DownloadManager.PAUSED_WAITING_FOR_NETWORK) {
            value = R.string.asdm_paused_waiting_for_network;
        } else if (reasonCode == DownloadManager.PAUSED_WAITING_TO_RETRY) {
            value = R.string.asdm_paused_waiting_to_retry;
        } else if (reasonCode == DownloadManager.ERROR_CANNOT_RESUME) {
            value = R.string.asdm_error_cannot_resume;
        } else if (reasonCode == DownloadManager.ERROR_DEVICE_NOT_FOUND) {
            value = R.string.asdm_error_device_not_found;
        } else if (reasonCode == DownloadManager.ERROR_FILE_ALREADY_EXISTS) {
            value = R.string.asdm_error_file_already_exists;
        } else if (reasonCode == DownloadManager.ERROR_FILE_ERROR) {
            value = R.string.asdm_error_file_error;
        } else if (reasonCode == DownloadManager.ERROR_HTTP_DATA_ERROR) {
            value = R.string.asdm_error_http_data_error;
        } else if (reasonCode == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
            value = R.string.asdm_error_insufficient_space;
        } else if (reasonCode == DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
            value = R.string.asdm_error_too_many_redirects;
        } else if (reasonCode == DownloadManager.ERROR_UNHANDLED_HTTP_CODE) {
            value = R.string.asdm_error_unhandled_http_code;
        } else if (reasonCode == DownloadManager.ERROR_UNKNOWN) {
            value = R.string.asdm_error_unknown;
        }
        return (value > 0 ? getString(value) + " (" : "(") + reasonCode + ")";
    }

    private void append(final StringBuilder sb, final int colIndex, final String prefix, final Func1<Integer, String> formatter) {
        sb.append("- ").append(prefix).append(": ");
        if (colIndex >= 0) {
            sb.append(formatter.call(colIndex));
        } else {
            sb.append("(data not available)");
        }
        sb.append("\n");
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fillAdapter() {
        // retrieve list of pending downloads
        pendingDownloads = PendingDownload.getAllPendingDownloads();
        if (pendingDownloads.size() == 0) {
            SimpleDialog.of(this).setTitle(R.string.debug_current_downloads).setMessage(R.string.downloader_no_pending_downloads).confirm((dialog, which) -> finish());
        } else {
            // get detailed info
            downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            for (PendingDownload.PendingDownloadDescriptor download : pendingDownloads) {
                final DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(download.id);

                final StringBuilder sb = new StringBuilder();
                final AtomicInteger status = new AtomicInteger();
                try (Cursor c = downloadManager.query(query)) {
                    final int statusColumn = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    while (c.moveToNext()) {
                        append(sb, statusColumn, "Status", (i) -> {
                            status.set(c.getInt(i));
                            return formatStatus(c.getInt(i));
                        });
                        append(sb, c.getColumnIndex(DownloadManager.COLUMN_REASON), "Reason", (i) -> formatReason(c.getInt(i)));
                        append(sb, c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES), "Bytes Total", (i) -> formatBytes(c.getLong(i)));
                        append(sb, c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR), "Bytes Current", (i) -> formatBytes(c.getLong(i)));
                        append(sb, c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP), "Last Modified", (i) -> formatDateForFilename(c.getLong(i)));
                        append(sb, c.getColumnIndex(DownloadManager.COLUMN_URI), "Remote URI", c::getString);
                        append(sb, c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI), "Local URI", c::getString);
                    }
                }
                download.info = sb.toString();
                download.isFailedDownload = (status.get() == DownloadManager.STATUS_FAILED);
            }

            // create view
            recyclerView = findViewById(R.id.list);
            adapter = new PendingDownloadsAdapter(this, downloadManager, pendingDownloads);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void cancelDownload(final long id, final boolean silent) {
        int pos = -1;
        for (int i = 0; i < pendingDownloads.size(); i++) {
            if (pendingDownloads.get(i).id == id) {
                pos = i;
                break;
            }
        }
        if (pos >= 0) {
            final PendingDownload.PendingDownloadDescriptor download = pendingDownloads.get(pos);
            pendingDownloads.remove(pos);
            adapter.notifyDataSetChanged();

            // cancel selected download
            PendingDownload.remove(id);
            downloadManager.remove(id);
            if (!silent) {
                Toast.makeText(this, String.format(getString(R.string.downloader_cancelled_download), download.filename), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class PendingDownloadsViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView detail;
        MaterialButton buttonResume;
        MaterialButton buttonDelete;

        PendingDownloadsViewHolder(final View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            detail = itemView.findViewById(R.id.detail);

            buttonResume = itemView.findViewById(R.id.button_left);
            buttonResume.setIconResource(R.drawable.ic_menu_refresh);
            buttonResume.setVisibility(View.GONE);

            buttonDelete = itemView.findViewById(R.id.button_right);
            buttonDelete.setIconResource(R.drawable.ic_menu_delete);
            buttonDelete.setVisibility(View.VISIBLE);
        }
    }

    private static class PendingDownloadsAdapter extends RecyclerView.Adapter<PendingDownloadsViewHolder> {

        final PendingDownloadsActivity activity;
        final DownloadManager downloadManager;
        final ArrayList<PendingDownload.PendingDownloadDescriptor> pendingDownloads;
        final Markwon markwon;

        PendingDownloadsAdapter(final PendingDownloadsActivity activity, final DownloadManager downloadManager, final ArrayList<PendingDownload.PendingDownloadDescriptor> pendingDownloads) {
            this.activity = activity;
            this.downloadManager = downloadManager;
            this.pendingDownloads = pendingDownloads;
            this.markwon = Markwon.create(activity);
        }

        @Override
        @NonNull
        public PendingDownloadsViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View downloadView = LayoutInflater.from(parent.getContext()).inflate(R.layout.twotexts_twobuttons_item, parent, false);
            return new PendingDownloadsViewHolder(downloadView);
        }

        @Override
        public void onBindViewHolder(final PendingDownloadsViewHolder viewHolder, final int position) {
            final PendingDownload.PendingDownloadDescriptor download = pendingDownloads.get(position);
            viewHolder.title.setText(download == null ? "" : download.filename + " (# " + download.id + ")");
            if (download != null) {
                markwon.setMarkdown(viewHolder.detail, download.info);
                viewHolder.buttonDelete.setOnClickListener(v -> SimpleDialog.of(activity).setTitle(R.string.downloader_cancel_download).setMessage(TextParam.text(String.format(activity.getString(R.string.downloader_cancel_file), download.filename))).confirm((dialog, which) -> activity.cancelDownload(download.id, false)));
                if (download.isFailedDownload) {
                    viewHolder.buttonResume.setVisibility(View.VISIBLE);
                    viewHolder.buttonResume.setOnClickListener(v -> {
                        final ArrayList<Download> downloads = new ArrayList<>();
                        downloads.add(new Download(PendingDownload.load(download.id)));
                        DownloaderUtils.triggerDownloads(activity, R.string.downloader_retry_download, R.string.downloader_retry_download_details, downloads, triggered -> {
                            if (triggered) {
                                activity.cancelDownload(download.id, true);
                                activity.fillAdapter();
                            }
                        });
                    });
                }
            } else {
                viewHolder.detail.setText("");
                viewHolder.buttonDelete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return pendingDownloads.size();
        }

    }

}
