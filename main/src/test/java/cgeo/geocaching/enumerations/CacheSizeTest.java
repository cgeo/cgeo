package cgeo.geocaching.enumerations;

import java.util.Locale;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CacheSizeTest {

    @Test
    public void testOrder() {
        assertThat(CacheSize.MICRO.comparable).isLessThan(CacheSize.SMALL.comparable);
        assertThat(CacheSize.SMALL.comparable).isLessThan(CacheSize.REGULAR.comparable);
        assertThat(CacheSize.REGULAR.comparable).isLessThan(CacheSize.LARGE.comparable);
    }

    @Test
    public void testGetById() {
        assertThat(CacheSize.getById("")).isEqualTo(CacheSize.UNKNOWN);
        assertThat(CacheSize.getById(null)).isEqualTo(CacheSize.UNKNOWN);
        assertThat(CacheSize.getById("random garbage")).isEqualTo(CacheSize.UNKNOWN);
        assertThat(CacheSize.getById("large")).isEqualTo(CacheSize.LARGE);
        assertThat(CacheSize.getById("LARGE")).isEqualTo(CacheSize.LARGE);
    }

    @Test
    public void testGetByIdComplete() {
        for (final CacheSize size : CacheSize.values()) {
            assertThat(CacheSize.getById(size.id)).isEqualTo(size);
            assertThat(CacheSize.getById(size.id.toLowerCase(Locale.US))).isEqualTo(size);
            assertThat(CacheSize.getById(size.id.toUpperCase(Locale.US))).isEqualTo(size);
        }
    }

    @Test
    public void testGetByIdNumeric() {
        assertThat(CacheSize.getById("3")).isEqualTo(CacheSize.REGULAR);
        assertThat(CacheSize.getById("-1")).isEqualTo(CacheSize.UNKNOWN);
    }

    @Test
    public void testGetByIdVeryLarge() throws Exception {
        assertThat(CacheSize.getById("Very large")).isEqualTo(CacheSize.VERY_LARGE);
        assertThat(CacheSize.getById("very_large")).as("size from website icon").isEqualTo(CacheSize.VERY_LARGE);
    }

}
