package cgeo.geocaching.test.mock;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;

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

    @Test
    public void testIsDisabledConfigurable() {
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC002");
        cache.setDisabled(true);
        assertThat(cache.isDisabled()).isTrue();

        cache.setDisabled(false);
        assertThat(cache.isDisabled()).isFalse();
    }

    @Test
    public void testDisabledAndArchivedInteraction() {
        // Geocache.isDisabled() returns false when archived, even if disabled flag is set
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC003");
        cache.setDisabled(true);
        cache.setArchived(true);
        assertThat(cache.isArchived()).isTrue();
        assertThat(cache.isDisabled()).as("disabled should be false when archived").isFalse();
        assertThat(cache.isEnabled()).isFalse();
    }

    @Test
    public void testIsArchivedConfigurable() {
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC004");
        cache.setArchived(true);
        assertThat(cache.isArchived()).isTrue();
        assertThat(cache.isEnabled()).isFalse();

        cache.setArchived(false);
        assertThat(cache.isArchived()).isFalse();
        assertThat(cache.isEnabled()).isTrue();
    }

    @Test
    public void testAllProperties() {
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC005");
        cache.setIsOwner(true);
        cache.setDisabled(true);
        cache.setFound(true);
        cache.setFavorite(true);
        cache.setFavoritePoints(42);
        cache.setPremiumMembersOnly(true);
        cache.setOnWatchlist(true);
        cache.setName("All Props Test");
        cache.setType(CacheType.MULTI);
        cache.setSize(CacheSize.SMALL);
        cache.setDifficulty(3.5f);
        cache.setTerrain(4.0f);
        cache.setCoords(new Geopoint(52.0, 9.0));
        cache.setOwnerDisplayName("TestOwner");
        cache.setOwnerUserId("testowner");
        cache.setPersonalNote("My note");
        cache.setHint("Look under the rock");

        assertThat(cache.isOwner()).isTrue();
        assertThat(cache.isDisabled()).isTrue();
        assertThat(cache.isFound()).isTrue();
        assertThat(cache.isFavorite()).isTrue();
        assertThat(cache.getFavoritePoints()).isEqualTo(42);
        assertThat(cache.isPremiumMembersOnly()).isTrue();
        assertThat(cache.isOnWatchlist()).isTrue();
        assertThat(cache.getName()).isEqualTo("All Props Test");
        assertThat(cache.getType()).isEqualTo(CacheType.MULTI);
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getDifficulty()).isEqualTo(3.5f);
        assertThat(cache.getTerrain()).isEqualTo(4.0f);
        assertThat(cache.getCoords()).isEqualTo(new Geopoint(52.0, 9.0));
        assertThat(cache.getOwnerDisplayName()).isEqualTo("TestOwner");
        assertThat(cache.getOwnerUserId()).isEqualTo("testowner");
        assertThat(cache.getPersonalNote()).isEqualTo("My note");
        assertThat(cache.getHint()).isEqualTo("Look under the rock");
    }

    @Test
    public void testIsEnabledState() {
        final ConfigurableMockedCache cache = new ConfigurableMockedCache("GC006");
        assertThat(cache.isEnabled()).isTrue();

        cache.setDisabled(true);
        assertThat(cache.isEnabled()).isFalse();

        cache.setDisabled(false);
        assertThat(cache.isEnabled()).isTrue();

        cache.setArchived(true);
        assertThat(cache.isEnabled()).isFalse();
    }
}

