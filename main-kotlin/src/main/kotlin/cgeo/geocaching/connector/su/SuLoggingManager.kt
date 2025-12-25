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

package cgeo.geocaching.connector.su

import cgeo.geocaching.connector.AbstractLoggingManager
import cgeo.geocaching.connector.ImageResult
import cgeo.geocaching.connector.LogContextInfo
import cgeo.geocaching.connector.LogResult
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.log.OfflineLogEntry
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.utils.Log

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Date
import java.util.Map

class SuLoggingManager : AbstractLoggingManager() {

    SuLoggingManager(final SuConnector connector, final Geocache cache) {
        super(connector, cache)
    }

    override     public LogContextInfo getLogContextInfo(final String serviceLogId) {
        val info: LogContextInfo = LogContextInfo(this, serviceLogId)
        val recCount: Integer = SuApi.getAvailableRecommendations()
        if (recCount != null) {
            info.setAvailableFavoritePoints(recCount)
        }
        return info

    }

    override     public LogResult createLog(final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        final LogResult result
        val cache: Geocache = getCache()
        try {
            result = SuApi.postLog(cache, logEntry.logType, Date(logEntry.date), logEntry.log, logEntry.favorite)
        } catch (final SuApi.SuApiException e) {
            Log.e("Logging manager SuApi.postLog exception: ", e)
            return LogResult.error(StatusCode.LOG_POST_ERROR, "Logging manager SuApi.postLog exception", e)
        }

        if (logEntry.favorite) {
            cache.setFavorite(true)
            cache.setFavoritePoints(cache.getFavoritePoints() + 1)
        }
        return result
    }

    override     public ImageResult createLogImage(final String logId, final Image image) {
        return SuApi.postImage(getCache(), image)
    }

    override     public Boolean supportsLogWithFavorite() {
        return true
    }
}
