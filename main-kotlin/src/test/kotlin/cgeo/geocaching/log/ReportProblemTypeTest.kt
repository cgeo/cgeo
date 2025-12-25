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

import cgeo.geocaching.enumerations.CacheType

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ReportProblemTypeTest {

    @Test
    public Unit testFindByCode() {
        assertThat(ReportProblemType.findByCode("")).isEqualTo(ReportProblemType.NO_PROBLEM)
        assertThat(ReportProblemType.findByCode("logFull")).isEqualTo(ReportProblemType.LOG_FULL)
        assertThat(ReportProblemType.findByCode("needsMaintenance")).isEqualTo(ReportProblemType.NEEDS_MAINTENANCE)
    }

    @Test
    public Unit testIsVisible() {
        assertThat(ReportProblemType.ARCHIVE.isVisible(LogType.DIDNT_FIND_IT, CacheType.TRADITIONAL)).isEqualTo(true)
        assertThat(ReportProblemType.DAMAGED.isVisible(LogType.DIDNT_FIND_IT, CacheType.TRADITIONAL)).isEqualTo(false)
        assertThat(ReportProblemType.MISSING.isVisible(LogType.FOUND_IT, CacheType.TRADITIONAL)).isEqualTo(false)
        assertThat(ReportProblemType.LOG_FULL.isVisible(LogType.FOUND_IT, CacheType.TRADITIONAL)).isEqualTo(true)
        assertThat(ReportProblemType.LOG_FULL.isVisible(LogType.FOUND_IT, CacheType.EARTH)).isEqualTo(false)
        assertThat(ReportProblemType.DAMAGED.isVisible(LogType.FOUND_IT, CacheType.WEBCAM)).isEqualTo(false)
        assertThat(ReportProblemType.MISSING.isVisible(LogType.FOUND_IT, CacheType.VIRTUAL)).isEqualTo(false)
    }

}
