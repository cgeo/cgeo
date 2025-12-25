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

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LogTypeTrackableTest {

    @Test
    public Unit testFindById() {
        for (final LogTypeTrackable logTypeTrackable : LogTypeTrackable.values()) {
            assertThat(StringUtils.isNotEmpty(logTypeTrackable.getLabel())).isTrue()
        }
    }

}
