package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.DownloaderActivityBinding;
import cgeo.geocaching.databinding.DownloaderItemBinding;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.ShareUtils;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import org.apache.commons.lang3.StringUtils;

public class DownloadSelectorActivity extends AbstractActionBarActivity {

    @NonNull
    private final List<Download> maps = new ArrayList<>();
    private ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps;
    private final MapListAdapter adapter = new MapListAdapter(this);
    private DownloaderActivityBinding binding;
    private AbstractDownloader current;
    private ArrayList<Download.DownloadTypeDescriptor> spinnerData = new ArrayList<>();
    private List<Download> lastCompanionList = Collections.emptyList();
    private Download.DownloadType lastCompanionType = null;

    protected class MapListAdapter extends RecyclerView.Adapter<MapListAdapter.ViewHolder> {
        @NonNull private final DownloadSelectorActivity activity;

        protected final class ViewHolder extends AbstractRecyclerViewHolder {
            private final DownloaderItemBinding binding;
            @Nullable private Thread thread;

            ViewHolder(final View view) {
                super(view);
                binding = DownloaderItemBinding.bind(view);
            }
        }

        MapListAdapter(@NonNull final DownloadSelectorActivity activity) {
            this.activity = activity;
        }

        @Override
        public int getItemCount() {
            return activity.getQueries().size();
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.downloader_item, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final Download offlineMap = activity.getQueries().get(position);
            holder.binding.label.setText(offlineMap.getName());
            holder.binding.progressHorizontal.setVisibility(View.GONE);
            if (offlineMap.isDir()) {
                holder.binding.info.setText(R.string.downloadmap_directory);
                holder.binding.action.setImageResource(offlineMap.isBackDir() ? R.drawable.downloader_folder_back : R.drawable.downloader_folder);
                holder.binding.getRoot().setOnClickListener(v -> new DownloadSelectorMapListTask(activity, offlineMap.getUri(), offlineMap.getName(), current, lastCompanionType, lastCompanionList, activity::setLastCompanions, activity::onMapListTaskPostExecuteInternal).execute());
            } else {
                final int typeResId = offlineMap.getType().getTypeNameResId();
                final String addInfo = offlineMap.getAddInfo();
                final String sizeInfo = offlineMap.getSizeInfo();
                holder.binding.info.setText(getString(typeResId > 0 ? typeResId : R.string.downloadmap_download)
                        + Formatter.SEPARATOR + offlineMap.getDateInfoAsString()
                        + (StringUtils.isNotBlank(addInfo) ? " (" + addInfo + ")" : "")
                        + (StringUtils.isNotBlank(sizeInfo) ? Formatter.SEPARATOR + offlineMap.getSizeInfo() : "")
                        + Formatter.SEPARATOR + offlineMap.getTypeAsString());
                holder.binding.action.setImageResource(offlineMap.getIconRes());
                holder.binding.getRoot().setOnClickListener(v -> {
                    if (offlineMap.getUri() != null) {
                        DownloaderUtils.triggerDownload(DownloadSelectorActivity.this, R.string.downloadmap_title,
                                offlineMap.getType().id, offlineMap.getUri(), "", offlineMap.getSizeInfo(), null,
                                id -> holder.thread = createProgressWatcherThread(holder.binding.progressHorizontal, id));
                    }
                });
                if (offlineMap.getUri() != null) {
                    final PendingDownload pendingDownload = PendingDownload.findByUri(offlineMap.getUri().toString());
                    if (pendingDownload != null) {
                        holder.thread = createProgressWatcherThread(holder.binding.progressHorizontal, pendingDownload.getDownloadId());
                    }
                }
            }
        }

        @Override
        public void onViewRecycled(final @NonNull ViewHolder holder) {
            if (holder.thread != null && !holder.thread.isInterrupted()) {
                holder.thread.interrupt();
            }
            holder.thread = null;
            super.onViewRecycled(holder);
        }

