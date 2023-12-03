package cgeo.geocaching.sorting;

import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.settings.Settings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;

public class GeocacheSortContext implements Parcelable {


    private GeocacheSort sort = null;
    private String contextParam = null;

    private GeocacheSortContext() {
        //empty on purpose
    }

    @NonNull
    public GeocacheSort getSort() {
        return sort;
    }

    public static GeocacheSortContext getFor(final CacheListType listType, final String contextParam) {

        GeocacheSort.SortType type = GeocacheSort.SortType.AUTO;
        boolean isInverse = false;

        //try to load sort context from persistence
        final String sortConfig = Settings.getSortConfig(createListContextKey(listType, contextParam));
        if (sortConfig != null) {
            final String[] tokens = sortConfig.split("-");
            type = tokens.length >= 1 ? EnumUtils.getEnum(GeocacheSort.SortType.class, tokens[0], GeocacheSort.SortType.AUTO) : GeocacheSort.SortType.AUTO;
            isInverse = tokens.length >= 2 && BooleanUtils.toBoolean(tokens[1]);
        }

        final GeocacheSort sort = new GeocacheSort();
        sort.setType(type, isInverse);
        sort.setListType(listType);
        final GeocacheSortContext ctx = new GeocacheSortContext();
        ctx.sort = sort;
        ctx.contextParam = contextParam;
        return ctx;
    }

    public void save() {
        //Only sort context of OFFLINE lists are stored
        if (sort == null || CacheListType.OFFLINE != sort.getListType()) {
            return;
        }

        Settings.setSortConfig(createListContextKey(sort.getListType(), this.contextParam),
            sort.getType().name() + "-" + sort.isInverse());
    }


    private static String createListContextKey(final CacheListType listType, final String listContextTypeParam) {
        final StringBuilder sb = new StringBuilder(listType == null ? "null" : listType.name());
        if (listContextTypeParam != null) {
            sb.append("-").append(listContextTypeParam);
        }
        return sb.toString();
    }

    // Parcelable


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeParcelable(sort, flags);
        dest.writeString(contextParam);
    }


    protected GeocacheSortContext(final Parcel in) {
        sort = in.readParcelable(GeocacheSort.class.getClassLoader());
        contextParam = in.readString();
    }

    public static final Creator<GeocacheSortContext> CREATOR = new Creator<GeocacheSortContext>() {
        @Override
        public GeocacheSortContext createFromParcel(final Parcel in) {
            return new GeocacheSortContext(in);
        }

        @Override
        public GeocacheSortContext[] newArray(final int size) {
            return new GeocacheSortContext[size];
        }
    };

}
