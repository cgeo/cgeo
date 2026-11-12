package cgeo.geocaching.unifiedmap.mapsforge;

import java.io.File;
import java.io.IOException;

import io.reactivex.rxjava3.schedulers.TestScheduler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.assertj.core.api.Assertions.assertThat;

public class BackgroundPurgingFileSystemTileCacheTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private static void createTileFiles(final File cacheDir) throws IOException {
        for (int zoom = 10; zoom < 12; zoom++) {
            for (int x = 0; x < 3; x++) {
                final File dir = new File(cacheDir, zoom + "/" + x);
                assertThat(dir.mkdirs()).isTrue();
                for (int y = 0; y < 3; y++) {
                    assertThat(new File(dir, y + ".tile").createNewFile()).isTrue();
                }
            }
        }
    }

    private File[] trashDirs(final String id) {
        final File[] result = tempFolder.getRoot().listFiles((dir, name) -> name.startsWith(id + BackgroundPurgingFileSystemTileCache.TRASH_MARKER));
        assertThat(result).isNotNull();
        return result;
    }

    @Test
    public void testPurgeDeletesInBackground() throws IOException {
        final File cacheDir = tempFolder.newFolder("mapcache");
        final TestScheduler scheduler = new TestScheduler();
        final BackgroundPurgingFileSystemTileCache cache = new BackgroundPurgingFileSystemTileCache(100, cacheDir, null, scheduler);
        createTileFiles(cacheDir);

        cache.purge();

        // cache directory is gone immediately, its content is moved to a trash directory which is not yet deleted
        assertThat(cacheDir).doesNotExist();
        assertThat(trashDirs("mapcache")).hasSize(1);

        // background deletion removes trash directory
        scheduler.triggerActions();
        assertThat(trashDirs("mapcache")).isEmpty();
    }

    @Test
    public void testDestroyPurgesInBackground() throws IOException {
        final File cacheDir = tempFolder.newFolder("mapcache");
        final TestScheduler scheduler = new TestScheduler();
        final BackgroundPurgingFileSystemTileCache cache = new BackgroundPurgingFileSystemTileCache(100, cacheDir, null, scheduler);
        createTileFiles(cacheDir);

        cache.destroy();

        assertThat(cacheDir).doesNotExist();
        assertThat(trashDirs("mapcache")).hasSize(1);
        scheduler.triggerActions();
        assertThat(trashDirs("mapcache")).isEmpty();
    }

    @Test
    public void testDeleteLeftovers() throws IOException {
        final File leftover = tempFolder.newFolder("mapcache" + BackgroundPurgingFileSystemTileCache.TRASH_MARKER + "123");
        createTileFiles(leftover);
        final File otherCache = tempFolder.newFolder("othercache" + BackgroundPurgingFileSystemTileCache.TRASH_MARKER + "123");
        final File activeCache = tempFolder.newFolder("mapcache");

        final TestScheduler scheduler = new TestScheduler();
        BackgroundPurgingFileSystemTileCache.deleteLeftoversInBackground(tempFolder.getRoot(), "mapcache", scheduler);
        assertThat(leftover).exists();

        scheduler.triggerActions();
        assertThat(leftover).doesNotExist();
        assertThat(otherCache).exists();
        assertThat(activeCache).exists();
    }

}
