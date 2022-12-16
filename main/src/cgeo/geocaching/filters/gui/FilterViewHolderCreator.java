package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LastFoundGeocacheFilter;
import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StoredListGeocacheFilter;
import cgeo.geocaching.filters.core.StoredSinceGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.app.Activity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FilterViewHolderCreator {

    private static boolean listInfoFilled = false;
    private static Collection<Geocache> listInfoFilteredList = Collections.emptyList();
    private static boolean listInfoIsComplete = false;

    private FilterViewHolderCreator() {
        //no instance
    }

    public static IFilterViewHolder<?> createFor(final IGeocacheFilter filter, final Activity activity) {
        return createFor(filter.getType(), activity, filter);
    }

    public static IFilterViewHolder<?> createFor(final GeocacheFilterType type, final Activity activity) {
        return createFor(type, activity, null);
    }

    private static IFilterViewHolder<?> createFor(final GeocacheFilterType type, final Activity activity, final IGeocacheFilter filter) {
        final IFilterViewHolder<?> result;
        switch (type) {
            case NAME:
            case OWNER:
            case DESCRIPTION:
            case PERSONAL_NOTE:
            case OFFLINE_LOG:
                result = new StringFilterViewHolder<>();
                break;
            case TYPE:
                result = new CheckboxFilterViewHolder<>(
                        ValueGroupFilterAccessor.<CacheType, TypeGeocacheFilter>createForValueGroupFilter()
                                .setSelectableValues(Arrays.asList(CacheType.TRADITIONAL, CacheType.MULTI, CacheType.MYSTERY, CacheType.LETTERBOX, CacheType.EVENT,
                                        CacheType.EARTH, CacheType.CITO, CacheType.WEBCAM, CacheType.VIRTUAL, CacheType.WHERIGO, CacheType.ADVLAB, CacheType.USER_DEFINED))
                                .setValueDisplayTextGetter(CacheType::getShortL10n)
                                .setValueDrawableGetter(ct -> ImageParam.drawable(MapMarkerUtils.getCacheTypeMarker(activity.getResources(), ct))),
                        2, null);
                break;
            case SIZE:
                result = new ChipChoiceFilterViewHolder<>(
                        ValueGroupFilterAccessor.<CacheSize, SizeGeocacheFilter>createForValueGroupFilter()
                                .setSelectableValues(CacheSize.values())
                                .setValueDisplayTextGetter(CacheSize::getL10n));
                break;
            case DIFFICULTY:
            case TERRAIN:
            case RATING:
                result = create1to5ItemRangeSelectorViewHolder();
                break;
            case DIFFICULTY_TERRAIN:
                result = new DifficultyAndTerrainFilterViewHolder();
                break;
            case STATUS:
                result = new StatusFilterViewHolder();
                break;
            case ATTRIBUTES:
                result = new AttributesFilterViewHolder();
                break;
            case FAVORITES:
                result = new FavoritesFilterViewHolder();
                break;
            case DISTANCE:
                result = new DistanceFilterViewHolder();
                break;
            case HIDDEN:
                result = new DateRangeFilterViewHolder<HiddenGeocacheFilter>(true,
                        LocalizationUtils.getIntArray(R.array.cache_filter_hidden_since_stored_values_d),
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label),
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label_short));
                break;
            case LAST_FOUND:
                final int[] values = LocalizationUtils.getIntArray(R.array.cache_filter_hidden_since_stored_values_d);
                result = new DateRangeFilterViewHolder<LastFoundGeocacheFilter>(true,
                        values,
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label),
                        LocalizationUtils.getStringArray(R.array.cache_filter_hidden_since_stored_values_label_short));
                break;
            case LOGS_COUNT:
                result = new LogsCountFilterViewHolder();
                break;
            case LOG_ENTRY:
                result = new LogEntryFilterViewHolder();
                break;
            case LOCATION:
                result = new StringFilterViewHolder<>(DataStore::getSuggestionsLocation);
                break;
            case STORED_LISTS:
                result = createStoredListFilterViewHolder();
                break;
            case ORIGIN:
                result = new CheckboxFilterViewHolder<>(
                        ValueGroupFilterAccessor.<IConnector, OriginGeocacheFilter>createForValueGroupFilter()
                                .setSelectableValues(ConnectorFactory.getConnectors())
                                .setValueDisplayTextGetter(IConnector::getName)
                                .setValueDrawableGetter(ct -> ImageParam.id(R.drawable.ic_menu_upload)), 1,
                        new HashSet<>(ConnectorFactory.getActiveConnectors()));
                break;
            case STORED_SINCE:
                result = createStoredSinceFilterViewHolder();
                break;
            case LOGICAL_FILTER_GROUP:
                result = new LogicalFilterViewHolder();
                break;
            default:
                result = null;
                break;
        }

        if (result == null) {
            return null;
        }

        result.init(type, activity);
        if (filter != null) {
            fillViewFrom(result, filter);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IGeocacheFilter> void fillViewFrom(final IFilterViewHolder<T> viewHolder, final IGeocacheFilter filter) {
        if (viewHolder != null && filter != null) {
            viewHolder.setViewFromFilter((T) filter);
        }
    }

    public static <T extends IGeocacheFilter> IGeocacheFilter createFrom(final IFilterViewHolder<T> holder) {
        return holder.createFilterFromView();
    }

    public static boolean isListInfoFilled() {
        return listInfoFilled;
    }

    public static boolean isListInfoComplete() {
        return listInfoIsComplete;
    }

    public static Collection<Geocache> getListInfoFilteredList() {
        return listInfoFilteredList;
    }

    public static void clearListInfo() {
        listInfoFilled = false;
        listInfoFilteredList = null;
        listInfoIsComplete = false;
    }

    public static void setListInfo(final Collection<Geocache> filteredList, final boolean isComplete) {
        listInfoFilled = filteredList != null && !filteredList.isEmpty();
        listInfoFilteredList = filteredList == null ? Collections.emptyList() : filteredList;
        listInfoIsComplete = isComplete;
    }

    private static IFilterViewHolder<?> create1to5ItemRangeSelectorViewHolder() {
        final Float[] range = new Float[]{1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f};
        return new ItemRangeSelectorViewHolder<>(
                new ValueGroupFilterAccessor<Float, NumberRangeGeocacheFilter<Float>>()
                        .setSelectableValues(range)
                        .setFilterValueGetter(f -> f.getValuesInRange(range))
                        .setFilterValueSetter((f, v) -> f.setRangeFromValues(v, 1f, 5f))
                        .setValueDisplayTextGetter(f -> String.format(Locale.getDefault(), "%.1f", f)),
                (i, f) -> i % 2 == 0 ? String.format(Locale.getDefault(), "%.1f", f) : null);
    }

    private static IFilterViewHolder<?> createStoredListFilterViewHolder() {

        final List<StoredList> allLists = DataStore.getLists();
        final Map<Integer, StoredList> allListsById = new HashMap<>();
        for (StoredList list : allLists) {
            allListsById.put(list.id, list);
        }

        final ValueGroupFilterAccessor<StoredList, StoredListGeocacheFilter> vgfa =
                new ValueGroupFilterAccessor<StoredList, StoredListGeocacheFilter>()
                        .setSelectableValues(allLists)
                        .setFilterValueGetter(StoredListGeocacheFilter::getFilterLists)
                        .setFilterValueSetter(StoredListGeocacheFilter::setFilterLists)
                        .setValueDrawableGetter(f -> f.markerId > 0 ? ImageParam.emoji(f.markerId) : ImageParam.id(R.drawable.ic_menu_manage_list))
                        .setValueDisplayTextGetter(f -> f.title)
                        .setGeocacheValueGetter((f, c) -> CollectionStream.of(c.getLists()).map(allListsById::get).toSet());

        return new CheckboxFilterViewHolder<>(vgfa, 1, Collections.emptySet());
    }

    private static IFilterViewHolder<?> createStoredSinceFilterViewHolder() {
        return new ItemRangeSelectorViewHolder<>(
                new ValueGroupFilterAccessor<Long, StoredSinceGeocacheFilter>()
                        .setSelectableValues(StoredSinceGeocacheFilter.getValueRange())
                        .setFilterValueGetter(f -> f.getValuesInRange(StoredSinceGeocacheFilter.getValueRange()))
                        .setFilterValueSetter(NumberRangeGeocacheFilter::setRangeFromValues)
                        .setValueDisplayTextGetter(s -> StoredSinceGeocacheFilter.toUserDisplayValue(s, false)),
                (i, s) -> StoredSinceGeocacheFilter.toUserDisplayValue(s, true));
    }

}
