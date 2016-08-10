package cgeo.geocaching.ui;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DistanceComparator;
import cgeo.geocaching.sorting.EventDateComparator;
import cgeo.geocaching.sorting.InverseComparator;
import cgeo.geocaching.sorting.SeriesNameComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class CacheListAdapter extends ArrayAdapter<Geocache> {

    private LayoutInflater inflater = null;
    private static CacheComparator cacheComparator = null;
    private Geopoint coords;
    private float azimuth = 0;
    private long lastSort = 0L;
    private boolean selectMode = false;
    private IFilter currentFilter = null;
    private List<Geocache> originalList = null;
    private final boolean isLiveList = Settings.isLiveList();

    private final Set<CompassMiniView> compasses = new LinkedHashSet<>();
    private final Set<DistanceView> distances = new LinkedHashSet<>();
    private final CacheListType cacheListType;
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

    private static final int[] RATING_BACKGROUND = new int[3];
    /**
     * automatically order cache series by name, if they all have a common suffix or prefix at least these many
     * characters
     */
    private static final int MIN_COMMON_CHARACTERS_SERIES = 4;
    static {
        if (Settings.isLightSkin()) {
            RATING_BACKGROUND[0] = R.drawable.favorite_background_red_light;
            RATING_BACKGROUND[1] = R.drawable.favorite_background_orange_light;
            RATING_BACKGROUND[2] = R.drawable.favorite_background_green_light;
        } else {
            RATING_BACKGROUND[0] = R.drawable.favorite_background_red_dark;
            RATING_BACKGROUND[1] = R.drawable.favorite_background_orange_dark;
            RATING_BACKGROUND[2] = R.drawable.favorite_background_green_dark;
        }
    }

    /**
     * view holder for the cache list adapter
     *
     */
    public static class ViewHolder extends AbstractViewHolder {
        @BindView(R.id.checkbox) protected CheckBox checkbox;
        @BindView(R.id.log_status_mark) protected ImageView logStatusMark;
        @BindView(R.id.text) protected TextView text;
        @BindView(R.id.distance) protected DistanceView distance;
        @BindView(R.id.favorite) protected TextView favorite;
        @BindView(R.id.info) protected TextView info;
        @BindView(R.id.inventory) protected TextView inventory;
        @BindView(R.id.direction) protected CompassMiniView direction;
        @BindView(R.id.dirimg) protected ImageView dirImg;
        private CacheListType cacheListType;
        public Geocache cache = null;

        public ViewHolder(final View view) {
            super(view);
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
            return SeriesNameComparator.INSTANCE;
        }
        if (cacheComparator == null) {
            return DistanceComparator.INSTANCE;
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
        if (currentFilter != null) {
            // Back up the list again
            originalList = new ArrayList<>(list);

            currentFilter.filter(list);
        }
    }

    /**
     * Called after a user action on the filter menu.
     */
    public void setFilter(final IFilter filter) {
        // Backup current caches list if it isn't backed up yet
        if (originalList == null) {
            originalList = new ArrayList<>(list);
        }

        // If there is already a filter in place, this is a request to change or clear the filter, so we have to
        // replace the original cache list
        if (currentFilter != null) {
            list.clear();
            list.addAll(originalList);
        }

        // Do the filtering or clear it
        if (filter != null) {
            filter.filter(list);
        }
        currentFilter = filter;

        notifyDataSetChanged();
    }

    public boolean isFiltered() {
        return currentFilter != null;
    }

    public String getFilterName() {
        return currentFilter.getName();
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
        if (CollectionUtils.isEmpty(list) || selectMode) {
            return;
        }

        if (isSortedByDistance()) {
            lastSort = 0;
            updateSortByDistance();
        } else {
            Collections.sort(list, getPotentialInversion(getCacheComparator()));
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
        if (selectMode) {
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
        Collections.sort(list, getPotentialInversion(new DistanceComparator(coords, list)));

        // avoid an update if the list has not changed due to location update
        if (list.equals(oldList)) {
            return;
        }
        notifyDataSetChanged();
        lastSort = System.currentTimeMillis();
    }

    private Comparator<? super Geocache> getPotentialInversion(final CacheComparator comparator) {
        if (inverseSort) {
            return new InverseComparator(comparator);
        }
        return comparator;
    }

    private boolean isSortedByDistance() {
        final CacheComparator comparator = getCacheComparator();
        return comparator == null || comparator instanceof DistanceComparator;
    }

    private boolean isSortedByEvent() {
        final CacheComparator comparator = getCacheComparator();
        return comparator == null || comparator instanceof EventDateComparator;
    }

    private boolean isSortedBySeries() {
        final CacheComparator comparator = getCacheComparator();
        return comparator == null || comparator instanceof SeriesNameComparator;
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
        if (cache.isFound() && cache.isLogOffline()) {
            holder.logStatusMark.setImageResource(R.drawable.mark_green_orange);
            holder.logStatusMark.setVisibility(View.VISIBLE);
        } else if (cache.isFound()) {
            holder.logStatusMark.setImageResource(R.drawable.mark_green_more);
            holder.logStatusMark.setVisibility(View.VISIBLE);
        } else if (cache.isLogOffline()) {
            holder.logStatusMark.setImageResource(R.drawable.mark_orange);
            holder.logStatusMark.setVisibility(View.VISIBLE);
        } else {
            holder.logStatusMark.setVisibility(View.GONE);
        }
        holder.text.setCompoundDrawablesWithIntrinsicBounds(MapUtils.getCacheMarker(res, cache, holder.cacheListType).getDrawable(), null, null, null);
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
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

        final boolean lightSkin = Settings.isLightSkin();

        final TouchListener touchListener = new TouchListener(cache, this);
        v.setOnClickListener(touchListener);
        v.setOnLongClickListener(touchListener);
        v.setOnTouchListener(touchListener);

        holder.checkbox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        holder.checkbox.setChecked(cache.isStatusChecked());
        holder.checkbox.setOnClickListener(new SelectionCheckBoxListener(cache));

        distances.add(holder.distance);
        holder.distance.setContent(cache.getCoords());
        compasses.add(holder.direction);
        holder.direction.setTargetCoords(cache.getCoords());

        if (cache.isDisabled() || cache.isArchived() || CalendarUtils.isPastEvent(cache)) { // strike
            holder.text.setPaintFlags(holder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.text.setPaintFlags(holder.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        if (cache.isArchived()) { // red color
            holder.text.setTextColor(ContextCompat.getColor(getContext(), R.color.archived_cache_color));
        } else {
            holder.text.setTextColor(ContextCompat.getColor(getContext(), lightSkin ? R.color.text_light : R.color.text_dark));
        }

        holder.text.setText(cache.getName(), TextView.BufferType.NORMAL);
        holder.cacheListType = cacheListType;
        updateViewHolder(holder, cache, res);

        final int inventorySize = cache.getInventoryItems();
        if (inventorySize > 0) {
            holder.inventory.setText(Integer.toString(inventorySize));
            holder.inventory.setVisibility(View.VISIBLE);
        } else {
            holder.inventory.setVisibility(View.GONE);
        }

        if (cache.getDistance() != null) {
            holder.distance.setDistance(cache.getDistance());
        }

        if (cache.getCoords() != null && coords != null) {
            holder.distance.update(coords);
        }

        // only show the direction if this is enabled in the settings
        if (isLiveList) {
            if (cache.getCoords() != null) {
                holder.direction.setVisibility(View.VISIBLE);
                holder.dirImg.setVisibility(View.GONE);
                holder.direction.updateAzimuth(azimuth);
                if (coords != null) {
                    holder.direction.updateCurrentCoords(coords);
                }
            } else if (cache.getDirection() != null) {
                holder.direction.setVisibility(View.VISIBLE);
                holder.dirImg.setVisibility(View.GONE);
                holder.direction.updateAzimuth(azimuth);
                holder.direction.updateHeading(cache.getDirection());
            } else if (StringUtils.isNotBlank(cache.getDirectionImg())) {
                holder.dirImg.setVisibility(View.INVISIBLE);
                holder.direction.setVisibility(View.GONE);
                DirectionImage.fetchDrawable(cache.getDirectionImg()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<BitmapDrawable>() {
                    @Override
                    public void accept(final BitmapDrawable bitmapDrawable) {
                        if (cache == holder.cache) {
                            holder.dirImg.setImageDrawable(bitmapDrawable);
                            holder.dirImg.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                holder.dirImg.setVisibility(View.GONE);
                holder.direction.setVisibility(View.GONE);
            }
        }

        final int favCount = cache.getFavoritePoints();
        holder.favorite.setText(favCount >= 0 ? Integer.toString(favCount) : "?");

        int favoriteBack;
        // set default background, neither vote nor rating may be available
        if (lightSkin) {
            favoriteBack = R.drawable.favorite_background_light;
        } else {
            favoriteBack = R.drawable.favorite_background_dark;
        }
        final float rating = cache.getRating();
        if (rating >= 3.5) {
            favoriteBack = RATING_BACKGROUND[2];
        } else if (rating >= 2.1) {
            favoriteBack = RATING_BACKGROUND[1];
        } else if (rating > 0.0) {
            favoriteBack = RATING_BACKGROUND[0];
        }
        holder.favorite.setBackgroundResource(favoriteBack);

        if (isHistory() && cache.getVisitedDate() > 0) {
            holder.info.setText(Formatter.formatCacheInfoHistory(cache));
        } else {
            holder.info.setText(Formatter.formatCacheInfoLong(cache));
        }

        return v;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        distances.clear();
        compasses.clear();
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

                // left to right swipe
                if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (!adapter.selectMode) {
                        adapter.switchSelectMode();
                        cache.setStatusChecked(true);
                    }
                    return true;
                }

                // right to left swipe
                if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (adapter.selectMode) {
                        adapter.switchSelectMode();
                    }
                    return true;
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
        if (!eventsOnly && isSortedByEvent()) {
            setComparator(DistanceComparator.INSTANCE);
        }
        if (!series && isSortedBySeries()) {
            setComparator(DistanceComparator.INSTANCE);
        }
    }

    private void checkEvents() {
        eventsOnly = true;
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
        if (series) {
            setComparator(new SeriesNameComparator());
        }
    }

    public boolean isEventsOnly() {
        return eventsOnly;
    }
}
