package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.TextUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class MapDownloadSelectorActivity extends AbstractActionBarActivity {

    // entry point for maps dir
    private static final Uri MAP_BASE = Uri.parse(CgeoApplication.getInstance().getString(R.string.mapserver_osm_v5));

    // for those patterns be careful: consecutive spaces get compressed into one! as compressWhitespace is set to true
    private static final Pattern PATTERN_MAP = Pattern.compile("alt=\"\\[ \\]\"><\\/td><td><a href=\"(([-a-z]+)\\.map)\">[-a-z]+\\.map<\\/a><\\/td><td align=\"right\">([-0-9]+)[ 0-9:]+<\\/td><td align=\"right\">([ 0-9\\.]+[KMG])<\\/td>");
    private static final Pattern PATTERN_DIR = Pattern.compile("alt=\"\\[DIR\\]\"><\\/td><td><a href=\"([-a-z]+\\/)");
    private static final Pattern PATTERN_UP  = Pattern.compile("alt=\"\\[PARENTDIR\\]\"><\\/td><td><a href=\"((\\/[-a-zA-Z0-9\\.]+)+\\/)");

    @NonNull
    private final List<OfflineMap> maps = new ArrayList<>();
    private final MapListAdapter adapter = new MapListAdapter(this);

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
                // return to caller with URL chosen
                final OfflineMap offlineMap = activity.getQueries().get(viewHolder.getAdapterPosition());
                final Intent intent = new Intent();
                intent.putExtra(MapDownloadUtils.RESULT_CHOSEN_URL, offlineMap.getUri());
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
                holder.info.setText("directory");
            } else {
                holder.info.setText("map file" + Formatter.SEPARATOR + offlineMap.getDateInfo() + Formatter.SEPARATOR + offlineMap.getSizeInfo());
            }
        }
    }

    private class MapListTask extends AsyncTaskWithProgressText<Void, List<OfflineMap>> {
        private final Uri uri;
        private final String newSelectionTitle;

        MapListTask(final Activity activity, final Uri uri, final String newSelectionTitle) {
            super(activity, newSelectionTitle, "Retrieving directory data...");
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
                if (!MAP_BASE.equals(uri)) {
                    final MatcherWrapper matchUp = new MatcherWrapper(PATTERN_UP, page);
                    if (matchUp.find()) {
                        final String oneUp = uri.toString();
                        final int endOfPreviousSegment = oneUp.lastIndexOf("/", oneUp.length() - 2); // skip trailing "/"
                        if (endOfPreviousSegment > -1) {
                            final OfflineMap offlineMap = new OfflineMap("(one dir up)", Uri.parse(oneUp.substring(0, endOfPreviousSegment + 1)), true, "", "");
                            list.add(offlineMap);
                        }
                    }
                }

                final MatcherWrapper matchDir = new MatcherWrapper(PATTERN_DIR, page);
                while (matchDir.find()) {
                    final OfflineMap offlineMap = new OfflineMap(matchDir.group(1), Uri.parse(uri + matchDir.group(1)), true, "", "");
                    list.add(offlineMap);
                }

                final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
                while (matchMap.find()) {
                    final OfflineMap offlineMap = new OfflineMap(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, matchMap.group(3), matchMap.group(4));
                    list.add(offlineMap);
                }

                Collections.sort(list, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));

                return list;
            } catch (final Exception e) {
                Log.e("Map downloader: error parsing parsing html page", e);
                return Collections.emptyList();
            }
        }

        @Override
        protected void onPostExecuteInternal(final List<OfflineMap> result) {
            setMaps(result, newSelectionTitle);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.mapdownloader_activity);
        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.mapdownloader_list, true, true);
        view.setAdapter(adapter);
        new MapListTask(this, MAP_BASE, "").execute();
    }

    public List<OfflineMap> getQueries() {
        return maps;
    }

    private synchronized void setMaps(final List<OfflineMap> maps, final String selectionTitle) {
        this.maps.clear();
        this.maps.addAll(maps);
        adapter.notifyDataSetChanged();
        this.setTitle(selectionTitle);
    }
}
