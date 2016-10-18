package cgeo.geocaching.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;

import android.test.AndroidTestCase;

public class LogTypeTrackableTest extends AndroidTestCase {

    public static void testFindById() {
        for (final LogTypeTrackable logTypeTrackable : LogTypeTrackable.values()) {
            assertThat(StringUtils.isNotEmpty(logTypeTrackable.getLabel())).isTrue();
        }
    }

}
