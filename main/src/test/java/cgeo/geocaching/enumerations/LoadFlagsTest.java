package cgeo.geocaching.enumerations;

import java.util.EnumSet;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LoadFlagsTest {

    @Test
    public void testLoadCacheOnly() {
        final EnumSet<LoadFlags.LoadFlag> flags = LoadFlags.LOAD_CACHE_ONLY;
        assertThat(flags).contains(LoadFlags.LoadFlag.CACHE_BEFORE);
        assertThat(flags).hasSize(1);
    }

    @Test
    public void testLoadCacheOrDb() {
        final EnumSet<LoadFlags.LoadFlag> flags = LoadFlags.LOAD_CACHE_OR_DB;
        assertThat(flags).contains(
                LoadFlags.LoadFlag.CACHE_BEFORE,
                LoadFlags.LoadFlag.DB_MINIMAL,
                LoadFlags.LoadFlag.OFFLINE_LOG
        );
        assertThat(flags).hasSize(3);
    }

    @Test
    public void testLoadWaypoints() {
        final EnumSet<LoadFlags.LoadFlag> flags = LoadFlags.LOAD_WAYPOINTS;
        assertThat(flags).contains(
                LoadFlags.LoadFlag.CACHE_AFTER,
                LoadFlags.LoadFlag.DB_MINIMAL,
                LoadFlags.LoadFlag.WAYPOINTS,
                LoadFlags.LoadFlag.OFFLINE_LOG
        );
        assertThat(flags).hasSize(4);
    }

    @Test
    public void testLoadAllDbOnly() {
        final EnumSet<LoadFlags.LoadFlag> flags = LoadFlags.LOAD_ALL_DB_ONLY;
        assertThat(flags).contains(
                LoadFlags.LoadFlag.DB_MINIMAL,
                LoadFlags.LoadFlag.ATTRIBUTES,
                LoadFlags.LoadFlag.WAYPOINTS,
                LoadFlags.LoadFlag.SPOILERS,
                LoadFlags.LoadFlag.LOGS,
                LoadFlags.LoadFlag.INVENTORY,
                LoadFlags.LoadFlag.OFFLINE_LOG
        );
        assertThat(flags).doesNotContain(LoadFlags.LoadFlag.CACHE_BEFORE, LoadFlags.LoadFlag.CACHE_AFTER);
    }

    @Test
    public void testSaveAll() {
        final EnumSet<LoadFlags.SaveFlag> flags = LoadFlags.SAVE_ALL;
        assertThat(flags).contains(LoadFlags.SaveFlag.CACHE, LoadFlags.SaveFlag.DB);
        assertThat(flags).hasSize(2);
    }

    @Test
    public void testRemoveAll() {
        final EnumSet<LoadFlags.RemoveFlag> flags = LoadFlags.REMOVE_ALL;
        assertThat(flags).contains(LoadFlags.RemoveFlag.CACHE, LoadFlags.RemoveFlag.DB);
        assertThat(flags).doesNotContain(LoadFlags.RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING);
        assertThat(flags).hasSize(2);
    }

    @Test
    public void testLoadFlagEnum() {
        assertThat(LoadFlags.LoadFlag.valueOf("CACHE_BEFORE")).isEqualTo(LoadFlags.LoadFlag.CACHE_BEFORE);
        assertThat(LoadFlags.LoadFlag.valueOf("DB_MINIMAL")).isEqualTo(LoadFlags.LoadFlag.DB_MINIMAL);
        assertThat(LoadFlags.LoadFlag.valueOf("WAYPOINTS")).isEqualTo(LoadFlags.LoadFlag.WAYPOINTS);
    }

    @Test
    public void testSaveFlagEnum() {
        assertThat(LoadFlags.SaveFlag.valueOf("CACHE")).isEqualTo(LoadFlags.SaveFlag.CACHE);
        assertThat(LoadFlags.SaveFlag.valueOf("DB")).isEqualTo(LoadFlags.SaveFlag.DB);
    }

    @Test
    public void testRemoveFlagEnum() {
        assertThat(LoadFlags.RemoveFlag.valueOf("CACHE")).isEqualTo(LoadFlags.RemoveFlag.CACHE);
        assertThat(LoadFlags.RemoveFlag.valueOf("DB")).isEqualTo(LoadFlags.RemoveFlag.DB);
        assertThat(LoadFlags.RemoveFlag.valueOf("OWN_WAYPOINTS_ONLY_FOR_TESTING")).isEqualTo(LoadFlags.RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING);
    }
}
