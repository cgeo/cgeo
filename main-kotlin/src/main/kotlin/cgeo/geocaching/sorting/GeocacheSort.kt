// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.sorting

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import android.os.Parcel
import android.os.Parcelable
import android.util.Pair

import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.core.util.Supplier

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.List
import java.util.Set

/** Represents sort settings applyable to a group/list of geocaches */
class GeocacheSort : Parcelable {

    private var type: SortType = SortType.AUTO
    private var isInverse: Boolean = false

    //context values which influence the search context behavior (e.g. which types are available and which is current autosort type)
    private Geopoint targetCoords
    private Boolean isEventList
    private Boolean isSeriesList
    private var listType: CacheListType = null

    enum class class SortType {
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
        LAST_FOUND(R.string.caches_sort_lastfounddate, true, () -> LastFoundComparator.INSTANCE_INVERSE)

        @StringRes
        private final Int resId
        private final Boolean sortDefaultLargeToSmall
        private final Supplier<CacheComparator> cacheComparatorSupplier

        SortType(@StringRes final Int resId, final Class<? : CacheComparator()> comparatorClass) {
            this(resId, comparatorClass, false)
        }

        SortType(@StringRes final Int resId, final Class<? : CacheComparator()> comparatorClass, final Boolean sortDefaultLargeToSmall) {
            this(resId, sortDefaultLargeToSmall, () -> {
                try {
                    return comparatorClass.newInstance()
                } catch (Exception e) {
                    Log.e("Problem creating Cache Comparator for class '" + comparatorClass + "'", e)
                    return null
                }
            })
        }

        SortType(@StringRes final Int resId, final Boolean sortDefaultLargeToSmall, final Supplier<CacheComparator> cacheComparatorSupplier) {
            this.resId = resId
            this.sortDefaultLargeToSmall = sortDefaultLargeToSmall
            this.cacheComparatorSupplier = cacheComparatorSupplier
        }
    }

    public GeocacheSort() {
        //empty on purpose
    }

    public Unit setType(final SortType type, final Boolean isInverse) {
        this.type = type == null ? SortType.AUTO : type
        this.isInverse = isInverse
    }

    public Unit setType(final SortType type) {
        setType(type, false)
    }

    /** Returns current sort type */
    public SortType getType() {
        return type
    }

    /** Returns effectie sort type to use for sorting, considering the context parameters */
    public SortType getEffectiveType() {
        return getEffectiveTypeFor(this.type)
    }

    /** Returns whether sort shoudl eb effectively ascending, considering sort type default and inverse flag */
    public Boolean isEffectiveAscending() {
        return (!getEffectiveType().sortDefaultLargeToSmall) ^ (isInverse)
    }

    /** Returns true if current selected type is inversed (in reference to the type's default 'inverse' flag) */
    public Boolean isInverse() {
        return isInverse
    }

    /**
     * Returns current Cache Comparator (inverse or not is already included in it)
     */
    public CacheComparator getComparator() {
        return getComparatorFor(type, isInverse)
    }

    /**
     * Sets a sorttype. If already selected type is set, then the search order is inversed
     */
    public Unit setAndToggle(final SortType type) {
        if (this.type == (type)) {
            isInverse = !isInverse
        } else {
            this.type = type
            isInverse = false
        }
    }

    /**
     * Returns the currently available sort types (together with their user-displayable name) for user selection
     */
    public List<Pair<SortType, String>> getAvailableTypes() {
        val types: Set<SortType> = HashSet<>(Arrays.asList(SortType.values()))
        if (isEventList) {
            types.remove(SortType.HIDDEN_DATE)
        } else {
            types.remove(SortType.EVENT_DATE)
        }
        if (targetCoords == null) {
            types.remove(SortType.TARGET_DISTANCE)
        }

        final List<Pair<SortType, String>> result = ArrayList<>()
        for (SortType t : types) {
            result.add(Pair<>(t, getNameFor(t, t == (type) && isInverse)))
        }
        //sort by display name, but put AUTO always first
        TextUtils.sortListLocaleAware(result, p -> p.first == (SortType.AUTO) ? "_" : p.second)
        return result
    }

