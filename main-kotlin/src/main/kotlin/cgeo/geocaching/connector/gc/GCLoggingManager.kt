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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.R
import cgeo.geocaching.connector.AbstractLoggingManager
import cgeo.geocaching.connector.ImageResult
import cgeo.geocaching.connector.LogContextInfo
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.network.Network
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import android.text.Html

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.List
import java.util.Map

class GCLoggingManager : AbstractLoggingManager() {

    private static val REPORT_PROBLEM_TYPES: List<ReportProblemType> = Arrays.asList(ReportProblemType.LOG_FULL, ReportProblemType.DAMAGED, ReportProblemType.MISSING, ReportProblemType.ARCHIVE, ReportProblemType.OTHER)

    GCLoggingManager(final Geocache cache) {
        super(GCConnector.getInstance(), cache)
    }

    override     public GCConnector getConnector() {
        return (GCConnector) super.getConnector()
    }

    @WorkerThread
    override     public LogContextInfo getLogContextInfo(final String serviceLogId) {
        val result: LogContextInfo = LogContextInfo(this, serviceLogId)
        if (!Settings.hasGCCredentials()) { // allow offline logging
            result.addError(R.string.err_login)
            return result
        }

        //New Log: https://www.geocaching.com/live/geocache/gcXZ/log
        //Existing log: https://www.geocaching.com/live/log/GLabc
        val url: String = serviceLogId == null ? GCLogAPI.getUrlForNewLog(getCache().getGeocode()) : GCLogAPI.getUrlForEditLog(getCache().getGeocode(), serviceLogId)
        final String page
        try {
            page = Network.getResponseData(Network.getRequest(url, null))
        } catch (final Exception e) {
            Log.w("HCLoggingManager: failed to retrieve log page data for '" + url + "'", e)
            result.setError()
            return result
        }

        result.setAvailableLogTypes(GCParser.parseTypes(page))
        val totalTrackables: Int = GCParser.parseTrackableCount(page)
        // log page HTML contains up to 20 TBs
        if (totalTrackables <= 20) {
            result.setAvailableTrackables(GCParser.parseTrackables(page))
        } else {
            try {
                result.setAvailableTrackables(loadTrackablesPaged(totalTrackables))
            } catch (Exception e) {
                result.setError()
                Log.w("GCLoggingManager.onLoadFinished: getTrackableInventory", e)
            }
        }

        // TODO: also parse ProblemLogTypes: logSettings.problemLogTypes.push(45)

        /* TODO: the GUID is not available in the log page
        if (StringUtils.isBlank(cache.getGuid())) {
            // Acquire the cache GUID from the log page. This will not only complete the information in the database,
            // but also allow the user to post a rating using GCVote since it requires the GUID to do so.

            val guid: String = TextUtils.getMatch(page, GCConstants.PATTERN_LOG_GUID, null)
            if (StringUtils.isNotBlank(guid)) {
                cache.setGuid(guid)
                DataStore.saveChangedCache(cache)
            } else {
                Log.w("Could not acquire GUID from log page for " + cache.getGeocode())
            }
        }*/

        try {
            final GCLogin.ServerParameters params = GCLogin.getInstance().getServerParameters()
            val premFavcount: Integer = GCWebAPI.getAvailableFavoritePoints(params.userInfo.referenceCode).blockingGet()
            if (premFavcount != null && premFavcount >= 0) {
                result.setAvailableFavoritePoints(premFavcount)
            } else {
                result.setError()
            }
        } catch (final Exception e) {
            result.setError()
            Log.w("GCLoggingManager.onLoadFinished: getAvailableFavoritePoints", e)
        }

        return result
    }

    private List<Trackable> loadTrackablesPaged(final Int totalTrackables) {
        val trackableInventoryItems: List<GCWebAPI.TrackableInventoryEntry> = GCWebAPI.getTrackableInventory(totalTrackables)
        val trackables: List<Trackable> = CollectionStream.of(trackableInventoryItems).map(entry -> {
            val trackable: Trackable = Trackable()
            trackable.setGeocode(entry.referenceCode)
            trackable.setTrackingcode(entry.trackingNumber)
            trackable.setName(entry.name)
            trackable.forceSetBrand(TrackableBrand.TRAVELBUG)
            return trackable
            //TrackableLog(entry.referenceCode, entry.trackingNumber, entry.name, TrackableBrand.TRAVELBUG)
        }).toList()
        return trackables
    }

    override     public LogResult createLog(final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {

        val cache: Geocache = getCache()

        try {
            return GCLogAPI.createLog(cache.getGeocode(), logEntry, inventory)
        } catch (final Exception e) {
            Log.e("GCLoggingManager.createLog", e)
            return LogResult.error(StatusCode.LOG_POST_ERROR, "GCLoggingManager.createLog", e)
        }
    }

    override     public LogResult editLog(final LogEntry newEntry) {
        return GCLogAPI.editLog(getCache().getGeocode(), newEntry)
    }

    override     public LogResult deleteLog(final LogEntry newEntry, final String reason) {
        return GCLogAPI.deleteLog(newEntry.serviceLogId, reason)
    }

    override     public ImageResult createLogImage(final String logId, final Image image) {
        if (!image.isEmpty() && image.getFile() != null) {
            return GCLogAPI.addLogImage(logId, image)
        }
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, "No valid image:" + image, null)
    }

    override     public ImageResult editLogImage(final String logId, final String serviceImageId, final String title, final String description) {
        return GCLogAPI.editLogImageData(logId, serviceImageId, title, description)
    }

    override     public ImageResult deleteLogImage(final String logId, final String serviceImageId) {
        return GCLogAPI.deleteLogImage(logId, serviceImageId)
    }

    override     public Boolean supportsEditLogImages() {
        return true
    }

    override     public Boolean supportsDeleteLogImages() {
        return true
    }

    override     public String convertLogTextToEditableText(final String logText) {

        if (!TextUtils.containsHtml(logText)) {
            return logText
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

        val preLogText: String = logText
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
            .replaceAll("<a href=\"([^\"]+)\"[^>]*>([^<]+)</a>", "[$2]($1)")
        return Html.fromHtml(preLogText).toString()
    }

    override     public Boolean supportsLogWithFavorite() {
        return true
    }

    override     public Boolean supportsLogWithTrackables() {
        return true
    }
    override     public List<ReportProblemType> getReportProblemTypes(final Geocache geocache) {
        if (geocache.isArchived() || geocache.isOwner()) {
            return Collections.emptyList()
        }
        val possibleReportProblemTypes: List<ReportProblemType> = ArrayList<>()
        for (final ReportProblemType reportProblem : REPORT_PROBLEM_TYPES) {
            if ((!geocache.isEventCache() && !geocache.isDisabled()) || reportProblem == ReportProblemType.ARCHIVE) {
                possibleReportProblemTypes.add(reportProblem)
            }
        }
        return possibleReportProblemTypes
    }

}
