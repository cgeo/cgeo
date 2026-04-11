package cgeo.geocaching.loaders;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ILogin;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.sorting.GeocacheSort;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class OwnerGeocacheListLoader extends LiveFilterGeocacheListLoader {

    @Nullable
    public final String username;
    @Nullable
    public final List<IConnector> connectorList;

    public OwnerGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final String username) {
        super(activity, sort);
        this.username = username;
        this.connectorList = null;
    }

    public OwnerGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final List<IConnector> connectorList) {
        super(activity, sort);
        this.username = null;
        this.connectorList = new ArrayList<>(connectorList);
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.OWNER;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        if (connectorList != null && !connectorList.isEmpty()) {
            // Create multiple OWNER filters combined with OR
            final OrGeocacheFilter orFilter = new OrGeocacheFilter();
            for (final IConnector connector : connectorList) {
                final IGeocacheFilter connectorFilter = createFilterForConnector(connector);
                if (connectorFilter != null) {
                    orFilter.addChild(connectorFilter);
                }
            }
            return orFilter.isFiltering() ? orFilter : GeocacheFilterType.OWNER.create();
        } else if (username != null) {
            // Single username mode (backward compatible)
            final OwnerGeocacheFilter ownerFilter = GeocacheFilterType.OWNER.create();
            ownerFilter.getStringFilter().setTextValue(username);
            return ownerFilter;
        }

        // Return empty filter if nothing specified
        return GeocacheFilterType.OWNER.create();
    }

    @Nullable
    private static IGeocacheFilter createFilterForConnector(@NonNull final IConnector connector) {
        if (connector instanceof ILogin) {
            final ILogin loginConnector = (ILogin) connector;
            final String username = loginConnector.getUserName();
            if (StringUtils.isNotEmpty(username)) {
                final AndGeocacheFilter andGeocacheFilter = new AndGeocacheFilter();

                final OwnerGeocacheFilter ownerFilter = GeocacheFilterType.OWNER.create();
                ownerFilter.getStringFilter().setTextValue(username);
                andGeocacheFilter.addChild(ownerFilter);
                final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
                originFilter.setValues(Collections.singletonList(connector));
                andGeocacheFilter.addChild(originFilter);

                return andGeocacheFilter;
            }
        }
        return null;
    }
}
