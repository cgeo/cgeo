package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.sorting.GeocacheSort;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class OwnerGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @Nullable public final String username;
    @Nullable public final Map<String, String> connectorOwnerMap;

    public OwnerGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final String username) {
        super(activity, sort);
        this.username = username;
        this.connectorOwnerMap = null;
    }

    public OwnerGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final Map<String, String> connectorOwnerMap) {
        super(activity, sort);
        this.username = null;
        this.connectorOwnerMap = new HashMap<>(connectorOwnerMap);
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.OWNER;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        if (connectorOwnerMap != null && !connectorOwnerMap.isEmpty()) {
            // Create multiple OWNER filters combined with OR
            final OrGeocacheFilter orFilter = new OrGeocacheFilter();
            for (final Map.Entry<String, String> entry : connectorOwnerMap.entrySet()) {
                final OwnerGeocacheFilter ownerFilter = GeocacheFilterType.OWNER.create();
                ownerFilter.getStringFilter().setTextValue(entry.getValue());
                orFilter.addChild(ownerFilter);
            }
            return orFilter;
        } else if (username != null) {
            // Single username mode (backward compatible)
            final OwnerGeocacheFilter ownerFilter = GeocacheFilterType.OWNER.create();
            ownerFilter.getStringFilter().setTextValue(username);
            return ownerFilter;
        }
        
        // Return empty filter if nothing specified
        return GeocacheFilterType.OWNER.create();
    }
}
