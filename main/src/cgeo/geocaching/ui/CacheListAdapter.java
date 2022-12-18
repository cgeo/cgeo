package cgeo.geocaching.ui;

import cgeo.geocaching.AttributesGridAdapter;
import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheslistItemBinding;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheAttributeCategory;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.GeocacheSortContext;
import cgeo.geocaching.sorting.GlobalGPSDistanceComparator;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import androidx.core.text.HtmlCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class CacheListAdapter extends ArrayAdapter<Geocache> implements SectionIndexer {

    private LayoutInflater inflater = null;
    private final GeocacheSortContext sortContext;
    private Geopoint coords;
    private float azimuth = 0;
    private long lastGlobalGPSUpdate = 0L;
    private boolean selectMode = false;
    private GeocacheFilter currentGeocacheFilter = null;
    private List<Geocache> originalList = null;
    private final boolean isLiveList = Settings.isLiveList();

    private final Set<CompassMiniView> compasses = new LinkedHashSet<>();
    private final Set<DistanceView> distances = new LinkedHashSet<>();
    private final CacheListType cacheListType;
    private List<AbstractList> storedLists = null;
    private String currentListTitle = "";
    private final Resources res;
    /**
     * Resulting list of caches
     */
    private final List<Geocache> list;


    private static final int SWIPE_MIN_DISTANCE = 60;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    /**
     * time in milliseconds after which the list may be resorted due to position updates
     */
    private static final int PAUSE_BETWEEN_GLOBAL_GPS_UPDATE = 1000;

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

    public CacheListAdapter(final Activity activity, final List<Geocache> list, final CacheListType cacheListType, final GeocacheSortContext sortContext) {
        super(activity, 0, list);
        final GeoData currentGeo = Sensors.getInstance().currentGeo();
        coords = currentGeo.getCoords();
        this.sortContext = sortContext;
        this.res = activity.getResources();
        this.cacheListType = cacheListType;
        this.list = list;
        checkSpecialSortOrder();
        buildFastScrollIndex();
        GlobalGPSDistanceComparator.updateGlobalGps(coords);
    }

    public List<Geocache> getList() {
        return list;
    }

    public void setList(final Collection<Geocache> list) {
        this.list.clear();
        this.list.addAll(list);
        this.originalList = null;

        forceFilter();
        checkSpecialSortOrder();
        forceSort();

        notifyDataSetChanged();
    }

    public void setStoredLists(final List<AbstractList> storedLists) {
        this.storedLists = storedLists;
    }

    public void setCurrentListTitle(final String currentListTitle) {
        this.currentListTitle = currentListTitle;
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
     * Refilter list of caches (e.g. after new caches were added to the list after reload)
     */
    public void forceFilter() {
        //simply reapply the existing filter
        setFilter(this.currentGeocacheFilter, true);
    }

    /**
     * Apply a new filter to the adapter (e.g. after filter was changed by user in menu)
     */
    public void setFilter(final GeocacheFilter advancedFilter) {
        setFilter(advancedFilter, false);
    }

    private void setFilter(final GeocacheFilter advancedFilter, final boolean force) {

        // Backup current caches list if it isn't backed up yet
        if (originalList == null) {
            originalList = new ArrayList<>(list);
        }

        if (!force && currentGeocacheFilter == advancedFilter) {
            return;
        }
        if (!force && currentGeocacheFilter != null && advancedFilter != null && currentGeocacheFilter.toConfig().equals(advancedFilter.toConfig())) {
            return;
        }

        // If there is already a filter in place, this is a request to change or clear the filter, so we have to
        // replace the original cache list
        if (hasActiveFilter()) {
            list.clear();
            list.addAll(originalList);
        }

        currentGeocacheFilter = advancedFilter;

        performFiltering();

        notifyDataSetChanged();
    }

    private void performFiltering() {
        // Do the filtering or clear it
        if (currentGeocacheFilter != null && currentGeocacheFilter.isFiltering()) {
            currentGeocacheFilter.filterList(list);
        }
    }

    public boolean hasActiveFilter() {
        return currentGeocacheFilter != null && currentGeocacheFilter.isFiltering();
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

    public int getOriginalListCount() {
        return originalList == null ? list.size() : originalList.size();
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

    public void selectNextCaches(final int amount) {
        int remaining = amount; // how many caches are left to check?
        final int max = list.size();
        for (int i = 0; i < max && remaining > 0; i++) {
            final Geocache cache = list.get(i);
            if (!cache.isStatusChecked()) {
                cache.setStatusChecked(true);
                remaining--;
            }
        }
        notifyDataSetChanged();
    }

    public void showAttributes() {
        final Context context = getContext();

        // collect attributes and counters
        final Map<String, Integer> attributes = new HashMap<>();
        CacheAttribute ca;
        final int max = list.size();
        for (int i = 0; i < max; i++) {
            for (String attr : list.get(i).getAttributes()) {
                // OC attributes are always positive, to count them with GC attributes append "_yes"
                final String attrVal;
                ca = CacheAttribute.getByName(attr);
                if (ca != null && attr.equals(ca.rawName)) {
                    attrVal = ca.getValue(true);
                } else {
                    attrVal = attr;
                }
                final Integer count = attributes.get(attrVal);
                if (count == null) {
                    attributes.put(attrVal, 1);
                } else {
                    attributes.put(attrVal, count + 1);
                }
            }
        }

        // traverse by category and attribute order
        final ArrayList<String> orderedAttributeNames = new ArrayList<>();
        final StringBuilder attributesText = new StringBuilder();
        CacheAttributeCategory lastCategory = null;
        for (CacheAttributeCategory category : CacheAttributeCategory.getOrderedCategoryList()) {
            for (CacheAttribute attr : CacheAttribute.getByCategory(category)) {
                for (Boolean enabled : Arrays.asList(false, true)) {
                    final String key = attr.getValue(enabled);
                    final Integer value = attributes.get(key);
                    if (value != null && value > 0) {
                        if (lastCategory != category) {
                            if (lastCategory != null) {
                                attributesText.append("<br /><br />");
                            }
                            attributesText.append("<b><u>").append(category.getName(context)).append("</u></b><br />");
                            lastCategory = category;
                        } else {
                            attributesText.append("<br />");
                        }
                        orderedAttributeNames.add(key);
                        attributesText.append(attr.getL10n(enabled)).append(": ").append(value);
                    }
                }
            }
        }

        final View v = LayoutInflater.from(context).inflate(R.layout.cachelist_attributeoverview, null);
        ((WrappingGridView) v.findViewById(R.id.attributes_grid)).setAdapter(new AttributesGridAdapter((Activity) context, orderedAttributeNames, null));
        ((TextView) v.findViewById(R.id.attributes_text)).setText(HtmlCompat.fromHtml(attributesText.toString(), 0));
        Dialogs.bottomSheetDialogWithActionbar(context, v, R.string.cache_filter_attributes).show();
    }

    public void forceSort() {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        if (isSortedByDistance()) {
            checkUpdateGlobalGPS(true);
        }

        sortContext.getComparator().sort(list);

        notifyDataSetChanged();
    }

    public void setActualCoordinates(@NonNull final Geopoint coords) {
        this.coords = coords;
        checkUpdateGlobalGPS(false);

        for (final DistanceView distance : distances) {
            distance.update(coords);
        }
        for (final CompassMiniView compass : compasses) {
            compass.updateCurrentCoords(coords);
        }
        if (isSortedByDistance()) {
            forceSort();
        }
    }

    private void checkUpdateGlobalGPS(final boolean force) {
        if (!force && (System.currentTimeMillis() - lastGlobalGPSUpdate) <= PAUSE_BETWEEN_GLOBAL_GPS_UPDATE) {
            return;
        }
        if (coords == null) {
            return;
        }
        GlobalGPSDistanceComparator.updateGlobalGps(coords);

        notifyDataSetChanged();
        lastGlobalGPSUpdate = System.currentTimeMillis();
    }

    private boolean isSortedByDistance() {
        return sortContext.getType() == GeocacheSortContext.SortType.DISTANCE;
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
        holder.binding.distance.setCacheData(cache.getCoords(), cache.getDistance());
        holder.binding.distance.update(coords);

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
        boolean eventsOnly = !list.isEmpty();
        for (final Geocache cache : list) {
            if (!cache.isEventCache()) {
                eventsOnly = false;
                break;
            }
        }
        sortContext.setEventList(eventsOnly);
    }

    /**
     * detect whether all caches in this list belong to a series with similar names
     */
    private void checkSeries() {
        boolean series = false;
        if (list.size() < 3 || list.size() > 500) {
            return;
        }
        final ArrayList<String> names = new ArrayList<>();
        final ArrayList<String> reverseNames = new ArrayList<>();
        for (final Geocache cache : list) {
            final String name = cache.getName();
            names.add(name);
            reverseNames.add(StringUtils.reverse(name));
        }
        final String commonPrefix = StringUtils.getCommonPrefix(names.toArray(new String[0]));
        if (StringUtils.length(commonPrefix) >= MIN_COMMON_CHARACTERS_SERIES) {
            series = true;
        } else {
            final String commonSuffix = StringUtils.getCommonPrefix(reverseNames.toArray(new String[0]));
            if (StringUtils.length(commonSuffix) >= MIN_COMMON_CHARACTERS_SERIES) {
                series = true;
            }
        }
        sortContext.setSeriesList(series);
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
        if (position < 0 || position >= list.size()) {
            return " ";
        }
        try {
            return sortContext.getComparator().getSortableSection(list.get(position));
        } catch (NullPointerException e) {
            return " ";
        }
    }

}
