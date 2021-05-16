package cgeo.geocaching.filters.gui;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter;
import cgeo.geocaching.filters.core.ValueGroupGeocacheFilter;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

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
                result = new StringFilterViewHolder<>();
                break;
            case PERSONAL_NOTE:
                result = new StringFilterViewHolder<>();
                break;
            case TYPE:
                result = new CheckboxFilterViewHolder<>(
                    ValueGroupFilterAccessor.<CacheType, ValueGroupGeocacheFilter<CacheType>>createForValueGroupFilter()
                        .setSelectableValues(new CacheType[]{CacheType.TRADITIONAL, CacheType.MYSTERY, CacheType.MULTI, CacheType.EARTH, CacheType.EVENT, CacheType.WHERIGO, CacheType.VIRTUAL})
                        .setValueDisplayTextGetter(CacheType::getL10n)
                        .setValueDrawableGetter(ct -> ct.markerId));
                break;
            case SIZE:
                result = new ToggleButtonFilterViewHolder<>(
                    ValueGroupFilterAccessor.<CacheSize, ValueGroupGeocacheFilter<CacheSize>>createForValueGroupFilter()
                        //.setSelectableValues(new CacheSize[]{CacheSize.VIRTUAL, CacheSize.NANO, CacheSize.MICRO, CacheSize.SMALL, CacheSize.REGULAR, CacheSize.LARGE, CacheSize.VERY_LARGE, CacheSize.OTHER})
                        .setSelectableValues(CacheSize.values())
                        .setValueDisplayTextGetter(CacheSize::getL10n));
                break;
            case OFFLINE_LOG:
                result = new StringFilterViewHolder<>();
                break;
            case DIFFICULTY:
            case TERRAIN:
                result = createTerrainDifficultyFilterViewHolder();
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
            default:
                result = null;
                break;
        }
        if (result != null) {
            result.init(type, activity);
            if (filter != null) {
                result.getView(); //force view-create
                fillViewFrom(result, filter);
            }
        }

        return result;
    }

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

    private static IFilterViewHolder<?> createTerrainDifficultyFilterViewHolder() {
        final Float[] range = new Float[]{1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f};
        return new ItemRangeSelectorViewHolder<>(
            new ValueGroupFilterAccessor<Float, NumberRangeGeocacheFilter<Float>>()
                .setSelectableValues(range)
                .setValueGetter(f -> f.getValuesInRange(range))
                .setValueSetter(NumberRangeGeocacheFilter::setRangeFromValues)
                .setValueDisplayTextGetter(f -> String.format(Locale.getDefault(), "%.1f", f)),
            (i, f) -> i % 2 == 0 ? String.format(Locale.getDefault(), "%.1f", f) : null);
    }

}

