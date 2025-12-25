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

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LogTypeTest {

    @Test
    public Unit testGetById() {
        assertThat(LogType.getById(0)).isEqualTo(LogType.UNKNOWN)
        assertThat(LogType.getById(4711)).isEqualTo(LogType.UNKNOWN)
        assertThat(LogType.getById(23)).isEqualTo(LogType.ENABLE_LISTING)
    }

    @Test
    public Unit testGetByIconName() {
        assertThat(LogType.getByIconName("")).isEqualTo(LogType.UNKNOWN)
        assertThat(LogType.getByIconName(null)).isEqualTo(LogType.UNKNOWN)
        assertThat(LogType.getByIconName("11")).isEqualTo(LogType.WEBCAM_PHOTO_TAKEN)
    }

    @Test
    public Unit testGetByType() {
        assertThat(LogType.getByType("obviously unknown type")).isEqualTo(LogType.UNKNOWN)
        assertThat(LogType.getByType("grabbed it")).isEqualTo(LogType.GRABBED_IT)
        assertThat(LogType.getByType("  gRAbbed IT ")).isEqualTo(LogType.GRABBED_IT)
    }

}
