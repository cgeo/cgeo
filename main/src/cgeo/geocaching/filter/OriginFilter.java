package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;

import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OriginFilter extends AbstractFilter {

    private static final long serialVersionUID = -226131408218792315L;
    private final IConnector connector;

    public OriginFilter(@NonNull final IConnector connector) {
        super(connector.getName());
        this.connector = connector;
    }

    @Override
    public final boolean accepts(@NonNull final Geocache cache) {
        return ConnectorFactory.getConnector(cache).getName().equalsIgnoreCase(connector.getName());
    }

    public static final class Factory implements IFilterFactory {

        @Override
        @NonNull
        public List<OriginFilter> getFilters() {
            final List<OriginFilter> filters = new ArrayList<>();
            for (final IConnector connector : ConnectorFactory.getConnectors()) {
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
