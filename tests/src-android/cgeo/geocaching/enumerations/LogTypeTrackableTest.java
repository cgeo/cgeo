package cgeo.geocaching.enumerations;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogTypeTrackableTest {

    @Test
    public void testFindById() {
        for (LogTypeTrackable logTypeTrackable : LogTypeTrackable.values()) {
            assertThat(StringUtils.isNotEmpty(logTypeTrackable.getLabel())).isTrue();
        }
    }

}
