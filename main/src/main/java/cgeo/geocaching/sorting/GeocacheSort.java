package cgeo.geocaching.sorting;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.util.Supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Represents sort settings applyable to a group/list of geocaches */
public class GeocacheSort implements Parcelable {

    private SortType type = SortType.AUTO;
    private boolean isInverse = false;

    //context values which influence the search context behavior (e.g. which types are available and which is current autosort type)
    private Geopoint targetCoords;
    private boolean isEventList;
    private boolean isSeriesList;
    private CacheListType listType = null;

    public enum SortType {
        AUTO(R.string.caches_sort_automatic, false, null),
        TARGET_DISTANCE(R.string.caches_sort_distance_target, false, null),
        DISTANCE(R.string.caches_sort_distance, false, () -> GlobalGPSDistanceComparator.INSTANCE),
        EVENT_DATE(R.string.caches_sort_eventdate, false, () -> EventDateComparator.INSTANCE),
        HIDDEN_DATE(R.string.caches_sort_date_hidden, false, () -> HiddenDateComparator.INSTANCE),
        DIFFICULTY(R.string.caches_sort_difficulty, DifficultyComparator.class),
        FINDS(R.string.caches_sort_finds, FindsComparator.class, true),
        GEOCODE(R.string.caches_sort_geocode, GeocodeComparator.class),
        INVENTORY(R.string.caches_sort_inventory, InventoryComparator.class, true),
        NAME(R.string.caches_sort_name, NameComparator.class),
        FAVORITES(R.string.caches_sort_favorites, PopularityComparator.class, true),
        FAVORITES_RATIO(R.string.caches_sort_favorites_ratio, PopularityRatioComparator.class, true),
        RATING(R.string.caches_sort_rating, RatingComparator.class, true),
        SIZE(R.string.caches_sort_size, SizeComparator.class),
        STATUS(R.string.caches_sort_state, StateComparator.class),
        STORAGE_DATE(R.string.caches_sort_storage, StorageTimeComparator.class, true),
        TERRAIN(R.string.caches_sort_terrain, TerrainComparator.class),
        LOGGED_DATE(R.string.caches_sort_date_logged, VisitComparator.class, true),
        OWN_VOTE(R.string.caches_sort_vote, VoteComparator.class, true),
        LAST_FOUND(R.string.caches_sort_lastfounddate, true, () -> LastFoundComparator.INSTANCE_INVERSE);

        @StringRes
        private final int resId;
        private final boolean sortDefaultLargeToSmall;
        private final Supplier<CacheComparator> cacheComparatorSupplier;

        SortType(@StringRes final int resId, final Class<? extends CacheComparator> comparatorClass) {
            this(resId, comparatorClass, false);
        }

        SortType(@StringRes final int resId, final Class<? extends CacheComparator> comparatorClass, final boolean sortDefaultLargeToSmall) {
            this(resId, sortDefaultLargeToSmall, () -> {
                try {
                    return comparatorClass.newInstance();
                } catch (Exception e) {
                    Log.e("Problem creating Cache Comparator for class '" + comparatorClass + "'", e);
                    return null;
                }
            });
        }

        SortType(@StringRes final int resId, final boolean sortDefaultLargeToSmall, final Supplier<CacheComparator> cacheComparatorSupplier) {
            this.resId = resId;
            this.sortDefaultLargeToSmall = sortDefaultLargeToSmall;
            this.cacheComparatorSupplier = cacheComparatorSupplier;
        }
    }

    public GeocacheSort() {
        //empty on purpose
    }

    public void setType(final SortType type, final boolean isInverse) {
        this.type = type == null ? SortType.AUTO : type;
        this.isInverse = isInverse;
    }

    public void setType(final SortType type) {
        setType(type, false);
    }

    /** Returns current sort type */
    public SortType getType() {
        return type;
    }

    /** Returns effectie sort type to use for sorting, considering the context parameters */
    public SortType getEffectiveType() {
        return getEffectiveTypeFor(this.type);
    }

    /** Returns whether sort shoudl eb effectively ascending, considering sort type default and inverse flag */
    public boolean isEffectiveAscending() {
        return (!getEffectiveType().sortDefaultLargeToSmall) ^ (isInverse);
    }

    /** Returns true if current selected type is inversed (in reference to the type's default 'inverse' flag) */
    public boolean isInverse() {
        return isInverse;
    }

    /**
     * Returns current Cache Comparator (inverse or not is already included in it)
     */
    public CacheComparator getComparator() {
        return getComparatorFor(type, isInverse);
    }

