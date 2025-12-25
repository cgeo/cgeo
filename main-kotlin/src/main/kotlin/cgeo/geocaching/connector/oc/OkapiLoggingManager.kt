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

package cgeo.geocaching.connector.oc

import cgeo.geocaching.R
import cgeo.geocaching.connector.AbstractLoggingManager
import cgeo.geocaching.connector.ImageResult
import cgeo.geocaching.connector.LogContextInfo
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.Date
import java.util.List
import java.util.Map

class OkapiLoggingManager : AbstractLoggingManager() {

    public OkapiLoggingManager(final OCApiLiveConnector connector, final Geocache cache) {
        super(connector, cache)
    }

    override     public OCApiLiveConnector getConnector() {
        return (OCApiLiveConnector) super.getConnector()
    }

    override     public LogResult createLog(final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        val result: LogResult = OkapiClient.postLog(getCache(), logEntry.logType, Date(logEntry.date), logEntry.log, logEntry.password, getConnector(), logEntry.reportProblem, logEntry.favorite, logEntry.rating)
        getConnector().login()
        return result
    }

    override     public ImageResult createLogImage(final String logId, final Image image) {
        return OkapiClient.postLogImage(logId, image, getConnector())
    }

    override     public LogContextInfo getLogContextInfo(final String serviceLogId) {
        val info: LogContextInfo = LogContextInfo(this, serviceLogId)
        if (!getConnector().isLoggedIn()) {
            info.setError()
        }
        info.setAvailableFavoritePoints(getConnector().getRemainingFavoritePoints())
        return info
    }
    override     public Boolean canLogReportType(final ReportProblemType reportType) {
        //Okapi can handle one report type
        return ReportProblemType.NEEDS_MAINTENANCE == reportType
    }

    override     public Boolean supportsLogWithFavorite() {
        return true
    }

    override     public Boolean supportsLogWithVote() {
        return true
    }

    override     public Boolean isImageCaptionMandatory() {
        return true
    }

    override     public List<ReportProblemType> getReportProblemTypes(final Geocache geocache) {
        if (geocache.isEventCache()) {
            return Collections.emptyList()
        }

        return Collections.singletonList(ReportProblemType.NEEDS_MAINTENANCE)
    }

    override     public Int getFavoriteCheckboxText() {
        return R.plurals.fav_points_remaining_oc
    }
}
