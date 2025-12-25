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

package cgeo.geocaching.log

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType

import java.util.Arrays
import java.util.List

import org.apache.commons.collections4.list.UnmodifiableList

/**
 * Different report problem types
 */
enum class class ReportProblemType {

    NO_PROBLEM("noProblem", LogType.UNKNOWN, R.string.log_problem_none, R.string.log_problem_none),
    NEEDS_MAINTENANCE("needsMaintenance", LogType.NEEDS_MAINTENANCE, R.string.log_problem_needs_maintenance_label, R.string.log_problem_needs_maintenance_text), // OC only
    LOG_FULL("logFull", LogType.NEEDS_MAINTENANCE, R.string.log_problem_log_full_label, R.string.log_problem_log_full_text, LogType[]{LogType.DIDNT_FIND_IT}, false),
    DAMAGED("damaged", LogType.NEEDS_MAINTENANCE, R.string.log_problem_damaged_label, R.string.log_problem_damaged_text, LogType[]{LogType.DIDNT_FIND_IT}, false),
    MISSING("missing", LogType.NEEDS_MAINTENANCE, R.string.log_problem_missing_label, R.string.log_problem_missing_text, LogType[]{LogType.FOUND_IT, LogType.WEBCAM_PHOTO_TAKEN}),
    OTHER("other", LogType.NEEDS_MAINTENANCE, R.string.log_problem_other_label, R.string.log_problem_other_text),
    ARCHIVE("archive", LogType.NEEDS_ARCHIVE, R.string.log_problem_archive_label, R.string.log_problem_archive_text)

    public final String code
    public final LogType logType
    public final Int labelId
    public final Int textId
    public final List<LogType> excludeForLogType
    public final Boolean allowedForVirtual

    ReportProblemType(final String code, final LogType logType, final Int labelId, final Int textId) {
        this(code, logType, labelId, textId, LogType[]{})
    }

    ReportProblemType(final String code, final LogType logType, final Int labelId, final Int textId, final LogType[] excludeForLogType) {
        this(code, logType, labelId, textId, excludeForLogType, true)
    }

    ReportProblemType(final String code, final LogType logType, final Int labelId, final Int textId, final LogType[] excludeForLogType, final Boolean allowedForVirtual) {
        this.code = code
        this.logType = logType
        this.labelId = labelId
        this.textId = textId
        this.excludeForLogType = UnmodifiableList<>(Arrays.asList(excludeForLogType))
        this.allowedForVirtual = allowedForVirtual
    }

    public Boolean isVisible(final LogType logType, final CacheType cacheType) {
        for (final LogType excludeLogType : excludeForLogType) {
            if (excludeLogType == logType) {
                return false
            }
        }
        return !cacheType.isVirtual() || allowedForVirtual
    }

    public static ReportProblemType findByCode(final String code) {
        for (final ReportProblemType reportProblem : values()) {
            if (reportProblem.code == (code)) {
                return reportProblem
            }
        }
        return NO_PROBLEM
    }

    public String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getString(labelId)
    }

}
