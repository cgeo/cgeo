package cgeo.geocaching.filters.gui;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.models.Geocache;

import android.app.Activity;

import java.util.Collections;
import java.util.List;

public class FilterViewHolderUtils {

    private static boolean listInfoFilled = false;
    private static List<Geocache> listInfoFilteredList = Collections.emptyList();
    private static boolean listInfoIsComplete = false;

    private FilterViewHolderUtils() {
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
                result = new OneOfManyFilterViewHolder<>(new CacheType[]{
                    CacheType.TRADITIONAL, CacheType.MYSTERY, CacheType.MULTI, CacheType.EARTH, CacheType.EVENT, CacheType.WHERIGO, CacheType.VIRTUAL },
                    ct -> ct.markerId);
                break;
            case SIZE:
                result = new OneOfManyFilterViewHolder<>(CacheSize.values());
                break;
            case OFFLINE_LOG:
                result = new StringFilterViewHolder<>();
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

    public static List<Geocache> getListInfoFilteredList() {
        return listInfoFilteredList;
    }

    public static void clearListInfo() {
        listInfoFilled = false;
        listInfoFilteredList = null;
        listInfoIsComplete = false;
    }

    public static void setListInfo(final List<Geocache> filteredList, final boolean isComplete) {
        listInfoFilled = filteredList != null && !filteredList.isEmpty();
        listInfoFilteredList = filteredList == null ? Collections.emptyList() : filteredList;
        listInfoIsComplete = isComplete;
    }

}
