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

package cgeo.geocaching.connector

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.utils.TextUtils

import android.text.Html

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Collections
import java.util.List
import java.util.Objects

abstract class AbstractLoggingManager : ILoggingManager {

    private final Geocache cache
    private final IConnector connector

    protected AbstractLoggingManager(final IConnector connector, final Geocache cache) {
        this.cache = Objects.requireNonNull(cache)
        this.connector = Objects.requireNonNull(connector)
    }

    override     public Geocache getCache() {
        return cache
    }

    override     public IConnector getConnector() {
        return connector
    }


    override     public LogResult editLog(final LogEntry newEntry) {
        return LogResult.error(StatusCode.LOG_POST_ERROR)
    }

    override     public LogResult deleteLog(final LogEntry newEntry, final String reason) {
        return LogResult.error(StatusCode.LOG_POST_ERROR)
    }

    override     public String convertLogTextToEditableText(final String logText) {
        if (TextUtils.containsHtml(logText)) {
            return Html.fromHtml(logText).toString()
        }
        return logText
    }

    override     public Boolean canEditLog(final LogEntry entry) {
        return getConnector().canEditLog(getCache(), entry)
    }

    override     public Boolean canDeleteLog(final LogEntry entry) {
        return getConnector().canDeleteLog(getCache(), entry)
    }

    override     public ImageResult createLogImage(final String logId, final Image image) {
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR)
    }

    override     public ImageResult editLogImage(final String logId, final String serviceImageId, final String title, final String description) {
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR)
    }

    override     public ImageResult deleteLogImage(final String logId, final String serviceImageId) {
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR)
    }

    override     public Boolean supportsEditLogImages() {
        return false
    }

    override     public Boolean supportsDeleteLogImages() {
        return false
    }

    override     public Boolean supportsLogWithFavorite() {
        return false
    }

    override     public Boolean supportsLogWithTrackables() {
        return false
    }

    override     public Boolean supportsLogWithVote() {
        return false
    }


    override     public Long getMaxImageUploadSize() {
        return null
    }

    override     public Boolean isImageCaptionMandatory() {
        return false
    }

    override     public Boolean canLogReportType(final ReportProblemType reportType) {
        return false
    }

    override     public List<ReportProblemType> getReportProblemTypes(final Geocache geocache) {
        return Collections.emptyList()
    }

    override     public LogContextInfo getLogContextInfo(final String serviceLogId) {
        return LogContextInfo(this, serviceLogId)
    }

    override     public Int getFavoriteCheckboxText() {
        return R.plurals.fav_points_remaining
    }

}
