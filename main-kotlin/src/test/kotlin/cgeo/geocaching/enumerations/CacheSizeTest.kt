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

package cgeo.geocaching.enumerations

import java.util.Locale

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class CacheSizeTest {

    @Test
    public Unit testOrder() {
        assertThat(CacheSize.MICRO.comparable).isLessThan(CacheSize.SMALL.comparable)
        assertThat(CacheSize.SMALL.comparable).isLessThan(CacheSize.REGULAR.comparable)
        assertThat(CacheSize.REGULAR.comparable).isLessThan(CacheSize.LARGE.comparable)
    }

    @Test
    public Unit testGetById() {
        assertThat(CacheSize.getById("")).isEqualTo(CacheSize.UNKNOWN)
        assertThat(CacheSize.getById(null)).isEqualTo(CacheSize.UNKNOWN)
        assertThat(CacheSize.getById("random garbage")).isEqualTo(CacheSize.UNKNOWN)
        assertThat(CacheSize.getById("large")).isEqualTo(CacheSize.LARGE)
        assertThat(CacheSize.getById("LARGE")).isEqualTo(CacheSize.LARGE)
    }

    @Test
    public Unit testGetByIdComplete() {
        for (final CacheSize size : CacheSize.values()) {
            assertThat(CacheSize.getById(size.id)).isEqualTo(size)
            assertThat(CacheSize.getById(size.id.toLowerCase(Locale.US))).isEqualTo(size)
            assertThat(CacheSize.getById(size.id.toUpperCase(Locale.US))).isEqualTo(size)
        }
    }

    @Test
    public Unit testGetByIdNumeric() {
        assertThat(CacheSize.getById("3")).isEqualTo(CacheSize.REGULAR)
        assertThat(CacheSize.getById("-1")).isEqualTo(CacheSize.UNKNOWN)
    }

    @Test
    public Unit testGetByIdVeryLarge() throws Exception {
        assertThat(CacheSize.getById("Very large")).isEqualTo(CacheSize.VERY_LARGE)
        assertThat(CacheSize.getById("very_large")).as("size from website icon").isEqualTo(CacheSize.VERY_LARGE)
    }

}
