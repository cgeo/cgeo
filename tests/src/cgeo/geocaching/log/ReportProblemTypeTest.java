package cgeo.geocaching.log;

import cgeo.geocaching.enumerations.CacheType;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ReportProblemTypeTest {

    @Test
    public void testFindByCode() {
        assertThat(ReportProblemType.findByCode("")).isEqualTo(ReportProblemType.NO_PROBLEM);
        assertThat(ReportProblemType.findByCode("logFull")).isEqualTo(ReportProblemType.LOG_FULL);
        assertThat(ReportProblemType.findByCode("needsMaintenance")).isEqualTo(ReportProblemType.NEEDS_MAINTENANCE);
    }

    @Test
    public void testIsVisible() {
        assertThat(ReportProblemType.ARCHIVE.isVisible(LogType.DIDNT_FIND_IT, CacheType.TRADITIONAL)).isEqualTo(true);
        assertThat(ReportProblemType.DAMAGED.isVisible(LogType.DIDNT_FIND_IT, CacheType.TRADITIONAL)).isEqualTo(false);
        assertThat(ReportProblemType.MISSING.isVisible(LogType.FOUND_IT, CacheType.TRADITIONAL)).isEqualTo(false);
        assertThat(ReportProblemType.LOG_FULL.isVisible(LogType.FOUND_IT, CacheType.TRADITIONAL)).isEqualTo(true);
        assertThat(ReportProblemType.LOG_FULL.isVisible(LogType.FOUND_IT, CacheType.EARTH)).isEqualTo(false);
        assertThat(ReportProblemType.DAMAGED.isVisible(LogType.FOUND_IT, CacheType.WEBCAM)).isEqualTo(false);
        assertThat(ReportProblemType.MISSING.isVisible(LogType.FOUND_IT, CacheType.VIRTUAL)).isEqualTo(false);
    }

}
