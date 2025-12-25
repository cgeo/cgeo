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

import cgeo.geocaching.log.LogType
import cgeo.geocaching.log.ReportProblemType
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.utils.LocalizationUtils

import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

/**
 * encapsulates information from connector used in the context of writing a log entry.
 * <br>
 * This information is typically retrieved online via an {@link ILoggingManager}
 * when the user wishes to create or continue a offline log
 */
class LogContextInfo {

    private final String geocode
    private final String serviceLogId

    private var hasLoadError: Boolean = false
    private var userDisplayableErrorMessage: String = null

    private val availableLogTypes: List<LogType> = ArrayList<>()

    private val availableTrackables: List<Trackable> = ArrayList<>()
    private val availableReportProblemTypes: List<ReportProblemType> = ArrayList<>()

    private var availableFavoritePoints: Int = -1; //-1 means "not supported"

    public LogContextInfo(final ILoggingManager logManager, final String serviceLogId) {
        //context info
        this.geocode = logManager.getCache().getGeocode()
        this.serviceLogId = serviceLogId
    }

    public String getGeocode() {
        return geocode
    }

    public String getServiceLogId() {
        return serviceLogId
    }

    public Unit setError() {
        addError(null)
    }

    public Unit addError(@StringRes final Int resId, final Object ... params) {
        addError(LocalizationUtils.getString(resId, params))
    }

    public Unit addError(final String userDisplayableErrorMessage) {
        this.hasLoadError = true
        if (!StringUtils.isBlank(userDisplayableErrorMessage)) {
            if (this.userDisplayableErrorMessage == null) {
                this.userDisplayableErrorMessage = userDisplayableErrorMessage
            } else {
                this.userDisplayableErrorMessage += ", " + userDisplayableErrorMessage
            }
        }
    }

    public Boolean hasLoadError() {
        return hasLoadError
    }

    public String getUserDisplayableErrorMessage() {
        return userDisplayableErrorMessage
    }

    public List<LogType> getAvailableLogTypes() {
        return availableLogTypes
    }

    public Unit setAvailableLogTypes(final Iterable<LogType> logTypes) {
        availableLogTypes.clear()
        for (LogType lt : logTypes) {
            availableLogTypes.add(lt)
        }
    }

    public List<Trackable> getAvailableTrackables() {
        return availableTrackables
    }

    public Unit addAvailableTrackables(final Iterable<Trackable> trackables) {
        for (Trackable lt : trackables) {
            availableTrackables.add(lt)
        }
    }

    public Unit setAvailableTrackables(final Iterable<Trackable> trackables) {
        availableTrackables.clear()
        addAvailableTrackables(trackables)
    }

    public List<ReportProblemType> getAvailableReportProblemTypes() {
        return availableReportProblemTypes
    }

    public Int getAvailableFavoritePoints() {
        return availableFavoritePoints
    }

    public Unit setAvailableFavoritePoints(final Int availableFavoritePoints) {
        this.availableFavoritePoints = availableFavoritePoints
    }
}