        private Thread createProgressWatcherThread(final LinearProgressIndicator progressbar, final long downloadId) {
            final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                return null;
            }
            final Thread thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    final DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadId);
                    final Cursor cursor = manager.query(q);
                    if (cursor.moveToFirst()) {
                        final int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_PENDING) {
                            runOnUiThread(() -> {
                                progressbar.show();
                                progressbar.setIndeterminate(true);
                            });
                        } else if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED) {
                            final int bytesDownloadedPos = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                            final int bytesTotalPos = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                            if (bytesDownloadedPos >= 0 && bytesTotalPos >= 0) {
                                int bytesDownloaded = 0;
                                int bytesTotal = 0;
                                try {
                                    bytesDownloaded = cursor.getInt(bytesDownloadedPos);
                                    bytesTotal = cursor.getInt(bytesTotalPos);
                                } catch (CursorIndexOutOfBoundsException ignore) {
                                    // should not happen, but...
                                }
                                final int progress = bytesTotal == 0 ? 0 : (int) ((bytesDownloaded * 100L) / bytesTotal);
                                runOnUiThread(() -> {
                                    progressbar.show();
                                    progressbar.setProgressCompat(progress, true);
                                });
                            } else {
                                runOnUiThread(() -> {
                                    progressbar.show();
                                    progressbar.setIndeterminate(true);
                                });
                            }
                        } else {
                            runOnUiThread(progressbar::hide);
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        Thread.currentThread().interrupt();
                    }
                    cursor.close();
                }
            });
            thread.start();
            return thread;
        }
    }

    private void setLastCompanions(final Download.DownloadType lastCompanionType, final List<Download> lastCompanionList) {
        this.lastCompanionType = lastCompanionType;
        this.lastCompanionList = lastCompanionList;
    }

    private void onMapListTaskPostExecuteInternal(final String newSelectionTitle, final List<Download> result) {
        setUpdateButtonVisibility();
        setMaps(result, newSelectionTitle, false);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.downloader_activity);
        binding = DownloaderActivityBinding.bind(findViewById(R.id.mapdownloader_activity_viewroot));

        spinnerData = Download.DownloadType.getOfflineMapTypes();
        final ArrayAdapter<Download.DownloadTypeDescriptor> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerData);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.downloaderType.setAdapter(spinnerAdapter);

        final Download.DownloadTypeDescriptor descriptor = Download.DownloadType.fromTypeId(Settings.getMapDownloaderSource());
        if (descriptor != null) {
            final int spinnerPosition = spinnerAdapter.getPosition(descriptor);
            binding.downloaderType.setSelection(spinnerPosition);
        }

        binding.downloaderType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                changeSource(position);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // deliberately left empty
            }
        });
        binding.likeIt.setOnClickListener(v -> ShareUtils.openUrl(this, current.likeItUrl));
        binding.downloaderInfo.setOnClickListener(v -> {
            if (StringUtils.isNotBlank(current.projectUrl)) {
                ShareUtils.openUrl(this, current.projectUrl);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // navigate one level back instead of leaving the activity if possible
        for (Download offlineMap : maps) {
            if (offlineMap.isBackDir()) {
                new DownloadSelectorMapListTask(this, offlineMap.getUri(), offlineMap.getName(), current, lastCompanionType, lastCompanionList, this::setLastCompanions, this::onMapListTaskPostExecuteInternal).execute();
                return;
            }
        }
        super.onBackPressed();
    }

    private void changeSource(final int position) {
        this.setTitle(R.string.downloadmap_title);
        maps.clear();
        adapter.notifyDataSetChanged();

        current = spinnerData.get(position).instance;
        installedOfflineMaps = CompanionFileUtils.availableOfflineMapRelatedFiles();

        binding.downloaderInfo.setVisibility(StringUtils.isNotBlank(current.mapSourceInfo) ? View.VISIBLE : View.GONE);
        binding.downloaderInfo.setText(current.mapSourceInfo);

        setUpdateButtonVisibility();
        binding.checkForUpdates.setOnClickListener(v -> {
            binding.checkForUpdates.setVisibility(View.GONE);
            new DownloadSelectorMapUpdateCheckTask(this, installedOfflineMaps, getString(R.string.downloadmap_available_updates), current, this::setMaps).execute();
        });

        DownloaderUtils.checkTargetDirectory(this, current.targetFolder, true, (path, isWritable) -> {
            if (isWritable) {
                final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.mapdownloader_list, true, true);
                view.setAdapter(adapter);
                new DownloadSelectorMapListTask(this, current.mapBase, "", current, lastCompanionType, lastCompanionList, this::setLastCompanions, this::onMapListTaskPostExecuteInternal).execute();
            } else {
                finish();
            }
        });

        Settings.setMapDownloaderSource(current.offlineMapType.id);
    }

    public List<Download> getQueries() {
        return maps;
    }

    private void setUpdateButtonVisibility() {
        binding.checkForUpdates.setVisibility(installedOfflineMaps != null && installedOfflineMaps.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private synchronized void setMaps(final List<Download> maps, @NonNull final String selectionTitle, final boolean noUpdatesFound) {
        this.maps.clear();
        this.maps.addAll(maps);
        adapter.notifyDataSetChanged();
        this.setTitle(selectionTitle);

        final boolean showSpinner = !selectionTitle.equals(getString(R.string.downloadmap_available_updates));
        binding.downloaderType.setVisibility(showSpinner ? View.VISIBLE : View.GONE);
        binding.downloaderInfo.setVisibility(showSpinner ? View.VISIBLE : View.GONE);

        if (noUpdatesFound) {
            SimpleDialog.of(this).setMessage(R.string.downloadmap_no_updates_found).show();
            new DownloadSelectorMapListTask(this, current.mapBase, "", current, lastCompanionType, lastCompanionList, this::setLastCompanions, this::onMapListTaskPostExecuteInternal).execute();
        }
    }
}
