package cgeo.geocaching.files;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.StaticMapsProvider;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import android.os.Handler;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CancellationException;

abstract class AbstractImportThread extends Thread {
    final int listId;
    final Handler importStepHandler;
    final CancellableHandler progressHandler;

    protected AbstractImportThread(final int listId, final Handler importStepHandler, final CancellableHandler progressHandler) {
        this.listId = listId;
        this.importStepHandler = importStepHandler;
        this.progressHandler = progressHandler;
    }

    @Override
    public void run() {
        try {
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_START));
            final Collection<Geocache> caches = doImport();
            Log.i("Imported successfully " + caches.size() + " caches.");

            final SearchResult search = new SearchResult(caches);
            // Do not put imported caches into the cachecache. That would consume lots of memory for no benefit.

            if (Settings.isStoreOfflineMaps() || Settings.isStoreOfflineWpMaps()) {
                importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, R.string.gpx_import_store_static_maps, search.getCount()));
                final boolean finishedWithoutCancel = importStaticMaps(search);
                // Skip last message if static maps where canceled
                if (!finishedWithoutCancel) {
                    return;
                }
            }

            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED, search.getCount(), 0, search));
        } catch (final IOException e) {
            Log.i("Importing caches failed - error reading data: ", e);
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_io, 0, e.getLocalizedMessage()));
        } catch (final ParserException e) {
            Log.i("Importing caches failed - data format error", e);
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_parser, 0, e.getLocalizedMessage()));
        } catch (final CancellationException ignored) {
            Log.i("Importing caches canceled");
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_CANCELED));
        } catch (final Exception e) {
            Log.e("Importing caches failed - unknown error: ", e);
            importStepHandler.sendMessage(importStepHandler.obtainMessage(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, R.string.gpx_import_error_unexpected, 0, e.getLocalizedMessage()));
        }
    }

    protected abstract Collection<Geocache> doImport() throws IOException, ParserException;

    private boolean importStaticMaps(final SearchResult importedCaches) {
        int storedCacheMaps = 0;
        for (final String geocode : importedCaches.getGeocodes()) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
            if (cache != null) {
                Log.d("GPXImporter.ImportThread.importStaticMaps start downloadMaps for cache " + geocode);
                RxUtils.waitForCompletion(StaticMapsProvider.downloadMaps(cache));
            } else {
                Log.d("GPXImporter.ImportThread.importStaticMaps: no data found for " + geocode);
            }
            storedCacheMaps++;
            if (progressHandler.isCancelled()) {
                return false;
            }
            progressHandler.sendMessage(progressHandler.obtainMessage(0, storedCacheMaps, 0));
        }
        return true;
    }
}