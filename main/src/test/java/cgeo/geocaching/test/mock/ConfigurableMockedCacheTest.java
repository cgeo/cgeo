package cgeo.geocaching.test.mock;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ConfigurableMockedCacheTest {

    @Test
    public void testDefaults() {
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC12345");
        assertThat(cache.getGeocode()).isEqualTo("GC12345");
        assertThat(cache.getName()).isEqualTo("Test Cache");
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL);
        assertThat(cache.getSize()).isEqualTo(CacheSize.REGULAR);
        assertThat(cache.getDifficulty()).isEqualTo(1.0f);
        assertThat(cache.getTerrain()).isEqualTo(1.0f);
        assertThat(cache.isOwner()).isFalse();
        assertThat(cache.isDisabled()).isFalse();
        assertThat(cache.isArchived()).isFalse();
        assertThat(cache.isFound()).isFalse();
        assertThat(cache.isFavorite()).isFalse();
        assertThat(cache.isPremiumMembersOnly()).isFalse();
    }

    @Test
    public void testIsOwnerConfigurable() {
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC001");
        cache.setIsOwner(true);
        assertThat(cache.isOwner()).isTrue();

        cache.setIsOwner(false);
        assertThat(cache.isOwner()).isFalse();
    }
}

