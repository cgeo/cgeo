package cgeo.geocaching.ui;

import butterknife.InjectView;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DistanceComparator;
import cgeo.geocaching.sorting.EventDateComparator;
import cgeo.geocaching.sorting.InverseComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.DateUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.jdt.annotation.NonNull;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.SparseArray;
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

    final private Set<CompassMiniView> compasses = new LinkedHashSet<>();
    final private Set<DistanceView> distances = new LinkedHashSet<>();
    final private CacheListType cacheListType;
    final private Resources res;
    /** Resulting list of caches */
    final private List<Geocache> list;
    private boolean eventsOnly;
    private boolean inverseSort = false;

    private static final int SWIPE_MIN_DISTANCE = 60;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    private static final SparseArray<Drawable> gcIconDrawables = new SparseArray<>();
    /**
     * time in milliseconds after which the list may be resorted due to position updates
     */
    private static final int PAUSE_BETWEEN_LIST_SORT = 1000;

    private static final int[] RATING_BACKGROUND = new int[3];
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
    protected static class ViewHolder extends AbstractViewHolder {
        @InjectView(R.id.checkbox) protected CheckBox checkbox;
        @InjectView(R.id.log_status_mark) protected ImageView logStatusMark;
        @InjectView(R.id.text) protected TextView text;
        @InjectView(R.id.distance) protected DistanceView distance;
        @InjectView(R.id.favorite) protected TextView favorite;
        @InjectView(R.id.info) protected TextView info;
        @InjectView(R.id.inventory) protected ImageView inventory;
        @InjectView(R.id.direction) protected CompassMiniView direction;
        @InjectView(R.id.dirimg) protected ImageView dirImg;

        public ViewHolder(final View view) {
            super(view);
        }
    }

    public CacheListAdapter(final Activity activity, final List<Geocache> list, final CacheListType cacheListType) {
        super(activity, 0, list);
        final GeoData currentGeo = CgeoApplication.getInstance().currentGeo();
        coords = currentGeo.getCoords();
        this.res = activity.getResources();
        this.list = list;
        this.cacheListType = cacheListType;
        checkEvents();

        final Drawable modifiedCoordinatesMarker = activity.getResources().getDrawable(R.drawable.marker_usermodifiedcoords);
        for (final CacheType cacheType : CacheType.values()) {
            // unmodified icon
            int hashCode = getIconHashCode(cacheType, false);
            gcIconDrawables.put(hashCode, activity.getResources().getDrawable(cacheType.markerId));
            // icon with flag for user modified coordinates
            hashCode = getIconHashCode(cacheType, true);
            final Drawable[] layers = new Drawable[2];
            layers[0] = activity.getResources().getDrawable(cacheType.markerId);
            layers[1] = modifiedCoordinatesMarker;
            final LayerDrawable ld = new LayerDrawable(layers);
            ld.setLayerInset(1,
                    layers[0].getIntrinsicWidth() - layers[1].getIntrinsicWidth(),
                    layers[0].getIntrinsicHeight() - layers[1].getIntrinsicHeight(),
                    0, 0);
            gcIconDrawables.put(hashCode, ld);
        }
    }

    private static int getIconHashCode(final CacheType cacheType, final boolean userModifiedOrFinal) {
        return new HashCodeBuilder()
                .append(cacheType)
                .append(userModifiedOrFinal)
                .toHashCode();
    }

    /**
     * change the sort order
     *
     * @param comparator
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

    public CacheComparator getCacheComparator() {
        if (isHistory()) {
            return VisitComparator.singleton;
        }
        if (cacheComparator == null && eventsOnly) {
            return EventDateComparator.singleton;
        }
        if (cacheComparator == null) {
            return DistanceComparator.singleton;
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
        }
        else {
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
        final ArrayList<Geocache> oldList = new ArrayList<>(list);
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

    public void setActualHeading(final float direction) {
        if (Math.abs(AngleUtils.difference(azimuth, direction)) < 5) {
            return;
        }

        azimuth = direction;
        for (final CompassMiniView compass : compasses) {
            compass.updateAzimuth(azimuth);
        }
    }

    @Override
    public View getView(final int position, final View rowView, final ViewGroup parent) {
        if (inflater == null) {
            inflater = ((Activity) getContext()).getLayoutInflater();
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

        Spannable spannable = null;
        if (cache.isDisabled() || cache.isArchived() || DateUtils.isPastEvent(cache)) { // strike
            spannable = Spannable.Factory.getInstance().newSpannable(cache.getName());
            spannable.setSpan(new StrikethroughSpan(), 0, spannable.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (cache.isArchived()) { // red color
            if (spannable == null) {
                spannable = Spannable.Factory.getInstance().newSpannable(cache.getName());
            }
            spannable.setSpan(new ForegroundColorSpan(res.getColor(R.color.archived_cache_color)), 0, spannable.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (spannable != null) {
            holder.text.setText(spannable, TextView.BufferType.SPANNABLE);
        }
        else {
            holder.text.setText(cache.getName());
        }
        holder.text.setCompoundDrawablesWithIntrinsicBounds(getCacheIcon(cache), null, null, null);

        if (cache.getInventoryItems() > 0) {
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
                holder.dirImg.setImageDrawable(DirectionImage.getDrawable(cache.getDirectionImg()));
                holder.dirImg.setVisibility(View.VISIBLE);
                holder.direction.setVisibility(View.GONE);
            } else {
                holder.dirImg.setVisibility(View.GONE);
                holder.direction.setVisibility(View.GONE);
            }
        }

        holder.favorite.setText(Integer.toString(cache.getFavoritePoints()));

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
            holder.info.setText(Formatter.formatCacheInfoLong(cache, cacheListType));
        }

        return v;
    }

    private static Drawable getCacheIcon(final Geocache cache) {
        int hashCode = getIconHashCode(cache.getType(), cache.hasUserModifiedCoords() || cache.hasFinalDefined());
        final Drawable drawable = gcIconDrawables.get(hashCode);
        if (drawable != null) {
            return drawable;
        }

        // fallback to mystery icon
        hashCode = getIconHashCode(CacheType.MYSTERY, cache.hasUserModifiedCoords() || cache.hasFinalDefined());
        return gcIconDrawables.get(hashCode);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        distances.clear();
        compasses.clear();
    }

    private static class SelectionCheckBoxListener implements View.OnClickListener {

        private final Geocache cache;

        public SelectionCheckBoxListener(final Geocache cache) {
            this.cache = cache;
        }

        @Override
        public void onClick(final View view) {
            assert view instanceof CheckBox;
            final boolean checkNow = ((CheckBox) view).isChecked();
            cache.setStatusChecked(checkNow);
        }
    }

    private static class TouchListener implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        private final Geocache cache;
        private final GestureDetector gestureDetector;
        private final @NonNull WeakReference<CacheListAdapter> adapterRef;

        public TouchListener(final Geocache cache, final @NonNull CacheListAdapter adapter) {
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
        private final @NonNull WeakReference<CacheListAdapter> adapterRef;

        public FlingGesture(final Geocache cache, final @NonNull CacheListAdapter adapter) {
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
        final ArrayList<Geocache> result = new ArrayList<>();
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

    public void checkEvents() {
        eventsOnly = true;
        for (final Geocache cache : list) {
            if (!cache.isEventCache()) {
                eventsOnly = false;
                return;
            }
        }
    }
}
