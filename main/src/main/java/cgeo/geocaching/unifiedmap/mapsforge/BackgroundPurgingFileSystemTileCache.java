package cgeo.geocaching.unifiedmap.mapsforge;

import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.File;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;

/**
 * Non-persistent {@link FileSystemTileCache} whose {@link #purge()} does not block the calling thread.
 * <br>
 * Mapsforge's implementation deletes the whole cache directory synchronously on {@link #purge()}
 * (which is also called by {@link #destroy()} for non-persistent caches). With many cached tiles this
 * may take several seconds and lead to ANRs when called on UI thread (e.g. when closing the map).
 * <br>
 * This implementation renames the cache directory first (a single, fast file system operation)
 * and deletes the renamed directory in background.
 */
public class BackgroundPurgingFileSystemTileCache extends FileSystemTileCache {

    @VisibleForTesting
    static final String TRASH_MARKER = ".trash-";

    @NonNull
    private final File cacheDirectory;
    @NonNull
    private final Scheduler deleteScheduler;

    public BackgroundPurgingFileSystemTileCache(final int capacity, @NonNull final File cacheDirectory, final GraphicFactory graphicFactory) {
        this(capacity, cacheDirectory, graphicFactory, Schedulers.io());
    }

    @VisibleForTesting
    BackgroundPurgingFileSystemTileCache(final int capacity, @NonNull final File cacheDirectory, final GraphicFactory graphicFactory, @NonNull final Scheduler deleteScheduler) {
        super(capacity, cacheDirectory, graphicFactory, false);
        this.cacheDirectory = cacheDirectory;
        this.deleteScheduler = deleteScheduler;
    }

    @Override
    public void purge() {
        final File trash = new File(cacheDirectory.getParentFile(), cacheDirectory.getName() + TRASH_MARKER + System.nanoTime());
        if (cacheDirectory.exists() && cacheDirectory.renameTo(trash)) {
            deleteScheduler.scheduleDirect(() -> FileUtils.deleteDirectory(trash));
        }
        // clears the working set. Remaining directory (if any, e.g. if renaming failed or new tiles were written meanwhile) is deleted synchronously
        super.purge();
    }

    /**
     * Creates a two-level tile cache (memory + file system) analogous to
     * {@link AndroidUtil#createTileCache(Context, String, int, float, double)}, but using a
     * {@link BackgroundPurgingFileSystemTileCache} as second level cache.
     * Falls back to a memory-only cache if no usable cache directory is available.
     */
    @NonNull
    public static TileCache createTileCache(@NonNull final Context context, @NonNull final String id, final int tileSize, final float screenRatio, final double overdrawFactor) {
        final int memoryCacheSize = AndroidUtil.getMinimumCacheSize(context, tileSize, overdrawFactor, screenRatio);
        final InMemoryTileCache memoryCache = new InMemoryTileCache(memoryCacheSize);

        final File cacheRoot = context.getExternalCacheDir();
        if (cacheRoot == null) {
            return memoryCache;
        }
        deleteLeftoversInBackground(cacheRoot, id, Schedulers.io());

        final File cacheDirectory = new File(cacheRoot, id);
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            return memoryCache;
        }
        final int fileCacheSize = AndroidUtil.estimateSizeOfFileSystemCache(cacheDirectory.getAbsolutePath(), memoryCacheSize, tileSize);
        if (!cacheDirectory.canWrite() || fileCacheSize <= 0) {
            return memoryCache;
        }
        try {
            return new TwoLevelTileCache(memoryCache, new BackgroundPurgingFileSystemTileCache(fileCacheSize, cacheDirectory, AndroidGraphicFactory.INSTANCE));
        } catch (final IllegalArgumentException iae) {
            Log.w("Could not create file system tile cache in " + cacheDirectory, iae);
            return memoryCache;
        }
    }

    /**
     * removes leftovers of earlier purges, e.g. if the process was killed before background deletion was finished
     */
    @VisibleForTesting
    static void deleteLeftoversInBackground(@NonNull final File cacheRoot, @NonNull final String id, @NonNull final Scheduler scheduler) {
        final String prefix = id + TRASH_MARKER;
        scheduler.scheduleDirect(() -> {
            final File[] leftovers = cacheRoot.listFiles((dir, name) -> name.startsWith(prefix));
            if (leftovers != null) {
                for (final File leftover : leftovers) {
                    if (leftover.isDirectory()) {
                        FileUtils.deleteDirectory(leftover);
                    }
                }
            }
        });
    }

}
