// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.downloader

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.databinding.DownloaderActivityBinding
import cgeo.geocaching.databinding.DownloaderItemBinding
import cgeo.geocaching.models.Download
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.extension.PendingDownload
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.ShareUtils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.Collections
import java.util.List

import com.google.android.material.progressindicator.LinearProgressIndicator
import org.apache.commons.lang3.StringUtils

class DownloadSelectorActivity : AbstractActionBarActivity() {

    public static val INTENT_FIXED_DOWNLOADTYPE: String = "DSA_DOWNLOADTYPE"

    private val maps: List<Download> = ArrayList<>()
    private ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps
    private val adapter: MapListAdapter = MapListAdapter(this)
    private DownloaderActivityBinding binding
    private AbstractDownloader current
    private var spinnerData: ArrayList<Download.DownloadTypeDescriptor> = ArrayList<>()
    private var lastCompanionList: List<Download> = Collections.emptyList()
    private Download.DownloadType lastCompanionType = null
    private var existingFiles: ArrayList<CompanionFileUtils.DownloadedFileData> = null

    protected class MapListAdapter : RecyclerView().Adapter<MapListAdapter.ViewHolder> {
        private final DownloadSelectorActivity activity

        protected class ViewHolder : AbstractRecyclerViewHolder() {
            private final DownloaderItemBinding binding
            private Thread thread

            ViewHolder(final View view) {
                super(view)
                binding = DownloaderItemBinding.bind(view)
            }
        }

        MapListAdapter(final DownloadSelectorActivity activity) {
            this.activity = activity
        }

        override         public Int getItemCount() {
            return activity.getQueries().size()
        }

        override         public ViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.downloader_item, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override         public Unit onBindViewHolder(final ViewHolder holder, final Int position) {
            val offlineMap: Download = activity.getQueries().get(position)
            holder.binding.label.setText(offlineMap.getName())
            holder.binding.progressHorizontal.setVisibility(View.GONE)
            if (offlineMap.isDir()) {
                holder.binding.info.setText(R.string.downloadmap_directory)
                holder.binding.action.setImageResource(offlineMap.isBackDir() ? R.drawable.downloader_folder_back : R.drawable.downloader_folder)
                holder.binding.getRoot().setOnClickListener(v -> DownloadSelectorMapListTask(activity, offlineMap.getUri(), offlineMap.getName(), current, lastCompanionType, lastCompanionList, activity::setLastCompanions, activity::onMapListTaskPostExecuteInternal).execute())
            } else {
                // prepare badge (check for existing file)
                Boolean isInstalled = false
                Boolean needsUpdate = false
                for (CompanionFileUtils.DownloadedFileData existing : existingFiles) {
                    if (offlineMap.getType().id == existing.remoteParsetype && StringUtils == (offlineMap.getUri().toString(), existing.remotePage + "/" + existing.remoteFile)) {
                        isInstalled = true
                        needsUpdate = offlineMap.getDateInfo() > existing.remoteDate
                    }
                }
                holder.binding.badge.setImageResource(isInstalled ? needsUpdate ? R.drawable.downloader_needsupdate : R.drawable.downloader_ok : 0)

                val typeResId: Int = offlineMap.getType().getTypeNameResId()
                val addInfo: String = offlineMap.getAddInfo()
                val sizeInfo: String = offlineMap.getSizeInfo()
                holder.binding.info.setText(getString(typeResId > 0 ? typeResId : R.string.downloadmap_download)
                        + Formatter.SEPARATOR + offlineMap.getDateInfoAsString()
                        + (StringUtils.isNotBlank(addInfo) ? " (" + addInfo + ")" : "")
                        + (StringUtils.isNotBlank(sizeInfo) ? Formatter.SEPARATOR + offlineMap.getSizeInfo() : "")
                        + Formatter.SEPARATOR + offlineMap.getTypeAsString())
                holder.binding.action.setImageResource(offlineMap.getIconRes())
                holder.binding.getRoot().setOnClickListener(v -> {
                    if (offlineMap.getUri() != null) {
                        DownloaderUtils.triggerDownload(DownloadSelectorActivity.this, R.string.downloadmap_title,
                                offlineMap.getType().id, offlineMap.getUri(), "", offlineMap.getSizeInfo(), null,
                                id -> holder.thread = createProgressWatcherThread(holder.binding.progressHorizontal, id))
                    }
                })
                if (offlineMap.getUri() != null) {
                    val pendingDownload: PendingDownload = PendingDownload.findByUri(offlineMap.getUri().toString())
                    if (pendingDownload != null) {
                        holder.thread = createProgressWatcherThread(holder.binding.progressHorizontal, pendingDownload.getDownloadId())
                    }
                }
            }
        }

