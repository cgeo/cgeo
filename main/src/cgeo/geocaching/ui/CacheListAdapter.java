package cgeo.geocaching.ui;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheslistItemBinding;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DistanceComparator;
import cgeo.geocaching.sorting.EventDateComparator;
import cgeo.geocaching.sorting.NameComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class CacheListAdapter extends ArrayAdapter<Geocache> implements SectionIndexer {

    private LayoutInflater inflater = null;
    private static CacheComparator cacheComparator = null;
    private Geopoint coords;
    private float azimuth = 0;
    private long lastSort = 0L;
    private boolean selectMode = false;
    private IFilter currentFilter = null;
    private GeocacheFilter currentGeocacheFilter = null;
    private List<Geocache> originalList = null;
    private final boolean isLiveList = Settings.isLiveList();

    private final Set<CompassMiniView> compasses = new LinkedHashSet<>();
    private final Set<DistanceView> distances = new LinkedHashSet<>();
    private final CacheListType cacheListType;
    private List<AbstractList> storedLists = null;
    private String currentListTitle = "";
    private final Resources res;
    /** Resulting list of caches */
    private final List<Geocache> list;
    private boolean eventsOnly;
    private boolean inverseSort = false;
    /**
     * {@code true} if the caches in this list are a complete series and should be sorted by name instead of distance
     */
    private boolean series = false;

    private static final int SWIPE_MIN_DISTANCE = 60;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    /**
     * time in milliseconds after which the list may be resorted due to position updates
     */
    private static final int PAUSE_BETWEEN_LIST_SORT = 1000;

    /**
     * automatically order cache series by name, if they all have a common suffix or prefix at least these many
     * characters
     */
    private static final int MIN_COMMON_CHARACTERS_SERIES = 4;

    // variables for section indexer
    private HashMap<String, Integer> mapFirstPosition;
    private HashMap<String, Integer> mapSection;
    private String[] sections;

    /**
     * view holder for the cache list adapter
     *
     */
    public static class ViewHolder extends AbstractViewHolder {
        private CacheListType cacheListType;
        public Geocache cache = null;
        private final CacheslistItemBinding binding;

        public ViewHolder(final View view) {
            super(view);
            binding = CacheslistItemBinding.bind(view);
        }
    }

    public CacheListAdapter(final Activity activity, final List<Geocache> list, final CacheListType cacheListType) {
        super(activity, 0, list);
        final GeoData currentGeo = Sensors.getInstance().currentGeo();
        coords = currentGeo.getCoords();
        this.res = activity.getResources();
        this.list = list;
        this.cacheListType = cacheListType;
        checkSpecialSortOrder();
        buildFastScrollIndex();

        DistanceComparator.updateGlobalGps(Sensors.getInstance().currentGeo().getCoords());
    }

    public void setStoredLists(final List<AbstractList> storedLists) {
        this.storedLists = storedLists;
    }

    public void setCurrentListTitle(final String currentListTitle) {
        this.currentListTitle = currentListTitle;
    }

    /**
     * change the sort order
     *
     */
    public void setComparator(final CacheComparator comparator) {
        cacheComparator = comparator;
        forceSort();
    }

    public void resetInverseSort() {
        inverseSort = false;
    }

    public void toggleInverseSort() {
        inverseSort = !inverseSort;
    }

    /**
     * Set the inverseSort order.
     *
     * @param inverseSort
     *          True if sort is inverted
     */
    public void setInverseSort(final boolean inverseSort) {
        this.inverseSort = inverseSort;
    }

    /**
     * Obtain the current inverseSort order.
     *
     * @return
     *          True if sort is inverted
     */
    public boolean getInverseSort() {
        return inverseSort;
    }

    public CacheComparator getCacheComparator() {
        if (isHistory()) {
            return VisitComparator.singleton;
        }
        if (cacheComparator == null && eventsOnly) {
            return EventDateComparator.INSTANCE;
        }
        if (cacheComparator == null && series) {
            return NameComparator.INSTANCE;
        }
        if (cacheComparator == null) {
            return DistanceComparator.DISTANCE_TO_GLOBAL_GPS;
        }
        return cacheComparator;
    }

    private boolean isHistory() {
        return cacheListType == CacheListType.HISTORY;
    }

    public Geocache findCacheByGeocode(final String geocode) {
        for (int i = 0; i < getCount(); i++) {
            if (getItem(i).getGeocode().equalsIgnoreCase(geocode)) {
                return getItem(i);
            }
        }

        return null;
    }
    /**
     * Called when a new page of caches was loaded.
     */
    public void reFilter() {
        if (hasActiveFilter()) {
            // Back up the list again
            originalList = new ArrayList<>(list);

            performFiltering();
        }
    }

    /**
     * Called after a user action on the filter menu.
     */
    public void setFilter(final IFilter filter, final GeocacheFilter advancedFilter) {

        GeocacheFilter gcFilter = null;
        if (advancedFilter != null) {
            gcFilter = advancedFilter;
         }

        // Backup current caches list if it isn't backed up yet
        if (originalList == null) {
            originalList = new ArrayList<>(list);
        }

        // If there is already a filter in place, this is a request to change or clear the filter, so we have to
        // replace the original cache list
        if (hasActiveFilter()) {
            list.clear();
            list.addAll(originalList);
        }

        currentFilter = filter;
        currentGeocacheFilter = gcFilter;

        performFiltering();

        notifyDataSetChanged();
    }

    private void performFiltering() {
        // Do the filtering or clear it
        if (currentFilter != null) {
            currentFilter.filter(list);
        }
        if (currentGeocacheFilter != null && currentGeocacheFilter.isFiltering()) {
            currentGeocacheFilter.filterList(list);
        }
    }

    public boolean hasActiveFilter() {
        final boolean newFilterFilters = currentGeocacheFilter != null && currentGeocacheFilter.isFiltering();
        return currentFilter != null || newFilterFilters;
    }

    public String getFilterName() {
        return (currentFilter == null ? "-" : currentFilter.getName()) + "|" +
            (currentGeocacheFilter == null || !currentGeocacheFilter.isFiltering() ? "-" : currentGeocacheFilter.toUserDisplayableString());
    }

    public int getCheckedCount() {
        int checked = 0;
        for (final Geocache cache : list) {
            if (cache.isStatusChecked()) {
                checked++;
            }
        }
        return checked;
    }

    public void setSelectMode(final boolean selectMode) {
        this.selectMode = selectMode;

        if (!selectMode) {
            for (final Geocache cache : list) {
                cache.setStatusChecked(false);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        return selectMode;
    }

    public void switchSelectMode() {
        setSelectMode(!isSelectMode());
    }

    public void invertSelection() {
        for (final Geocache cache : list) {
            cache.setStatusChecked(!cache.isStatusChecked());
        }
        notifyDataSetChanged();
    }

    public void forceSort() {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        if (isSortedByDistance()) {
            lastSort = 0;
            updateSortByDistance();
        } else {
            getCacheComparator().sort(list, inverseSort);
        }

        notifyDataSetChanged();
    }

    public void setActualCoordinates(@NonNull final Geopoint coords) {
        this.coords = coords;
        updateSortByDistance();

        for (final DistanceView distance : distances) {
            distance.update(coords);
        }
        for (final CompassMiniView compass : compasses) {
            compass.updateCurrentCoords(coords);
        }
    }

    private void updateSortByDistance() {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        if ((System.currentTimeMillis() - lastSort) <= PAUSE_BETWEEN_LIST_SORT) {
            return;
        }
        if (!isSortedByDistance()) {
            return;
        }
        if (coords == null) {
            return;
        }
        final List<Geocache> oldList = new ArrayList<>(list);
        DistanceComparator.updateGlobalGps(coords);
        DistanceComparator.DISTANCE_TO_GLOBAL_GPS.sort(list, inverseSort);

        // avoid an update if the list has not changed due to location update
        if (list.equals(oldList)) {
            return;
        }
        notifyDataSetChanged();
        lastSort = System.currentTimeMillis();
    }

    private boolean isSortedByDistance() {
        final CacheComparator comparator = getCacheComparator();
        return comparator == null || comparator instanceof DistanceComparator;
    }

    public void setActualHeading(final float direction) {
        if (Math.abs(AngleUtils.difference(azimuth, direction)) < 5) {
            return;
        }

        azimuth = direction;
        for (final CompassMiniView compass : compasses) {
            compass.updateAzimuth(azimuth);
        }
    }

    public static void updateViewHolder(final ViewHolder holder, final Geocache cache, final Resources res) {
        if (cache.isFound() && cache.hasLogOffline()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_green_orange);
        } else if (cache.isFound()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_green_more);
        } else if (cache.hasLogOffline()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_orange);
        } else if (cache.isDNF()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_red);
        } else {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_transparent);
        }
        holder.binding.textIcon.setImageDrawable(MapMarkerUtils.getCacheMarker(res, cache, holder.cacheListType).getDrawable());
    }

    @Override
    public View getView(final int position, final View rowView, @NonNull final ViewGroup parent) {
        if (inflater == null) {
            inflater = LayoutInflater.from(getContext());
        }

        if (position > getCount()) {
            Log.w("CacheListAdapter.getView: Attempt to access missing item #" + position);
            return null;
        }

        final Geocache cache = getItem(position);

        View v = rowView;

        final ViewHolder holder;
        if (v == null) {
            v = inflater.inflate(R.layout.cacheslist_item, parent, false);
            holder = new ViewHolder(v);
        } else {
            holder = (ViewHolder) v.getTag();
        }
        holder.cache = cache;

        final TouchListener touchListener = new TouchListener(cache, this);
        v.setOnClickListener(touchListener);
        v.setOnLongClickListener(touchListener);
        v.setOnTouchListener(touchListener);

        holder.binding.checkbox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        holder.binding.checkbox.setChecked(cache.isStatusChecked());
        holder.binding.checkbox.setOnClickListener(new SelectionCheckBoxListener(cache));

        distances.add(holder.binding.distance);
        holder.binding.distance.setContent(cache.getCoords());
        compasses.add(holder.binding.direction);
        holder.binding.direction.setTargetCoords(cache.getCoords());

        if (cache.isDisabled() || cache.isArchived() || CalendarUtils.isPastEvent(cache)) { // strike
            holder.binding.text.setPaintFlags(holder.binding.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.binding.text.setPaintFlags(holder.binding.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        if (cache.isArchived()) { // red color
            holder.binding.text.setTextColor(ContextCompat.getColor(getContext(), R.color.archived_cache_color));
        } else {
            holder.binding.text.setTextColor(ContextCompat.getColor(getContext(), R.color.colorText));
        }

        holder.binding.text.setText(cache.getName(), TextView.BufferType.NORMAL);
        holder.cacheListType = cacheListType;
        updateViewHolder(holder, cache, res);

        final int inventorySize = cache.getInventoryItems();
        if (inventorySize > 0) {
            holder.binding.inventory.setText(String.format(Locale.getDefault(), "%d", inventorySize));
            holder.binding.inventory.setVisibility(View.VISIBLE);
        } else {
            holder.binding.inventory.setVisibility(View.GONE);
        }

        if (cache.getDistance() != null) {
            holder.binding.distance.setDistance(cache.getDistance());
        }

        if (cache.getCoords() != null && coords != null) {
            holder.binding.distance.update(coords);
        }

        // only show the direction if this is enabled in the settings
        if (isLiveList) {
            if (cache.getCoords() != null) {
                holder.binding.direction.setVisibility(View.VISIBLE);
                holder.binding.dirimg.setVisibility(View.GONE);
                holder.binding.direction.updateAzimuth(azimuth);
                if (coords != null) {
                    holder.binding.direction.updateCurrentCoords(coords);
                }
            } else if (cache.getDirection() != null) {
                holder.binding.direction.setVisibility(View.VISIBLE);
                holder.binding.dirimg.setVisibility(View.GONE);
                holder.binding.direction.updateAzimuth(azimuth);
                holder.binding.direction.updateHeading(cache.getDirection());
            } else if (StringUtils.isNotBlank(cache.getDirectionImg())) {
                holder.binding.dirimg.setVisibility(View.INVISIBLE);
                holder.binding.direction.setVisibility(View.GONE);
                DirectionImage.fetchDrawable(cache.getDirectionImg()).observeOn(AndroidSchedulers.mainThread()).subscribe(bitmapDrawable -> {
                    if (cache == holder.cache) {
                        holder.binding.dirimg.setImageDrawable(bitmapDrawable);
                        holder.binding.dirimg.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                holder.binding.dirimg.setVisibility(View.GONE);
                holder.binding.direction.setVisibility(View.GONE);
            }
        }

        final int favCount = cache.getFavoritePoints();
        holder.binding.favorite.setText(Formatter.formatFavCount(favCount));
        final float rating = cache.getRating();
        holder.binding.favorite.setBackgroundResource(rating >= 3.5 ? R.drawable.favorite_background_green : rating >= 2.1 ? R.drawable.favorite_background_orange : rating > 0.0 ? R.drawable.favorite_background_red : R.drawable.favorite_background);

        if (isHistory() && cache.getVisitedDate() > 0) {
            holder.binding.info.setText(Formatter.formatCacheInfoHistory(cache));
        } else {
            holder.binding.info.setText(Formatter.formatCacheInfoLong(cache));
        }

        // optionally show list infos
        if (null != storedLists) {
            final List<String> infos = new ArrayList<>();
            final Set<Integer> lists = cache.getLists();
            for (final AbstractList temp : storedLists) {
                if (lists.contains(temp.id) && !temp.title.equals(currentListTitle)) {
                    infos.add(temp.title);
                }
            }
            if (!infos.isEmpty()) {
                holder.binding.info.append("\n" + StringUtils.join(infos, Formatter.SEPARATOR));
            }
        }

        return v;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        distances.clear();
        compasses.clear();
        buildFastScrollIndex();
    }

    private static class SelectionCheckBoxListener implements View.OnClickListener {

        private final Geocache cache;

        SelectionCheckBoxListener(final Geocache cache) {
            this.cache = cache;
        }

        @Override
        public void onClick(final View view) {
            final boolean checkNow = ((CheckBox) view).isChecked();
            cache.setStatusChecked(checkNow);
        }
    }

    private static class TouchListener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        private final Geocache cache;
        private final GestureDetector gestureDetector;
        @NonNull private final WeakReference<CacheListAdapter> adapterRef;

        TouchListener(final Geocache cache, @NonNull final CacheListAdapter adapter) {
            this.cache = cache;
            gestureDetector = new GestureDetector(adapter.getContext(), new FlingGesture(cache, adapter));
            adapterRef = new WeakReference<>(adapter);
        }

        // Tap on item
        @Override
        public void onClick(final View view) {
            final CacheListAdapter adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            if (adapter.isSelectMode()) {
                cache.setStatusChecked(!cache.isStatusChecked());
                adapter.notifyDataSetChanged();
            } else {
                CacheDetailActivity.startActivity(adapter.getContext(), cache.getGeocode(), cache.getName());
            }
        }

        // Long tap on item
        @Override
        public boolean onLongClick(final View view) {
            view.showContextMenu();
            return true;
        }

        // Swipe on item
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(final View view, final MotionEvent event) {
            return gestureDetector.onTouchEvent(event);

        }
    }

    private static class FlingGesture extends GestureDetector.SimpleOnGestureListener {

        private final Geocache cache;
        @NonNull private final WeakReference<CacheListAdapter> adapterRef;

        FlingGesture(final Geocache cache, @NonNull final CacheListAdapter adapter) {
            this.cache = cache;
            adapterRef = new WeakReference<>(adapter);
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }
                final CacheListAdapter adapter = adapterRef.get();
                if (adapter == null) {
                    return false;
                }

                // horizontal swipe
                if (Math.abs(velocityX) > Math.abs(velocityY)) {

                    // left to right swipe
                    if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE) {
                        if (!adapter.selectMode) {
                            adapter.switchSelectMode();
                            cache.setStatusChecked(true);
                        }
                        return true;
                    }

                    // right to left swipe
                    if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE) {
                        if (adapter.selectMode) {
                            adapter.switchSelectMode();
                        }
                        return true;
                    }
                }
            } catch (final Exception e) {
                Log.w("CacheListAdapter.FlingGesture.onFling", e);
            }

            return false;
        }
    }

    public List<Geocache> getFilteredList() {
        return list;
    }

    public List<Geocache> getCheckedCaches() {
        final List<Geocache> result = new ArrayList<>();
        for (final Geocache cache : list) {
            if (cache.isStatusChecked()) {
                result.add(cache);
            }
        }
        return result;
    }

    public List<Geocache> getCheckedOrAllCaches() {
        final List<Geocache> result = getCheckedCaches();
        if (!result.isEmpty()) {
            return result;
        }
        return new ArrayList<>(list);
    }

    public int getCheckedOrAllCount() {
        final int checked = getCheckedCount();
        if (checked > 0) {
            return checked;
        }
        return list.size();
    }

    public void checkSpecialSortOrder() {
        checkEvents();
        checkSeries();
    }

    private void checkEvents() {
        eventsOnly = list.isEmpty() ? false : true;
        for (final Geocache cache : list) {
            if (!cache.isEventCache()) {
                eventsOnly = false;
                return;
            }
        }
    }

    /**
     * detect whether all caches in this list belong to a series with similar names
     */
    private void checkSeries() {
        series = false;
        if (list.size() < 3 || list.size() > 50) {
            return;
        }
        final ArrayList<String> names = new ArrayList<>();
        final ArrayList<String> reverseNames = new ArrayList<>();
        for (final Geocache cache : list) {
            final String name = cache.getName();
            names.add(name);
            reverseNames.add(StringUtils.reverse(name));
        }
        final String commonPrefix = StringUtils.getCommonPrefix(names.toArray(new String[names.size()]));
        if (StringUtils.length(commonPrefix) >= MIN_COMMON_CHARACTERS_SERIES) {
            series = true;
        } else {
            final String commonSuffix = StringUtils.getCommonPrefix(reverseNames.toArray(new String[reverseNames.size()]));
            if (StringUtils.length(commonSuffix) >= MIN_COMMON_CHARACTERS_SERIES) {
                series = true;
            }
        }
    }

    public boolean isEventsOnly() {
        return eventsOnly;
    }

    // methods for section indexer

    private void buildFastScrollIndex() {
        mapFirstPosition = new LinkedHashMap<>();
        final ArrayList<String> sectionList = new ArrayList<>();
        String lastComparable = null;
        for (int x = 0; x < list.size(); x++) {
            final String comparable = getComparable(x);
            if (!StringUtils.equals(lastComparable, comparable)) {
                mapFirstPosition.put(comparable, x);
                sectionList.add(comparable);
                lastComparable = comparable;
            }
        }
        sections = new String[sectionList.size()];
        sectionList.toArray(sections);
        mapSection = new LinkedHashMap<>();
        for (int x = 0; x < sections.length; x++) {
            mapSection.put(sections[x], x);
        }
    }

    public int getPositionForSection(final int section) {
        if (sections == null || sections.length == 0) {
            return 0;
        }
        final Integer position = mapFirstPosition.get(sections[Math.max(0, Math.min(section, sections.length - 1))]);
        return null == position ? 0 : position;
    }

    public int getSectionForPosition(final int position) {
        final Integer section = mapSection.get(getComparable(position));
        return null == section ? 0 : section;
    }

    public Object[] getSections() {
        return sections;
    }

    @NonNull
    private String getComparable(final int position) {
        try {
            return getCacheComparator().getSortableSection(list.get(position));
        } catch (NullPointerException e) {
            return " ";
        }
    }

}
