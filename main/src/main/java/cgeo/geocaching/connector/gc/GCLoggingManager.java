package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.AbstractLoggingManager;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogContextInfo;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class GCLoggingManager extends AbstractLoggingManager {

    private static final List<ReportProblemType> REPORT_PROBLEM_TYPES = Arrays.asList(ReportProblemType.LOG_FULL, ReportProblemType.DAMAGED, ReportProblemType.MISSING, ReportProblemType.ARCHIVE, ReportProblemType.OTHER);

    GCLoggingManager(final Geocache cache) {
        super(GCConnector.getInstance(), cache);
    }

    @NonNull
    @Override
    public GCConnector getConnector() {
        return (GCConnector) super.getConnector();
    }

    @WorkerThread
    @NonNull
    @Override
    public LogContextInfo getLogContextInfo(@Nullable final String serviceLogId) {
        final LogContextInfo result = new LogContextInfo(this, serviceLogId);
        if (!Settings.hasGCCredentials()) { // allow offline logging
            result.addError(R.string.err_login);
            return result;
        }

        //New Log: https://www.geocaching.com/live/geocache/gcXZ/log
        //Existing log: https://www.geocaching.com/live/log/GLabc
        final String url = serviceLogId == null ? GCLogAPI.getUrlForNewLog(getCache().getGeocode()) : GCLogAPI.getUrlForEditLog(getCache().getGeocode(), serviceLogId);
        final String page;
        try {
            page = Network.getResponseData(Network.getRequest(url, null));
        } catch (final Exception e) {
            Log.w("HCLoggingManager: failed to retrieve log page data for '" + url + "'", e);
            result.setError();
            return result;
        }

        result.setAvailableLogTypes(GCParser.parseTypes(page));
        final int totalTrackables = GCParser.parseTrackableCount(page);
        // log page HTML contains up to 20 TBs
        if (totalTrackables <= 20) {
            result.setAvailableTrackables(GCParser.parseTrackables(page));
        } else {
            try {
                result.setAvailableTrackables(loadTrackablesPaged(totalTrackables));
            } catch (Exception e) {
                result.setError();
                Log.w("GCLoggingManager.onLoadFinished: getTrackableInventory", e);
            }
        }

        // TODO: also parse ProblemLogTypes: logSettings.problemLogTypes.push(45);

        /* TODO: the GUID is not available in the new log page
        if (StringUtils.isBlank(cache.getGuid())) {
            // Acquire the cache GUID from the log page. This will not only complete the information in the database,
            // but also allow the user to post a rating using GCVote since it requires the GUID to do so.

            final String guid = TextUtils.getMatch(page, GCConstants.PATTERN_LOG_GUID, null);
            if (StringUtils.isNotBlank(guid)) {
                cache.setGuid(guid);
                DataStore.saveChangedCache(cache);
            } else {
                Log.w("Could not acquire GUID from log page for " + cache.getGeocode());
            }
        }*/

        try {
            final GCLogin.ServerParameters params = GCLogin.getInstance().getServerParameters();
            final Integer premFavcount = GCWebAPI.getAvailableFavoritePoints(params.userInfo.referenceCode).blockingGet();
            if (premFavcount != null && premFavcount >= 0) {
                result.setAvailableFavoritePoints(premFavcount);
            } else {
                result.setError();
            }
        } catch (final Exception e) {
            result.setError();
            Log.w("GCLoggingManager.onLoadFinished: getAvailableFavoritePoints", e);
        }

        return result;
    }

    private List<Trackable> loadTrackablesPaged(final int totalTrackables) {
        final List<GCWebAPI.TrackableInventoryEntry> trackableInventoryItems = GCWebAPI.getTrackableInventory(totalTrackables);
        final List<Trackable> trackables = CollectionStream.of(trackableInventoryItems).map(entry -> {
            final Trackable trackable = new Trackable();
            trackable.setGeocode(entry.referenceCode);
            trackable.setTrackingcode(entry.trackingNumber);
            trackable.setName(entry.name);
            trackable.forceSetBrand(TrackableBrand.TRAVELBUG);
            return trackable;
            //new TrackableLog(entry.referenceCode, entry.trackingNumber, entry.name, TrackableBrand.TRAVELBUG)
        }).toList();
        return trackables;
    }

    @NonNull
    @Override
    public LogResult createLog(@NonNull final OfflineLogEntry logEntry, @Nullable final Map<String, Trackable> inventory) {

        final Geocache cache = getCache();

        try {
            return GCLogAPI.createLog(cache.getGeocode(), logEntry, inventory);
        } catch (final Exception e) {
            Log.e("GCLoggingManager.createLog", e);
            return LogResult.error(StatusCode.LOG_POST_ERROR, "GCLoggingManager.createLog", e);
        }
    }

    @NonNull
    @Override
    public LogResult editLog(@NonNull final LogEntry newEntry) {
        return GCLogAPI.editLog(getCache().getGeocode(), newEntry);
    }

    @NonNull
    @Override
    public LogResult deleteLog(@NonNull final LogEntry newEntry, @Nullable final String reason) {
        return GCLogAPI.deleteLog(newEntry.serviceLogId, reason);
    }

    @NonNull
    @Override
    public ImageResult createLogImage(@NonNull final String logId, @NonNull final Image image) {
        if (!image.isEmpty() && image.getFile() != null) {
            return GCLogAPI.addLogImage(logId, image);
        }
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "No valid image:" + image, null);
    }

    @NonNull
    @Override
    public ImageResult editLogImage(@NonNull final String logId, @NonNull final String serviceImageId, @Nullable final String title, @Nullable final String description) {
        return GCLogAPI.editLogImageData(logId, serviceImageId, title, description);
    }

    @NonNull
    @Override
    public ImageResult deleteLogImage(@NonNull final String logId, @NonNull final String serviceImageId) {
        return GCLogAPI.deleteLogImage(logId, serviceImageId);
    }

    @Override
    public boolean supportsEditLogImages() {
        return true;
    }

    @Override
    public boolean supportsDeleteLogImages() {
        return true;
    }

    @Override
    public String convertLogTextToEditableText(final String logText) {

        if (!TextUtils.containsHtml(logText)) {
            return logText;
        }

        //Manual handling required:
        //- italic: <em> and </em> -> *
        //- bold: <strong> and </strong> -> **
        //- link: <a href="link" ...>text</a> -> [text](link)
        //- headers: <h1>, <h2>, <h3>
        //- horizontal rule: <hr>
        //- lists

        //Handled by Html.fromHtml()
        //- <p></p> -> carriage return with one line space
        //- <br /> -> just carriage return

        final String preLogText = logText
            .replaceAll("<strong>", "**")
            .replaceAll("</strong>", "**")
            .replaceAll("<em>", "*")
            .replaceAll("</em>", "*")
            .replaceAll("<h1>", "# ")
            .replaceAll("</h1>", " #")
            .replaceAll("<h2>", "## ")
            .replaceAll("</h2>", " ##")
            .replaceAll("<h3>", "### ")
            .replaceAll("</h3>", " ###")
            .replaceAll("<hr>", "---")
            .replaceAll("<hr />", "---")
            .replaceAll("<ul>", "")
            .replaceAll("<ol>", "")
            .replaceAll("</li>", "")
            .replaceAll("<li>", "* ")
            .replaceAll("<a href=\"([^\"]+)\"[^>]*>([^<]+)</a>", "[$2]($1)");
        return Html.fromHtml(preLogText).toString();
    }

    @Override
    public boolean supportsLogWithFavorite() {
        return true;
    }

    @Override
    public boolean supportsLogWithTrackables() {
        return true;
    }
    @NonNull
    @Override
    public List<ReportProblemType> getReportProblemTypes(@NonNull final Geocache geocache) {
        if (geocache.isArchived() || geocache.isOwner()) {
            return Collections.emptyList();
        }
        final List<ReportProblemType> possibleReportProblemTypes = new ArrayList<>();
        for (final ReportProblemType reportProblem : REPORT_PROBLEM_TYPES) {
            if ((!geocache.isEventCache() && !geocache.isDisabled()) || reportProblem == ReportProblemType.ARCHIVE) {
                possibleReportProblemTypes.add(reportProblem);
            }
        }
        return possibleReportProblemTypes;
    }

}
