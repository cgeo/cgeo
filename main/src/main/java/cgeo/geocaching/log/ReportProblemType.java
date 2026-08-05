package cgeo.geocaching.log;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.list.UnmodifiableList;

/**
 * Different report problem types
 */
public enum ReportProblemType {

    NO_PROBLEM("noProblem", LogType.UNKNOWN, R.string.log_problem_none, R.string.log_problem_none),
    NEEDS_MAINTENANCE("needsMaintenance", LogType.NEEDS_MAINTENANCE, R.string.log_problem_needs_maintenance_label, R.string.log_problem_needs_maintenance_text), // OC only
    LOG_FULL("logFull", LogType.NEEDS_MAINTENANCE, R.string.log_problem_log_full_label, R.string.log_problem_log_full_text, new LogType[]{LogType.DIDNT_FIND_IT}, false),
    DAMAGED("damaged", LogType.NEEDS_MAINTENANCE, R.string.log_problem_damaged_label, R.string.log_problem_damaged_text, new LogType[]{LogType.DIDNT_FIND_IT}, false),
    MISSING("missing", LogType.NEEDS_MAINTENANCE, R.string.log_problem_missing_label, R.string.log_problem_missing_text, new LogType[]{LogType.FOUND_IT, LogType.WEBCAM_PHOTO_TAKEN}),
    OTHER("other", LogType.NEEDS_MAINTENANCE, R.string.log_problem_other_label, R.string.log_problem_other_text),
    ARCHIVE("archive", LogType.NEEDS_ARCHIVE, R.string.log_problem_archive_label, R.string.log_problem_archive_text);

    public final String code;
    public final LogType logType;
    public final int labelId;
    public final int textId;
    public final List<LogType> excludeForLogType;
    public final boolean allowedForVirtual;

    ReportProblemType(final String code, final LogType logType, final int labelId, final int textId) {
        this(code, logType, labelId, textId, new LogType[]{});
    }

    ReportProblemType(final String code, final LogType logType, final int labelId, final int textId, final LogType[] excludeForLogType) {
        this(code, logType, labelId, textId, excludeForLogType, true);
    }

    ReportProblemType(final String code, final LogType logType, final int labelId, final int textId, final LogType[] excludeForLogType, final boolean allowedForVirtual) {
        this.code = code;
        this.logType = logType;
        this.labelId = labelId;
        this.textId = textId;
        this.excludeForLogType = new UnmodifiableList<>(Arrays.asList(excludeForLogType));
        this.allowedForVirtual = allowedForVirtual;
    }

    public boolean isVisible(final LogType logType, final CacheType cacheType) {
        for (final LogType excludeLogType : excludeForLogType) {
            if (excludeLogType == logType) {
                return false;
            }
        }
        return !cacheType.isVirtual() || allowedForVirtual;
    }

    public static ReportProblemType findByCode(final String code) {
        for (final ReportProblemType reportProblem : values()) {
            if (reportProblem.code.equals(code)) {
                return reportProblem;
            }
        }
        return NO_PROBLEM;
    }

    public String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getString(labelId);
    }

}
