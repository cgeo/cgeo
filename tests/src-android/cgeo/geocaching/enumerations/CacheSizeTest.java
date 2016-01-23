package cgeo.geocaching.enumerations;

import static org.assertj.core.api.Assertions.assertThat;

import android.test.AndroidTestCase;

public class CacheSizeTest extends AndroidTestCase {

    public static void testGetById() throws Exception {
        assertThat(CacheSize.getById("Very large")).isEqualTo(CacheSize.VERY_LARGE);
        assertThat(CacheSize.getById("very_large")).as("size from website icon").isEqualTo(CacheSize.VERY_LARGE);
    }

}
