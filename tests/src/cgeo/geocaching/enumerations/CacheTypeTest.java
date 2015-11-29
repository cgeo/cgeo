package cgeo.geocaching.enumerations;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;

import android.test.AndroidTestCase;

import java.util.Locale;

public class CacheTypeTest extends AndroidTestCase {

    public static void testGetById() {
        assertThat(CacheType.getById("")).isEqualTo(CacheType.UNKNOWN);
        assertThat(CacheType.getById(null)).isEqualTo(CacheType.UNKNOWN);
        assertThat(CacheType.getById("random garbage")).isEqualTo(CacheType.UNKNOWN);
        assertThat(CacheType.getById("wherigo")).isEqualTo(CacheType.WHERIGO);
    }

    public static void testGetByPattern() {
        assertThat(CacheType.getByPattern("")).isEqualTo(CacheType.UNKNOWN);
        assertThat(CacheType.getByPattern(null)).isEqualTo(CacheType.UNKNOWN);
        assertThat(CacheType.getByPattern("random garbage")).isEqualTo(CacheType.UNKNOWN);
        assertThat(CacheType.getByPattern("cache in trash out event")).isEqualTo(CacheType.CITO);
    }

    public static void testGetByIdComplete() {
        for (CacheType type : CacheType.values()) {
            assertThat(CacheType.getById(type.id)).isEqualTo(type);
            assertThat(CacheType.getById(type.id.toLowerCase(Locale.US))).isEqualTo(type);
            assertThat(CacheType.getById(type.id.toUpperCase(Locale.US))).isEqualTo(type);
        }
    }

    public static void testGetByPatternComplete() {
        for (CacheType type : CacheType.values()) {
            assertThat(CacheType.getByPattern(type.pattern)).isEqualTo(type);
            assertThat(CacheType.getByPattern(type.pattern.toLowerCase(Locale.US))).isEqualTo(type);
            assertThat(CacheType.getByPattern(type.pattern.toUpperCase(Locale.US))).isEqualTo(type);
        }
    }

    public static void testContainsCache() {
        final Geocache traditional = new Geocache();
        traditional.setType(CacheType.TRADITIONAL);

        assertThat(CacheType.ALL.contains(traditional)).isTrue();
        assertThat(CacheType.TRADITIONAL.contains(traditional)).isTrue();
        assertThat(CacheType.MYSTERY.contains(traditional)).isFalse();
    }

    public static void testEventCacheTypes() throws Exception {
        assertThat(CacheType.EVENT.isEvent()).isTrue();
        assertThat(CacheType.MEGA_EVENT.isEvent()).isTrue();
        assertThat(CacheType.GIGA_EVENT.isEvent()).isTrue();
        assertThat(CacheType.BLOCK_PARTY.isEvent()).isTrue();
        assertThat(CacheType.CITO.isEvent()).isTrue();
        assertThat(CacheType.LOSTANDFOUND.isEvent()).isTrue();
        assertThat(CacheType.TRADITIONAL.isEvent()).isFalse();
    }
}
