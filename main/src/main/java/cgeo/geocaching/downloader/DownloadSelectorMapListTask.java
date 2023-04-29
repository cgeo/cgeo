package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Action2;

import android.app.Activity;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

class DownloadSelectorMapListTask extends AsyncTaskWithProgressText<Void, List<Download>> {
    private final Uri uri;
    private final String newSelectionTitle;
    private final AbstractDownloader current;
    private Download.DownloadType lastCompanionType;
    private List<Download> lastCompanionList;
    private final Action2<Download.DownloadType, List<Download>> setLastCompanions;
    private final Action2<String, List<Download>> onPostExecuteInternal;

    DownloadSelectorMapListTask(final Activity activity, final Uri uri, final String newSelectionTitle, final AbstractDownloader current, final Download.DownloadType lastCompanionType, final List<Download> lastCompanionList, final Action2<Download.DownloadType, List<Download>> setLastCompanions, final Action2<String, List<Download>> onPostExecuteInternal) {
        super(activity, newSelectionTitle, activity.getString(R.string.downloadmap_retrieving_directory_data));
        this.uri = uri;
        this.newSelectionTitle = newSelectionTitle;
        this.current = current;
        this.lastCompanionType = lastCompanionType;
        this.lastCompanionList = lastCompanionList;
        this.setLastCompanions = setLastCompanions;
        this.onPostExecuteInternal = onPostExecuteInternal;
        Log.i("starting MapDownloaderTask: " + uri.toString());
    }

    @Override
    protected List<Download> doInBackgroundInternal(final Void[] none) {
        final List<Download> list = new ArrayList<>();

        // check for companion type (e. g.: themes for maps)
        if (current.companionType != null) {
            if (lastCompanionType == null || !lastCompanionType.equals(current.companionType) || lastCompanionList.isEmpty()) {
                final AbstractDownloader companion = Download.DownloadType.getInstance(current.companionType.id);
                if (companion != null) {
                    lastCompanionList = doInBackgroundHelper(companion.mapBase, companion);
                    lastCompanionType = current.companionType;
                    setLastCompanions.call(lastCompanionType, lastCompanionList);
                }
            }
            if (lastCompanionList != null) {
                list.addAll(lastCompanionList);
            }
        }

        // query current type
        list.addAll(doInBackgroundHelper(uri, current));
        return list;
    }

    private List<Download> doInBackgroundHelper(final Uri uri, final AbstractDownloader downloader) {
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
        final List<Download> list = new ArrayList<>();

        try {
            downloader.analyzePage(uri, list, page);
            Collections.sort(list, (left, right) -> TextUtils.COLLATOR.compare(left.getName(), right.getName()));
            return list;
        } catch (final Exception e) {
            Log.e("Map downloader: error parsing parsing html page", e);
            return Collections.emptyList();
        }
    }

    @Override
    protected void onPostExecuteInternal(final List<Download> result) {
        onPostExecuteInternal.call(newSelectionTitle, result);
    }
}
