package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OriginFilter extends AbstractFilter {

    private final IConnector connector;

    public OriginFilter(final IConnector connector) {
        super(connector.getName());
        this.connector = connector;
    }

    @Override
    public final boolean accepts(final Geocache cache) {
        return ConnectorFactory.getConnector(cache) == connector;
    }

    public static final class Factory implements IFilterFactory {

        @Override
        public List<OriginFilter> getFilters() {
            final ArrayList<OriginFilter> filters = new ArrayList<OriginFilter>();
            for (IConnector connector : ConnectorFactory.getConnectors()) {
                filters.add(new OriginFilter(connector));
            }

            // sort connectors by name
            Collections.sort(filters, new Comparator<OriginFilter>() {

                @Override
                public int compare(final OriginFilter lhs, final OriginFilter rhs) {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            });

            return filters;
        }

    }
}
