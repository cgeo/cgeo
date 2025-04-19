package cgeo.geocaching.connector.wm;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.NetworkUtils;

import kotlin.Triple;

final class WMApi {
    private static final String WAYMARK_URI = "https://www.waymarking.com/waymarks/%s";
    private static final String GALLERY_URI = "https://www.waymarking.com/gallery/default.aspx?f=1&gid=2&guid=%s";
    private static final String SEND_LOG_URI = "https://www.waymarking.com/logs/add.aspx?f=1&logtype=1&guid=%s";
    private static final String VIEW_LOGS_URI = "https://www.waymarking.com/logs/default.aspx?f=1&r=10&st=2&guid=%s";

    @Nullable
    public static Geocache searchByGeocode(@NonNull final String geocode, final DisposableHandler handler) {
        try {
            // Get core waymark data
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);
            final String waymarkUri = String.format(WAYMARK_URI, geocode);
            final String waymarkHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(waymarkUri).blockingGet());
            final Triple<Geocache, Integer, Integer> cachePair = WMParser.parseCoreWaymark(waymarkHtml);
            if (cachePair == null) return null;

            final Geocache cache = cachePair.getFirst();
            final String cacheId = cache.getCacheId();

            List<LogEntry> logs = List.of();
            if (!StringUtils.isEmpty(cacheId) && !cache.isArchived()) {
                // Get found state
                DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_details);
                final String logUri = String.format(SEND_LOG_URI, cacheId);
                final String logHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(logUri).blockingGet());
                final boolean found = WMParser.getFoundState(logHtml);
                cache.setFound(found);

                // Get images
                final int imageCount = cachePair.getSecond();
                if (imageCount > 0) {
                    DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);
                    final String galleryUri = String.format(GALLERY_URI, cacheId);
                    final String galleryHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(galleryUri).blockingGet());
                    final List<Image> images = WMParser.getImages(galleryHtml);
                    cache.setSpoilers(images);
                }

                // Get logs
                final int logCount = cachePair.getThird();
                if (logCount > 0) {
                    DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_logs);
                    final String logsUri = String.format(VIEW_LOGS_URI, cacheId);
                    final String logsHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(logsUri).blockingGet());
                    logs = WMParser.getLogs(logsHtml);
                }
            }

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
            DataStore.saveLogs(cache.getGeocode(), logs, true);
            return cache;
        } catch (final Exception ex) {
            Log.w("WMApi: Exception while getting " + geocode, ex);
            return null;
        }
    }
}