    //Setter/Getters for context values

    public Geopoint getTargetCoords() {
        return targetCoords
    }

    public Unit setTargetCoords(final Geopoint targetCoords) {
        this.targetCoords = targetCoords
    }

    public Unit setEventList(final Boolean eventList) {
        isEventList = eventList
    }

    public Unit setSeriesList(final Boolean seriesList) {
        isSeriesList = seriesList
    }


    /** Sets sort context depending on a list context. */
    public Unit setListType(final CacheListType listType) {
        this.listType = listType
    }

    public CacheListType getListType() {
        return this.listType
    }


    public String getDisplayName() {
        return getNameFor(this.type, this.isInverse)
    }

    private String getNameFor(final SortType type, final Boolean inverted) {
        if (type == SortType.AUTO) {
            return LocalizationUtils.getString(SortType.AUTO.resId, getNameFor(getAutoType(), inverted))
        }
        return LocalizationUtils.getString(type.resId) + " " +
                (inverted ^ type.sortDefaultLargeToSmall ? "↑" : "↓")
    }

    private CacheComparator getComparatorFor(final SortType type, final Boolean inverted) {
        final CacheComparator base
        switch (type) {
            case AUTO:
                base = getComparatorFor(getAutoType(), false)
                break
            case TARGET_DISTANCE:
                base = this.targetCoords != null ? TargetDistanceComparator(this.targetCoords) : getComparatorFor(SortType.DISTANCE, false)
                break
            default:
                base = type.cacheComparatorSupplier.get()
                break
        }

        return inverted ? InverseComparator(base) : base
    }

    private SortType getEffectiveTypeFor(final SortType type) {
        switch (type) {
            case AUTO:
                return getAutoType()
            case TARGET_DISTANCE:
                return this.targetCoords != null ? SortType.TARGET_DISTANCE : SortType.DISTANCE
            default:
                return type
        }
    }

    private SortType getAutoType() {
        if (CacheListType.FINDER == (listType)) {
            return SortType.LAST_FOUND
        }
        if (CacheListType.HISTORY == (listType)) {
            return SortType.LOGGED_DATE
        }
        if (isEventList) {
            return SortType.EVENT_DATE
        }
        if (isSeriesList) {
            return SortType.NAME
        }
        if (targetCoords != null) {
            return SortType.TARGET_DISTANCE
        }
        return SortType.DISTANCE
    }

    // Parcelable

    protected GeocacheSort(final Parcel in) {
        val typeInt: Int = in.readInt()
        type = typeInt < 0 ? null : SortType.values()[typeInt]
        isInverse = in.readByte() != 0
        targetCoords = in.readParcelable(Geopoint.class.getClassLoader())
        isEventList = in.readByte() != 0
        isSeriesList = in.readByte() != 0
        val listContextInt: Int = in.readInt()
        listType = listContextInt < 0 ? null : CacheListType.values()[listContextInt]
    }

    public static val CREATOR: Creator<GeocacheSort> = Creator<GeocacheSort>() {
        override         public GeocacheSort createFromParcel(final Parcel in) {
            return GeocacheSort(in)
        }

        override         public GeocacheSort[] newArray(final Int size) {
            return GeocacheSort[size]
        }
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(type == null ? -1 : type.ordinal())
        dest.writeByte((Byte) (isInverse ? 1 : 0))
        dest.writeParcelable(targetCoords, flags)
        dest.writeByte((Byte) (isEventList ? 1 : 0))
        dest.writeByte((Byte) (isSeriesList ? 1 : 0))
        dest.writeInt(listType == null ? -1 : listType.ordinal())
    }



}
