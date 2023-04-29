package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action3;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class DownloadSelectorMapUpdateCheckTask extends AsyncTaskWithProgressText<Void, List<Download>> {
    private final ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps;
    private final String newSelectionTitle;
    private final AbstractDownloader current;
    private final Action3<List<Download>, String, Boolean> setMaps;

    DownloadSelectorMapUpdateCheckTask(final Activity activity, final ArrayList<CompanionFileUtils.DownloadedFileData> installedOfflineMaps, final String newSelectionTitle, final AbstractDownloader current, final Action3<List<Download>, String, Boolean> setMaps) {
        super(activity, newSelectionTitle, activity.getString(R.string.downloadmap_checking_for_updates));
        this.installedOfflineMaps = installedOfflineMaps;
        this.newSelectionTitle = newSelectionTitle;
        this.current = current;
        this.setMaps = setMaps;
        Log.i("starting MapUpdateCheckTask");
    }

    @Override
    protected List<Download> doInBackgroundInternal(final Void[] none) {
        final List<Download> result = new ArrayList<>();
        result.add(new Download(activity.getString(R.string.downloadmap_title), current.mapBase, true, "", "", current.offlineMapType, AbstractDownloader.ICONRES_FOLDER));
        for (CompanionFileUtils.DownloadedFileData installedOfflineMap : installedOfflineMaps) {
            final Download offlineMap = checkForUpdate(installedOfflineMap);
            if (offlineMap != null && offlineMap.getDateInfo() > installedOfflineMap.remoteDate) {
                offlineMap.setAddInfo(CalendarUtils.yearMonthDay(installedOfflineMap.remoteDate));
                result.add(offlineMap);
            }
        }
        return result;
    }

    @Nullable
    @WorkerThread
    private Download checkForUpdate(final CompanionFileUtils.DownloadedFileData offlineMapData) {
        final AbstractDownloader downloader = Download.DownloadType.getInstance(offlineMapData.remoteParsetype);
        if (downloader == null) {
            Log.e("Map update checker: Cannot find map downloader of type " + offlineMapData.remoteParsetype + " for file " + offlineMapData.localFile);
            return null;
        }

        final Parameters params = new Parameters();
        String page = "";
        try {
            final Response response = Network.getRequest(downloader.getUpdatePageUrl(offlineMapData.remotePage), params).blockingGet();
            page = Network.getResponseData(response, true);
        } catch (final Exception e) {
            return null;
        }

        if (StringUtils.isBlank(page)) {
            Log.e("getMap: No data from server");
            return null;
        }

        try {
            return downloader.checkUpdateFor(page, offlineMapData.remotePage, offlineMapData.remoteFile);
        } catch (final Exception e) {
            Log.e("Map update checker: error parsing parsing html page", e);
            return null;
        }
    }

    @Override
    protected void onPostExecuteInternal(final List<Download> result) {
        setMaps.call(result, newSelectionTitle, result.size() < 2);
    }
}
