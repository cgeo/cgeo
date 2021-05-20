package cgeo.geocaching.filters.gui;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter;
import cgeo.geocaching.filters.core.ValueGroupGeocacheFilter;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;

import java.util.Arrays;
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
                //gc.com groups in their search their cache types as follows:
                //* "Tradi" also includes: GCHQ, PROJECT_APE
                //* "Event" also includes:CITO,mega,giga, gps_exhibit,commun_celeb, gchq_celeb, block_party
                //-> unlike gc.com, currently CITO is an OWN search box below (to make number even)
                result = new CheckboxFilterViewHolder<>(
                    ValueGroupFilterAccessor.<CacheType, CacheType, ValueGroupGeocacheFilter<CacheType>>createForValueGroupFilter()
                        .setSelectableValues(Arrays.asList(CacheType.TRADITIONAL, CacheType.MULTI, CacheType.MYSTERY, CacheType.LETTERBOX, CacheType.EVENT,
                            CacheType.EARTH, CacheType.CITO, CacheType.WEBCAM, CacheType.VIRTUAL, CacheType.WHERIGO, CacheType.ADVLAB, CacheType.USER_DEFINED))
                        .addDisplayValues(CacheType.TRADITIONAL, CacheType.TRADITIONAL, CacheType.GCHQ, CacheType.PROJECT_APE)
                        .addDisplayValues(CacheType.EVENT, CacheType.EVENT, CacheType.MEGA_EVENT, CacheType.GIGA_EVENT, CacheType.COMMUN_CELEBRATION,
                            CacheType.GCHQ_CELEBRATION, CacheType.GPS_EXHIBIT, CacheType.BLOCK_PARTY)
                        .addDisplayValues(CacheType.VIRTUAL, CacheType.VIRTUAL, CacheType.LOCATIONLESS)
                        .addDisplayValues(CacheType.USER_DEFINED, CacheType.USER_DEFINED, CacheType.UNKNOWN)
                        .setValueDisplayTextGetter(CacheType::getShortL10n)
                        .setValueDrawableGetter(ct -> ct.markerId) , 2);
                break;
            case SIZE:
                result = new ToggleButtonFilterViewHolder<>(
                    ValueGroupFilterAccessor.<CacheSize, CacheSize, ValueGroupGeocacheFilter<CacheSize>>createForValueGroupFilter()
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
            case HIDDEN:
                result = new DateRangeFilterViewHolder<HiddenGeocacheFilter>();
                break;
            default:
                result = null;
                break;
        }
        if (result != null) {
            result.init(type, activity);
            if (filter != null) {
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
            new ValueGroupFilterAccessor<Float, Float, NumberRangeGeocacheFilter<Float>>()
                .setSelectableValues(range)
                .setFilterValueGetter(f -> f.getValuesInRange(range))
                .setFilterValueSetter(NumberRangeGeocacheFilter::setRangeFromValues)
                .setValueDisplayTextGetter(f -> String.format(Locale.getDefault(), "%.1f", f)),
            (i, f) -> i % 2 == 0 ? String.format(Locale.getDefault(), "%.1f", f) : null);
    }

}

