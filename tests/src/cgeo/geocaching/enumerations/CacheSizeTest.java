package cgeo.geocaching.enumerations;

import static org.assertj.core.api.Assertions.assertThat;

import android.test.AndroidTestCase;

import java.util.Locale;

public class CacheSizeTest extends AndroidTestCase {

    public static void testOrder() {
        assertThat(CacheSize.MICRO.comparable < CacheSize.SMALL.comparable).isTrue();
        assertThat(CacheSize.SMALL.comparable < CacheSize.REGULAR.comparable).isTrue();
        assertThat(CacheSize.REGULAR.comparable < CacheSize.LARGE.comparable).isTrue();
    }

    public static void testGetById() {
        assertThat(CacheSize.getById("")).isEqualTo(CacheSize.UNKNOWN);
        assertThat(CacheSize.getById(null)).isEqualTo(CacheSize.UNKNOWN);
        assertThat(CacheSize.getById("random garbage")).isEqualTo(CacheSize.UNKNOWN);
        assertThat(CacheSize.getById("large")).isEqualTo(CacheSize.LARGE);
        assertThat(CacheSize.getById("LARGE")).isEqualTo(CacheSize.LARGE);
    }

    public static void testGetByIdComplete() {
        for (CacheSize size : CacheSize.values()) {
            assertThat(CacheSize.getById(size.id)).isEqualTo(size);
            assertThat(CacheSize.getById(size.id.toLowerCase(Locale.US))).isEqualTo(size);
            assertThat(CacheSize.getById(size.id.toUpperCase(Locale.US))).isEqualTo(size);
        }
    }

    public static void testGetByIdNumeric() {
        assertThat(CacheSize.getById("3")).isEqualTo(CacheSize.REGULAR);
        assertThat(CacheSize.getById("-1")).isEqualTo(CacheSize.UNKNOWN);
    }
}
