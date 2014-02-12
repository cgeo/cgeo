package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.filter.SizeFilter.Factory;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * singleton registry of all available filter components
 *
 */
public class FilterRegistry {
    private final List<FactoryEntry> registry = new ArrayList<>();
    private static Resources res;

    static class FactoryEntry {
        private final String name;
        private final @NonNull Class<? extends IFilterFactory> filterFactory;

        public FactoryEntry(final String name, final @NonNull Class<? extends IFilterFactory> filterFactory) {
            this.name = name;
            this.filterFactory = filterFactory;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        public Class<? extends IFilterFactory> getFactory() {
            return filterFactory;
        }
    }

    private static class SingletonHolder {
        private static final FilterRegistry INSTANCE = new FilterRegistry();
    }

    public static FilterRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private FilterRegistry() {
        res = CgeoApplication.getInstance().getResources();
        register(R.string.caches_filter_type, TypeFilter.Factory.class);
        register(R.string.caches_filter_size, SizeFilter.Factory.class);
        register(R.string.cache_terrain, TerrainFilter.Factory.class);
        register(R.string.cache_difficulty, DifficultyFilter.Factory.class);
        register(R.string.cache_attributes, AttributeFilter.Factory.class);
        register(R.string.cache_status, StateFilterFactory.class);
        register(R.string.caches_filter_origin, OriginFilter.Factory.class);
        register(R.string.caches_filter_distance, DistanceFilter.Factory.class);
        register(R.string.caches_filter_popularity, PopularityFilter.Factory.class);
        register(R.string.caches_filter_popularity_ratio, PopularityRatioFilter.Factory.class);
        register(R.string.caches_filter_personal_data, PersonalDataFilterFactory.class);
    }

    private void register(final int resourceId, final @NonNull Class<? extends IFilterFactory> factoryClass) {
        registry.add(new FactoryEntry(res.getString(resourceId), factoryClass));
    }

    public String getFactoryName(final Class<Factory> factoryClass) {
        for (final FactoryEntry entry : registry) {
            if (entry.filterFactory == factoryClass) {
                return entry.name;
            }
        }
        return StringUtils.EMPTY;
    }

    public List<FactoryEntry> getFactories() {
        return Collections.unmodifiableList(registry);
    }
}
