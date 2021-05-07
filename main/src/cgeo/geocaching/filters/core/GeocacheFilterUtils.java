package cgeo.geocaching.filters.core;

import cgeo.geocaching.utils.expressions.ExpressionParser;

import androidx.annotation.NonNull;

import java.text.ParseException;

public class GeocacheFilterUtils {



    private static final ExpressionParser<IGeocacheFilter> FILTER_PARSER = new ExpressionParser<IGeocacheFilter>()
        .register(AndGeocacheFilter::new)
        .register(OrGeocacheFilter::new)
        .register(NotGeocacheFilter::new)
        .register(InconclusiveGeocacheFilter::new);

    static {
        for (GeocacheFilterType gcf : GeocacheFilterType.values()) {
            FILTER_PARSER.register(() -> gcf.create());
        }
    }

    private GeocacheFilterUtils() {
        //no instane shall be created
    }

    public static IGeocacheFilter createFilter(@NonNull final String filterConfig) throws ParseException {
        return FILTER_PARSER.create(filterConfig);
    }

    public static String getFilterConfig(@NonNull final IGeocacheFilter filter) {
        return FILTER_PARSER.getConfig(filter);
    }

}
