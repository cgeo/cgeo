package cgeo.geocaching.connector.wm;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.NetworkUtils;

final class WMApi {
    private static final String WAYMARK_URI = "https://www.waymarking.com/waymarks/%s";
    private static final String GALLERY_URI = "https://www.waymarking.com/gallery/default.aspx?f=1&gid=2&guid=%s";

    @Nullable
    public static Geocache searchByGeocode(@NonNull final String geocode, final DisposableHandler handler) {
        try {
            DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

            final String waymarkUri = String.format(WAYMARK_URI, geocode);
            final String waymarkHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(waymarkUri).blockingGet());
            final Pair<Geocache, Integer> cachePair = WMParser.parseCoreWaymark(waymarkHtml);
            if (cachePair == null) return null;

            final Geocache cache = cachePair.first;
            final int imageCount = cachePair.second;

            final String cacheId = cache.getCacheId();
            if (imageCount > 0 && !StringUtils.isEmpty(cacheId)) {
                DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_spoilers);

                final String galleryUri = String.format(GALLERY_URI, cacheId);
                final String galleryHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(galleryUri).blockingGet());

                final List<Image> spoilers = new ArrayList<>();
                final List<String> imagePageUris = WMParser.getImagePageUris(galleryHtml);
                for (final String imagePageUri : imagePageUris) {
                    final String imagePageHtml = NetworkUtils.getResponseBodyOrStatus(Network.getRequest(imagePageUri).blockingGet());
                    final Image image = WMParser.parseImage(imagePageHtml);
                    if (image != null) {
                        spoilers.add(image);
                    }
                }
                cache.setSpoilers(spoilers);
            }

            //TODO logs
            //TODO found state

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
            return cache;
        } catch (final Exception ex) {
            Log.w("WMApi: Exception while getting " + geocode, ex);
            return null;
        }
    }
}
