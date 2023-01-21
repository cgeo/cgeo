package cgeo.geocaching.log;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogTypeTrackableTest {

    @Test
    public void testFindById() {
        for (final LogTypeTrackable logTypeTrackable : LogTypeTrackable.values()) {
            assertThat(StringUtils.isNotEmpty(logTypeTrackable.getLabel())).isTrue();
        }
    }

}
