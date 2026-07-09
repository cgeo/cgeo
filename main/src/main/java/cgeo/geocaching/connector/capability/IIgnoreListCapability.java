package cgeo.geocaching.connector.capability;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.List;

/**
 * Extension of {@link IIgnoreCapability} for connectors that also allow reading back the full
 * content of the online ignore list, so that it can be mirrored into a local list.
 */
public interface IIgnoreListCapability extends IIgnoreCapability {

    /**
     * Fetch the caches currently on the connector's online ignore list.
     *
     * @return the caches on the online ignore list, or {@code null} on error
     */
    @WorkerThread
    @Nullable
    List<Geocache> fetchIgnoreList();
}
