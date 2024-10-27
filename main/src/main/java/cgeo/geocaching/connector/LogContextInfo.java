package cgeo.geocaching.connector;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * encapsulates information from connector used in the context of writing a log entry.
 * <br>
 * This information is typically retrieved online via an {@link ILoggingManager}
 * when the user wishes to create or continue a new offline log
 */
public class LogContextInfo {

    private final String geocode;
    private final String serviceLogId;

    private boolean hasLoadError = false;
    private String userDisplayableErrorMessage = null;

    private final List<LogType> availableLogTypes = new ArrayList<>();

    private final List<Trackable> availableTrackables = new ArrayList<>();
    private final List<ReportProblemType> availableReportProblemTypes = new ArrayList<>();

    private int availableFavoritePoints = -1; //-1 means "not supported"

    public LogContextInfo(final ILoggingManager logManager, final String serviceLogId) {
        //context info
        this.geocode = logManager.getCache().getGeocode();
        this.serviceLogId = serviceLogId;
    }

    public String getGeocode() {
        return geocode;
    }

    public String getServiceLogId() {
        return serviceLogId;
    }

    public void setError() {
        addError(null);
    }

    public void addError(@StringRes final int resId, final Object ... params) {
        addError(LocalizationUtils.getString(resId, params));
    }

    public void addError(final String userDisplayableErrorMessage) {
        this.hasLoadError = true;
        if (!StringUtils.isBlank(userDisplayableErrorMessage)) {
            if (this.userDisplayableErrorMessage == null) {
                this.userDisplayableErrorMessage = userDisplayableErrorMessage;
            } else {
                this.userDisplayableErrorMessage += ", " + userDisplayableErrorMessage;
            }
        }
    }

    public boolean hasLoadError() {
        return hasLoadError;
    }

    @Nullable
    public String getUserDisplayableErrorMessage() {
        return userDisplayableErrorMessage;
    }

    public List<LogType> getAvailableLogTypes() {
        return availableLogTypes;
    }

    public void setAvailableLogTypes(final Iterable<LogType> logTypes) {
        availableLogTypes.clear();
        for (LogType lt : logTypes) {
            availableLogTypes.add(lt);
        }
    }

    public List<Trackable> getAvailableTrackables() {
        return availableTrackables;
    }

    public void addAvailableTrackables(final Iterable<Trackable> trackables) {
        for (Trackable lt : trackables) {
            availableTrackables.add(lt);
        }
    }

    public void setAvailableTrackables(final Iterable<Trackable> trackables) {
        availableTrackables.clear();
        addAvailableTrackables(trackables);
    }

    public List<ReportProblemType> getAvailableReportProblemTypes() {
        return availableReportProblemTypes;
    }

    public int getAvailableFavoritePoints() {
        return availableFavoritePoints;
    }

    public void setAvailableFavoritePoints(final int availableFavoritePoints) {
        this.availableFavoritePoints = availableFavoritePoints;
    }
}