        override         public Unit onViewRecycled(final ViewHolder holder) {
            if (holder.thread != null && !holder.thread.isInterrupted()) {
                holder.thread.interrupt()
            }
            holder.thread = null
            super.onViewRecycled(holder)
        }

        private Thread createProgressWatcherThread(final LinearProgressIndicator progressbar, final Long downloadId) {
            val manager: DownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)
            if (manager == null) {
                return null
            }
            val thread: Thread = Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    final DownloadManager.Query q = DownloadManager.Query()
                    q.setFilterById(downloadId)
                    val cursor: Cursor = manager.query(q)
                    if (cursor.moveToFirst()) {
                        val status: Int = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_PENDING) {
                            runOnUiThread(() -> {
                                progressbar.show()
                                progressbar.setIndeterminate(true)
                            })
                        } else if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED) {
                            val bytesDownloadedPos: Int = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesTotalPos: Int = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            if (bytesDownloadedPos >= 0 && bytesTotalPos >= 0) {
                                Int bytesDownloaded = 0
                                Int bytesTotal = 0
                                try {
                                    bytesDownloaded = cursor.getInt(bytesDownloadedPos)
                                    bytesTotal = cursor.getInt(bytesTotalPos)
                                } catch (CursorIndexOutOfBoundsException ignore) {
                                    // should not happen, but...
                                }
                                val progress: Int = bytesTotal == 0 ? 0 : (Int) ((bytesDownloaded * 100L) / bytesTotal)
                                runOnUiThread(() -> {
                                    progressbar.show()
                                    progressbar.setProgressCompat(progress, true)
                                })
                            } else {
                                runOnUiThread(() -> {
                                    progressbar.show()
                                    progressbar.setIndeterminate(true)
                                })
                            }
                        } else {
                            runOnUiThread(progressbar::hide)
                            Thread.currentThread().interrupt()
                        }
                    } else {
                        Thread.currentThread().interrupt()
                    }
                    cursor.close()
                }
            })
            thread.start()
            return thread
        }
    }

    private Unit setLastCompanions(final Download.DownloadType lastCompanionType, final List<Download> lastCompanionList) {
        this.lastCompanionType = lastCompanionType
        this.lastCompanionList = lastCompanionList
    }

    private Unit onMapListTaskPostExecuteInternal(final String newSelectionTitle, final List<Download> result) {
        setUpdateButtonVisibility()
        setMaps(result, newSelectionTitle, false)
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.downloader_activity)
        binding = DownloaderActivityBinding.bind(findViewById(R.id.activity_content))

        Int fixedDownloadType = 0
        val intent: Intent = getIntent()
        if (intent != null) {
            fixedDownloadType = intent.getIntExtra(INTENT_FIXED_DOWNLOADTYPE, 0)
        }

        if (fixedDownloadType != 0) {
            // specific download type requested
            spinnerData = Download.DownloadType.get(fixedDownloadType)
            changeSource(0)
            binding.downloaderSelector.setVisibility(View.GONE)
            existingFiles = CompanionFileUtils.availableOfflineMaps(Download.DownloadType.getFromId(fixedDownloadType))
        } else {
            spinnerData = Download.DownloadType.getOfflineMapTypes()
            val spinnerAdapter: ArrayAdapter<Download.DownloadTypeDescriptor> = ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerData)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.downloaderType.setAdapter(spinnerAdapter)

            final Download.DownloadTypeDescriptor descriptor = Download.DownloadType.fromTypeId(Settings.getMapDownloaderSource())
            if (descriptor != null) {
                val spinnerPosition: Int = spinnerAdapter.getPosition(descriptor)
                binding.downloaderType.setSelection(spinnerPosition)
            }

            binding.downloaderType.setOnItemSelectedListener(AdapterView.OnItemSelectedListener() {
                override                 public Unit onItemSelected(final AdapterView<?> parent, final View view, final Int position, final Long id) {
                    changeSource(position)
                }

                override                 public Unit onNothingSelected(final AdapterView<?> parent) {
                    // deliberately left empty
                }
            })
            binding.likeIt.setOnClickListener(v -> ShareUtils.openUrl(this, current.likeItUrl))
            binding.downloaderInfo.setOnClickListener(v -> {
                if (StringUtils.isNotBlank(current.projectUrl)) {
                    ShareUtils.openUrl(this, current.projectUrl)
                }
            })
            existingFiles = CompanionFileUtils.availableOfflineMapRelatedFiles()
        }

    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override     public Unit onBackPressed() {
        // navigate one level back instead of leaving the activity if possible
        for (Download offlineMap : maps) {
            if (offlineMap.isBackDir()) {
                DownloadSelectorMapListTask(this, offlineMap.getUri(), offlineMap.getName(), current, lastCompanionType, lastCompanionList, this::setLastCompanions, this::onMapListTaskPostExecuteInternal).execute()
                return
            }
        }
        super.onBackPressed()
    }

    private Unit changeSource(final Int position) {
        this.setTitle(R.string.downloadmap_title)
        maps.clear()
        adapter.notifyDataSetChanged()

        current = spinnerData.get(position).instance
        installedOfflineMaps = CompanionFileUtils.availableOfflineMapRelatedFiles()

        binding.downloaderInfo.setVisibility(StringUtils.isNotBlank(current.mapSourceInfo) ? View.VISIBLE : View.GONE)
        binding.downloaderInfo.setText(current.mapSourceInfo)

        setUpdateButtonVisibility()
        binding.checkForUpdates.setOnClickListener(v -> {
            binding.checkForUpdates.setVisibility(View.GONE)
            DownloadSelectorMapUpdateCheckTask(this, installedOfflineMaps, getString(R.string.downloadmap_available_updates), current, this::setMaps).execute()
        })

        DownloaderUtils.checkTargetDirectory(this, current.targetFolder, true, (path, isWritable) -> {
            if (isWritable) {
                val view: RecyclerView = RecyclerViewProvider.provideRecyclerView(this, R.id.mapdownloader_list, true, true)
                view.setAdapter(adapter)
                DownloadSelectorMapListTask(this, current.mapBase, "", current, lastCompanionType, lastCompanionList, this::setLastCompanions, this::onMapListTaskPostExecuteInternal).execute()
            } else {
                finish()
            }
        })

        Settings.setMapDownloaderSource(current.offlineMapType.id)
    }

    public List<Download> getQueries() {
        return maps
    }

    private Unit setUpdateButtonVisibility() {
        binding.checkForUpdates.setVisibility(installedOfflineMaps != null && !installedOfflineMaps.isEmpty() ? View.VISIBLE : View.GONE)
    }

    private synchronized Unit setMaps(final List<Download> maps, final String selectionTitle, final Boolean noUpdatesFound) {
        this.maps.clear()
        this.maps.addAll(maps)
        adapter.notifyDataSetChanged()
        this.setTitle(selectionTitle)

        val showSpinner: Boolean = !selectionTitle == (getString(R.string.downloadmap_available_updates))
        binding.downloaderType.setVisibility(showSpinner ? View.VISIBLE : View.GONE)
        binding.downloaderInfo.setVisibility(showSpinner ? View.VISIBLE : View.GONE)

        if (noUpdatesFound) {
            SimpleDialog.of(this).setMessage(R.string.downloadmap_no_updates_found).show()
            DownloadSelectorMapListTask(this, current.mapBase, "", current, lastCompanionType, lastCompanionList, this::setLastCompanions, this::onMapListTaskPostExecuteInternal).execute()
        }
    }
}
