package cgeo.geocaching.unifiedmap.layers.mbtiles;

/* based upon work of (c) 2023 Christian Pesch, https://github.com/cpesch */

import cgeo.geocaching.utils.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.JobQueue;

class MBTilesMapWorkerPool implements Runnable {
    private final MBTilesRenderer renderer;
    private boolean inShutdown, isRunning;
    private final JobQueue<MBTilesRendererJob> jobQueue;
    private final Layer layer;
    private ExecutorService self, workers;
    private final TileCache tileCache;

    MBTilesMapWorkerPool(final TileCache tileCache, final JobQueue<MBTilesRendererJob> jobQueue, final MBTilesRenderer renderer, final MBTilesLayer layer) {
        super();
        this.tileCache = tileCache;
        this.jobQueue = jobQueue;
        this.renderer = renderer;
        this.layer = layer;
        this.inShutdown = false;
        this.isRunning = false;
    }

    public void run() {
        try {
            while (!inShutdown) {
                final MBTilesRendererJob rendererJob = this.jobQueue.get(Parameters.NUMBER_OF_THREADS);
                if (rendererJob == null) {
                    continue;
                }
                if (!this.tileCache.containsKey(rendererJob)) {
                    workers.execute(new MapWorker(rendererJob));
                } else {
                    jobQueue.remove(rendererJob);
                }
            }
        } catch (InterruptedException e) {
            Log.e("MapWorkerPool interrupted", e);
        } catch (RejectedExecutionException e) {
            Log.e("MapWorkerPool rejected", e);
        }
    }

    public synchronized void start() {
        if (this.isRunning) {
            return;
        }
        this.inShutdown = false;
        this.self = Executors.newSingleThreadExecutor();
        this.workers = Executors.newFixedThreadPool(Parameters.NUMBER_OF_THREADS);
        this.self.execute(this);
        this.isRunning = true;
    }

    public synchronized void stop() {
        if (!this.isRunning) {
            return;
        }
        this.inShutdown = true;
        this.jobQueue.interrupt();

        // Shutdown executors
        this.self.shutdown();
        this.workers.shutdown();

        try {
            if (!this.self.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.self.shutdownNow();
                if (!this.self.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    Log.w("Shutdown self executor failed");
                }
            }
        } catch (InterruptedException e) {
            Log.e("Shutdown self executor interrupted", e);
        }

        try {
            if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                this.workers.shutdownNow();
                if (!this.workers.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    Log.w("Shutdown workers executor failed");
                }
            }
        } catch (InterruptedException e) {
            Log.e("Shutdown workers executor interrupted", e);
        }

        this.isRunning = false;
    }

    class MapWorker implements Runnable {
        private final MBTilesRendererJob rendererJob;

        MapWorker(final MBTilesRendererJob rendererJob) {
            this.rendererJob = rendererJob;
        }

        public void run() {
            TileBitmap bitmap = null;
            try {
                if (inShutdown) {
                    return;
                }
                bitmap = renderer.executeJob(rendererJob);
                if (inShutdown) {
                    return;
                }

                if (bitmap != null) {
                    tileCache.put(rendererJob, bitmap);
                }
                layer.requestRedraw();
            } finally {
                jobQueue.remove(rendererJob);
                if (bitmap != null) {
                    bitmap.decrementRefCount();
                }
            }
        }
    }
}
