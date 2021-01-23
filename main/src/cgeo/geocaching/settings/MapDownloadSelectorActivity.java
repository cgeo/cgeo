package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;
import cgeo.geocaching.utils.OfflineMapUtils;
import cgeo.geocaching.utils.TextUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class MapDownloadSelectorActivity extends AbstractActionBarActivity {

    @NonNull
    private final List<OfflineMap> maps = new ArrayList<>();
    private ArrayList<OfflineMapUtils.OfflineMapData> installedOfflineMaps;
    private final MapListAdapter adapter = new MapListAdapter(this);
    protected @BindView(R.id.downloader_type) Spinner downloaderType;
    protected @BindView(R.id.downloader_info) TextView downloaderInfo;
    protected @BindView(R.id.check_for_updates) Button checkForUpdates;
    private AbstractMapDownloader current;
    private ArrayList<OfflineMap.OfflineMapTypeDescriptor> spinnerData = new ArrayList<>();

    protected class MapListAdapter extends RecyclerView.Adapter<MapListAdapter.ViewHolder> {
        @NonNull private final MapDownloadSelectorActivity activity;

        protected final class ViewHolder extends AbstractRecyclerViewHolder {
            protected @BindView(R.id.label) TextView label;
            protected @BindView(R.id.download) Button download;
            protected @BindView(R.id.retrieve) Button retrieve;
            protected @BindView(R.id.info) TextView info;

            ViewHolder(final View view) {
                super(view);
            }
        }

        MapListAdapter(@NonNull final MapDownloadSelectorActivity activity) {
            this.activity = activity;
        }

        @Override
        public int getItemCount() {
            return activity.getQueries().size();
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mapdownloader_item, parent, false);
            final ViewHolder viewHolder = new ViewHolder(view);
            viewHolder.retrieve.setOnClickListener(v -> {
                final OfflineMap offlineMap = activity.getQueries().get(viewHolder.getAdapterPosition());
                    new MapListTask(activity, offlineMap.getUri(), offlineMap.getName()).execute();
            });
            viewHolder.download.setOnClickListener(v -> {
                final OfflineMap offlineMap = activity.getQueries().get(viewHolder.getAdapterPosition());
                // return to caller with URL chosen
                final Intent intent = new Intent();
                intent.putExtra(MapDownloadUtils.RESULT_CHOSEN_URL, offlineMap.getUri());
                intent.putExtra(MapDownloadUtils.RESULT_SIZE_INFO, offlineMap.getSizeInfo());
                intent.putExtra(MapDownloadUtils.RESULT_DATE, offlineMap.getDateInfo());
                intent.putExtra(MapDownloadUtils.RESULT_TYPEID, offlineMap.getType().id);
                setResult(RESULT_OK, intent);
                finish();
            });
            return viewHolder;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final OfflineMap offlineMap = activity.getQueries().get(position);
            holder.download.setVisibility(!offlineMap.getIsDir() ? View.VISIBLE : View.GONE);
            holder.retrieve.setVisibility(offlineMap.getIsDir() ? View.VISIBLE : View.GONE);
            holder.label.setText(offlineMap.getName());
            if (offlineMap.getIsDir()) {
                holder.info.setText(R.string.downloadmap_directory);
            } else {
                final String addInfo = offlineMap.getAddInfo();
                holder.info.setText(getString(R.string.downloadmap_mapfile) + Formatter.SEPARATOR + offlineMap.getDateInfoAsString() + (StringUtils.isNotBlank(addInfo) ? " (" + addInfo + ")" : "") + Formatter.SEPARATOR + offlineMap.getSizeInfo() + Formatter.SEPARATOR + offlineMap.getTypeAsString());
            }
        }
    }

    private class MapListTask extends AsyncTaskWithProgressText<Void, List<OfflineMap>> {
        private final Uri uri;
        private final String newSelectionTitle;

        MapListTask(final Activity activity, final Uri uri, final String newSelectionTitle) {
            super(activity, newSelectionTitle, getString(R.string.downloadmap_retrieving_directory_data));
            this.uri = uri;
            this.newSelectionTitle = newSelectionTitle;
            Log.i("starting MapDownloaderTask: " + uri.toString());
        }

        @Override
        protected List<OfflineMap> doInBackgroundInternal(final Void[] none) {
            final Parameters params = new Parameters();

            String page = "";
            try {
                final Response response = Network.getRequest(uri.toString(), params).blockingGet();
                page = Network.getResponseData(response, true);
            } catch (final Exception e) {
                return Collections.emptyList();
            }

            if (StringUtils.isBlank(page)) {
                Log.e("getMap: No data from server");
                return Collections.emptyList();
            }
            final List<OfflineMap> list = new ArrayList<>();

            try {
                current.analyzePage(uri, list, page);
                Collections.sort(list, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));
                return list;
            } catch (final Exception e) {
                Log.e("Map downloader: error parsing parsing html page", e);
                return Collections.emptyList();
            }
        }

        @Override
        protected void onPostExecuteInternal(final List<OfflineMap> result) {
            setUpdateButtonVisibility();
            setMaps(result, newSelectionTitle, false);
        }
    }

    private class MapUpdateCheckTask extends AsyncTaskWithProgressText<Void, List<OfflineMap>> {
        private final ArrayList<OfflineMapUtils.OfflineMapData> installedOfflineMaps;
        private final String newSelectionTitle;

        MapUpdateCheckTask(final Activity activity, final ArrayList<OfflineMapUtils.OfflineMapData> installedOfflineMaps, final String newSelectionTitle) {
            super(activity, newSelectionTitle, activity.getString(R.string.downloadmap_checking_for_updates));
            this.installedOfflineMaps = installedOfflineMaps;
            this.newSelectionTitle = newSelectionTitle;
            Log.i("starting MapUpdateCheckTask");
        }

        @Override
        protected List<OfflineMap> doInBackgroundInternal(final Void[] none) {
            final List<OfflineMap> result = new ArrayList<>();
            result.add(new OfflineMap(getString(R.string.downloadmap_title), current.mapBase, true, "", "", current.offlineMapType));
            for (OfflineMapUtils.OfflineMapData installedOfflineMap : installedOfflineMaps) {
                final OfflineMap offlineMap = checkForUpdate(installedOfflineMap);
                if (offlineMap != null && offlineMap.getDateInfo() > installedOfflineMap.remoteDate) {
                    offlineMap.setAddInfo(CalendarUtils.yearMonthDay(installedOfflineMap.remoteDate));
                    result.add(offlineMap);
                }
            }
            return result;
        }

        @Nullable
        private OfflineMap checkForUpdate(final OfflineMapUtils.OfflineMapData offlineMapData) {
            final AbstractMapDownloader downloader = OfflineMap.OfflineMapType.getInstance(offlineMapData.remoteParsetype);
            if (downloader == null) {
                Log.e("Map update checker: Cannot find map downloader of type " + offlineMapData.remoteParsetype + " for file " + offlineMapData.localFile);
                return null;
            }

            final Parameters params = new Parameters();
            String page = "";
            try {
                final Response response = Network.getRequest(offlineMapData.remotePage, params).blockingGet();
                page = Network.getResponseData(response, true);
            } catch (final Exception e) {
                return null;
            }

            if (StringUtils.isBlank(page)) {
                Log.e("getMap: No data from server");
                return null;
            }

            try {
                return downloader.findMap(page, offlineMapData.remotePage, offlineMapData.remoteFile);
            } catch (final Exception e) {
                Log.e("Map update checker: error parsing parsing html page", e);
                return null;
            }
        }

        @Override
        protected void onPostExecuteInternal(final List<OfflineMap> result) {
            setMaps(result, newSelectionTitle, result.size() < 2);
        }
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.mapdownloader_activity);

        spinnerData = OfflineMap.OfflineMapType.getOfflineMapTypes();
        final ArrayAdapter<OfflineMap.OfflineMapTypeDescriptor> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerData);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        downloaderType.setAdapter(spinnerAdapter);
        downloaderType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                changeSource(position);
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // deliberately left empty
            }
        });
    }

    private void changeSource(final int position) {
        this.setTitle(R.string.downloadmap_title);
        maps.clear();
        adapter.notifyDataSetChanged();

        current = spinnerData.get(position).instance;
        installedOfflineMaps = OfflineMapUtils.availableOfflineMaps(null);

        downloaderInfo.setVisibility(StringUtils.isNotBlank(current.mapSourceInfo) ? View.VISIBLE : View.GONE);
        downloaderInfo.setText(current.mapSourceInfo);

        setUpdateButtonVisibility();
        checkForUpdates.setOnClickListener(v -> {
            checkForUpdates.setVisibility(View.GONE);
            new MapUpdateCheckTask(this, installedOfflineMaps, getString(R.string.downloadmap_available_updates)).execute();
        });

        MapDownloadUtils.checkMapDirectory(this, true, (path, isWritable) -> {
            if (isWritable) {
                final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.mapdownloader_list, true, true);
                view.setAdapter(adapter);
                new MapListTask(this, current.mapBase, "").execute();
            } else {
                finish();
            }
        });
    }

    public List<OfflineMap> getQueries() {
        return maps;
    }

    private void setUpdateButtonVisibility() {
        checkForUpdates.setVisibility (installedOfflineMaps != null && installedOfflineMaps.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private synchronized void setMaps(final List<OfflineMap> maps, @NonNull final String selectionTitle, final boolean noUpdatesFound) {
        this.maps.clear();
        this.maps.addAll(maps);
        adapter.notifyDataSetChanged();
        this.setTitle(selectionTitle);

        final boolean showSpinner = !selectionTitle.equals(getString(R.string.downloadmap_available_updates));
        downloaderType.setVisibility(showSpinner ? View.VISIBLE : View.GONE);
        downloaderInfo.setVisibility(showSpinner ? View.VISIBLE : View.GONE);

        if (noUpdatesFound) {
            Dialogs.message(this, R.string.downloadmap_no_updates_found);
            new MapListTask(this, current.mapBase, "").execute();
        }
    }

}