    /**
     * Sets a new sorttype. If already selected type is set, then the search order is inversed
     */
    public void setAndToggle(final SortType type) {
        if (this.type.equals(type)) {
            isInverse = !isInverse;
        } else {
            this.type = type;
            isInverse = false;
        }
    }

    /**
     * Returns the currently available sort types (together with their user-displayable name) for user selection
     */
    public List<Pair<SortType, String>> getAvailableTypes() {
        final Set<SortType> types = new HashSet<>(Arrays.asList(SortType.values()));
        if (isEventList) {
            types.remove(SortType.HIDDEN_DATE);
        } else {
            types.remove(SortType.EVENT_DATE);
        }
        if (targetCoords == null) {
            types.remove(SortType.TARGET_DISTANCE);
        }

        final List<Pair<SortType, String>> result = new ArrayList<>();
        for (SortType t : types) {
            result.add(new Pair<>(t, getNameFor(t, t.equals(type) && isInverse)));
        }
        //sort by display name, but put AUTO always first
        TextUtils.sortListLocaleAware(result, p -> p.first.equals(SortType.AUTO) ? "_" : p.second);
        return result;
    }

    //Setter/Getters for context values

    public Geopoint getTargetCoords() {
        return targetCoords;
    }

    public void setTargetCoords(final Geopoint targetCoords) {
        this.targetCoords = targetCoords;
    }

    public void setEventList(final boolean eventList) {
        isEventList = eventList;
    }

    public void setSeriesList(final boolean seriesList) {
        isSeriesList = seriesList;
    }


    /** Sets sort context depending on a list context. */
    public void setListType(final CacheListType listType) {
        this.listType = listType;
    }

    public CacheListType getListType() {
        return this.listType;
    }


    public String getDisplayName() {
        return getNameFor(this.type, this.isInverse);
    }

    private String getNameFor(final SortType type, final boolean inverted) {
        if (type == SortType.AUTO) {
            return LocalizationUtils.getString(SortType.AUTO.resId, getNameFor(getAutoType(), inverted));
        }
        return LocalizationUtils.getString(type.resId) + " " +
                (inverted ^ type.sortDefaultLargeToSmall ? "↑" : "↓");
    }

    private CacheComparator getComparatorFor(final SortType type, final boolean inverted) {
        final CacheComparator base;
        switch (type) {
            case AUTO:
                base = getComparatorFor(getAutoType(), false);
                break;
            case TARGET_DISTANCE:
                base = this.targetCoords != null ? new TargetDistanceComparator(this.targetCoords) : getComparatorFor(SortType.DISTANCE, false);
                break;
            default:
                base = type.cacheComparatorSupplier.get();
                break;
        }

        return inverted ? new InverseComparator(base) : base;
    }

    private SortType getEffectiveTypeFor(final SortType type) {
        switch (type) {
            case AUTO:
                return getAutoType();
            case TARGET_DISTANCE:
                return this.targetCoords != null ? SortType.TARGET_DISTANCE : SortType.DISTANCE;
            default:
                return type;
        }
    }

    private SortType getAutoType() {
        if (CacheListType.FINDER.equals(listType)) {
            return SortType.LAST_FOUND;
        }
        if (CacheListType.HISTORY.equals(listType)) {
            return SortType.LOGGED_DATE;
        }
        if (isEventList) {
            return SortType.EVENT_DATE;
        }
        if (isSeriesList) {
            return SortType.NAME;
        }
        if (targetCoords != null) {
            return SortType.TARGET_DISTANCE;
        }
        return SortType.DISTANCE;
    }

    // Parcelable

    protected GeocacheSort(final Parcel in) {
        final int typeInt = in.readInt();
        type = typeInt < 0 ? null : SortType.values()[typeInt];
        isInverse = in.readByte() != 0;
        targetCoords = in.readParcelable(Geopoint.class.getClassLoader());
        isEventList = in.readByte() != 0;
        isSeriesList = in.readByte() != 0;
        final int listContextInt = in.readInt();
        listType = listContextInt < 0 ? null : CacheListType.values()[listContextInt];
    }

    public static final Creator<GeocacheSort> CREATOR = new Creator<GeocacheSort>() {
        @Override
        public GeocacheSort createFromParcel(final Parcel in) {
            return new GeocacheSort(in);
        }

        @Override
        public GeocacheSort[] newArray(final int size) {
            return new GeocacheSort[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeInt(type == null ? -1 : type.ordinal());
        dest.writeByte((byte) (isInverse ? 1 : 0));
        dest.writeParcelable(targetCoords, flags);
        dest.writeByte((byte) (isEventList ? 1 : 0));
        dest.writeByte((byte) (isSeriesList ? 1 : 0));
        dest.writeInt(listType == null ? -1 : listType.ordinal());
    }



}
