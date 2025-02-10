package cgeo.geocaching.files;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.DisposableHandler;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;

class GPXMultiParserAbstractFiles {

    protected void showProgressMessage(@Nullable final DisposableHandler handler, final int bytesRead) {
        if (handler != null) {
            if (handler.isDisposed()) {
                throw new CancellationException();
            }
            handler.sendMessage(handler.obtainMessage(0, bytesRead, 0));
        }
    }

    protected void fixCache(final Geocache cache) {
        final List<Trackable> inventory = cache.getInventory();
        cache.setInventoryItems(inventory.size());
        final long time = System.currentTimeMillis();
        cache.setUpdated(time);
        cache.setDetailedUpdate(time);

        // fix potentially bad cache id
        if (GCConnector.getInstance().equals(ConnectorFactory.getConnector(cache))) {
            cache.setCacheId(String.valueOf(GCUtils.gcLikeCodeToGcLikeId(cache.getGeocode())));
        }
    }

}
