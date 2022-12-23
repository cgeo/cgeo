package cgeo.geocaching.sorting;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.os.Bundle;
import android.util.Pair;

import androidx.annotation.StringRes;
import androidx.core.util.Supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;

public class GeocacheSortContext {

    private static final String STATE_TYPE = "s_type";
    private static final String STATE_INVERSE = "s_inverse";
    private static final String STATE_TARGET_COORD = "s_targetcoord";
    private static final String STATE_EVENTLIST = "s_eventlist";
    private static final String STATE_SERIESLIST = "s_serieslist";
    private static final String STATE_LISTCONTEXT = "s_listcontext";
    private static final String STATE_LISTCONTEXTPARAM = "s_listcontextparameter";

    private SortType currentType = SortType.AUTO;
    private boolean currentIsInverse = false;

    //context values which influence the search context behavior (e.g. which types are available and which is current autosort type)
    private Geopoint targetCoords;
    private boolean isEventList;
    private boolean isSeriesList;

    //list context. If set, it is used to load/persist sort context for it
    private CacheListType listContextType = null;
    private String listContextTypeParam = null;

    public enum SortType {
        AUTO(R.string.caches_sort_automatic, false, null),
        TARGET_DISTANCE(R.string.caches_sort_distance_target, false, null),
        DISTANCE(R.string.caches_sort_distance, false, () -> GlobalGPSDistanceComparator.INSTANCE),
        EVENT_DATE(R.string.caches_sort_eventdate, EventDateComparator.class),
        HIDDEN_DATE(R.string.caches_sort_date_hidden, DateComparator.class),
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
        OWN_VOTE(R.string.caches_sort_vote, VoteComparator.class, true);

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

    /**
     * Returns current sort type
     */
    public SortType getType() {
        return currentType;
    }

    /**
     * Returns true if current selected type is inversed (in reference to the type's default 'inverse' flag)
     */
    public boolean isInverse() {
        return currentIsInverse;
    }

    /**
     * Returns current Cache Comparator (inverse or not is already included in it)
     */
    public CacheComparator getComparator() {
        return getComparatorFor(currentType, currentIsInverse);
    }

    /**
     * Sets a new sorttype. If already selected type is set, then the search order is inversed
     */
    public void setAndToggle(final SortType type) {
        if (currentType.equals(type)) {
            currentIsInverse = !currentIsInverse;
        } else {
            currentType = type;
            currentIsInverse = false;
        }
        persistSortConfig();
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
            result.add(new Pair<>(t, getNameFor(t, t.equals(currentType) && currentIsInverse)));
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

    public boolean isEventList() {
        return isEventList;
    }

    public void setEventList(final boolean eventList) {
        isEventList = eventList;
    }

    public boolean isSeriesList() {
        return isSeriesList;
    }

    public void setSeriesList(final boolean seriesList) {
        isSeriesList = seriesList;
    }

    /**
     * Saves to bundle (for state handling in activity lifecycle
     */
    public Bundle saveToBundle() {
        final Bundle b = new Bundle();
        b.putInt(STATE_TYPE, currentType.ordinal());
        b.putBoolean(STATE_INVERSE, currentIsInverse);
        b.putParcelable(STATE_TARGET_COORD, targetCoords);
        b.putBoolean(STATE_EVENTLIST, isEventList);
        b.putBoolean(STATE_SERIESLIST, isSeriesList);
        b.putInt(STATE_LISTCONTEXT, listContextType == null ? -1 : listContextType.ordinal());
        b.putString(STATE_LISTCONTEXTPARAM, listContextTypeParam);
        return b;
    }

    /**
     * Loads from bundle (for state handling in activity lifecycle
     */
    public void loadFromBundle(final Bundle b) {
        currentType = SortType.values()[b.getInt(STATE_TYPE, SortType.AUTO.ordinal())];
        currentIsInverse = b.getBoolean(STATE_INVERSE);

        targetCoords = b.containsKey(STATE_TARGET_COORD) ? b.getParcelable(STATE_TARGET_COORD) : null;
        isEventList = b.getBoolean(STATE_EVENTLIST);
        isSeriesList = b.getBoolean(STATE_SERIESLIST);

        listContextType = b.getInt(STATE_LISTCONTEXT, -1) < 0 ? null : CacheListType.values()[b.getInt(STATE_LISTCONTEXT)];
        listContextTypeParam = b.getString(STATE_LISTCONTEXTPARAM);
    }

    /**
     * Sets sort context depending on a list context. This list context is used to load/persist current sort state in it
     */
    public void setListContext(final CacheListType listType, final String listContextTypeParam) {
        this.listContextType = listType;
        this.listContextTypeParam = listContextTypeParam;

        if (listType == null) {
            return;
        }

        //try to load sort context from persistence
        final String sortConfig = Settings.getSortConfig(createListContextKey(listType, listContextTypeParam));
        if (sortConfig == null) {
            this.currentIsInverse = false;
            this.currentType = SortType.AUTO;
        } else {
            final String[] tokens = sortConfig.split("-");
            this.currentType = tokens.length >= 1 ? EnumUtils.getEnum(SortType.class, tokens[0], SortType.AUTO) : SortType.AUTO;
            this.currentIsInverse = tokens.length >= 2 && BooleanUtils.toBoolean(tokens[1]);
        }
    }

    private void persistSortConfig() {
        if (listContextType == null) {
            return;
        }

        Settings.setSortConfig(createListContextKey(this.listContextType, this.listContextTypeParam),
                this.currentType.name() + "-" + this.currentIsInverse);
    }


    private static String createListContextKey(final CacheListType listType, final String listContextTypeParam) {
        final StringBuilder sb = new StringBuilder(listType == null ? "null" : listType.name());
        if (listContextTypeParam != null) {
            sb.append("-").append(listContextTypeParam);
        }
        return sb.toString();
    }

    public String getSortName() {
        return getNameFor(this.currentType, this.currentIsInverse);
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

    private SortType getAutoType() {
        if (CacheListType.HISTORY.equals(listContextType)) {
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


}
