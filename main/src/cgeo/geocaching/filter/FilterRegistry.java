package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.filter.SizeFilter.Factory;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * singleton registry of all available filter components
 *
 */
public class FilterRegistry {
    static class FactoryEntry {
        private final String name;
        @NonNull private final Class<? extends IFilterFactory> filterFactory;

        FactoryEntry(final String name, @NonNull final Class<? extends IFilterFactory> filterFactory) {
            this.name = name;
            this.filterFactory = filterFactory;
        }

        public Class<? extends IFilterFactory> getFactory() {
            return filterFactory;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class SingletonHolder {
        private static final FilterRegistry INSTANCE = new FilterRegistry();
    }

    public static FilterRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final List<FactoryEntry> registry = new ArrayList<>();

    private FilterRegistry() {
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

    public List<FactoryEntry> getFactories() {
        return Collections.unmodifiableList(registry);
    }

    public String getFactoryName(final Class<Factory> factoryClass) {
        for (final FactoryEntry entry : registry) {
            if (entry.filterFactory == factoryClass) {
                return entry.name;
            }
        }
        return StringUtils.EMPTY;
    }

    private void register(@StringRes final int resourceId, @NonNull final Class<? extends IFilterFactory> factoryClass) {
        final Resources res = CgeoApplication.getInstance().getResources();
        registry.add(new FactoryEntry(res.getString(resourceId), factoryClass));
    }
}
