package cgeo.geocaching.log;

import android.test.AndroidTestCase;

import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogTypeTrackableTest extends AndroidTestCase {

    public static void testFindById() {
        for (final LogTypeTrackable logTypeTrackable : LogTypeTrackable.values()) {
            assertThat(StringUtils.isNotEmpty(logTypeTrackable.getLabel())).isTrue();
        }
    }

}
