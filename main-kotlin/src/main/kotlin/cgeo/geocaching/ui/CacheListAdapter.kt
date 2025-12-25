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

package cgeo.geocaching.ui

import cgeo.geocaching.AttributesGridAdapter
import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.R
import cgeo.geocaching.databinding.CacheslistItemBinding
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.enumerations.CacheAttributeCategory
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.list.AbstractList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.sensors.GeoData
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.sorting.GeocacheSortContext
import cgeo.geocaching.sorting.GlobalGPSDistanceComparator
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.TextUtils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.SectionIndexer
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.core.text.HtmlCompat

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Set

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils

class CacheListAdapter : ArrayAdapter()<Geocache> : SectionIndexer {

    private var inflater: LayoutInflater = null
    private final GeocacheSortContext sortContext
    private Geopoint coords
    private var azimuth: Float = 0
    private var lastGlobalGPSUpdate: Long = 0L
    private var selectMode: Boolean = false
    private var currentGeocacheFilter: GeocacheFilter = null
    private var originalList: List<Geocache> = null
    private val isLiveList: Boolean = Settings.isLiveList()

    private val compasses: Set<CompassMiniView> = LinkedHashSet<>()
    private val distances: Set<DistanceView> = LinkedHashSet<>()
    private final CacheListType cacheListType
    private var storedLists: List<AbstractList> = null
    private var currentListTitle: String = ""
    private final Resources res
    /**
     * Resulting list of caches
     */
    private final List<Geocache> list


    /**
     * time in milliseconds after which the list may be resorted due to position updates
     */
    private static val PAUSE_BETWEEN_GLOBAL_GPS_UPDATE: Int = 1000

    /**
     * automatically order cache series by name, if they all have a common suffix or prefix at least these many
     * characters
     */
    private static val MIN_COMMON_CHARACTERS_SERIES: Int = 4

    // variables for section indexer
    private HashMap<String, Integer> mapFirstPosition
    private HashMap<String, Integer> mapSection
    private String[] sections

    /**
     * view holder for the cache list adapter
     */
    public static class ViewHolder : AbstractViewHolder() {
        private CacheListType cacheListType
        var cache: Geocache = null
        private final CacheslistItemBinding binding

        public ViewHolder(final View view) {
            super(view)
            binding = CacheslistItemBinding.bind(view)
        }
    }

    public CacheListAdapter(final Activity activity, final List<Geocache> list, final CacheListType cacheListType, final GeocacheSortContext sortContext) {
        super(activity, 0, list)
        val currentGeo: GeoData = LocationDataProvider.getInstance().currentGeo()
        coords = currentGeo.getCoords()
        this.sortContext = sortContext
        this.res = activity.getResources()
        this.cacheListType = cacheListType
        this.list = list
        checkSpecialSortOrder()
        buildFastScrollIndex()
        GlobalGPSDistanceComparator.updateGlobalGps(coords)
    }

    public List<Geocache> getList() {
        return list
    }

    public Unit setList(final Collection<Geocache> list) {
        this.list.clear()
        this.list.addAll(list)
        this.originalList = null

        forceFilter()
        checkSpecialSortOrder()
        forceSort()

        notifyDataSetChanged()
    }

    public Unit setElement(final Geocache geocache) {
        val geocode: String = geocache.getGeocode()
        for (Int i = 0; i < getCount(); i++) {
            if (getItem(i).getGeocode().equalsIgnoreCase(geocode)) {
                this.list.set(i, geocache)
            }
        }
    }

    public Unit setStoredLists(final List<AbstractList> storedLists) {
        this.storedLists = storedLists
    }

    public Unit setCurrentListTitle(final String currentListTitle) {
        this.currentListTitle = currentListTitle
    }

    private Boolean isHistory() {
        return cacheListType == CacheListType.HISTORY
    }

    public Geocache findCacheByGeocode(final String geocode) {
        for (Int i = 0; i < getCount(); i++) {
            if (getItem(i).getGeocode().equalsIgnoreCase(geocode)) {
                return getItem(i)
            }
        }

        return null
    }

    /**
     * Refilter list of caches (e.g. after caches were added to the list after reload)
     */
    public Unit forceFilter() {
        //simply reapply the existing filter
        setFilter(this.currentGeocacheFilter, true)
    }

    /**
     * Apply a filter to the adapter (e.g. after filter was changed by user in menu)
     */
    public Unit setFilter(final GeocacheFilter advancedFilter) {
        setFilter(advancedFilter, false)
    }

    private Unit setFilter(final GeocacheFilter advancedFilter, final Boolean force) {

        // Backup current caches list if it isn't backed up yet
        if (originalList == null) {
            originalList = ArrayList<>(list)
        }

        if (!force && currentGeocacheFilter == advancedFilter) {
            return
        }
        if (!force && currentGeocacheFilter != null && advancedFilter != null && currentGeocacheFilter.toConfig() == (advancedFilter.toConfig())) {
            return
        }

        // If there is already a filter in place, this is a request to change or clear the filter, so we have to
        // replace the original cache list
        if (hasActiveFilter()) {
            list.clear()
            list.addAll(originalList)
        }

        currentGeocacheFilter = advancedFilter

        performFiltering()

        notifyDataSetChanged()
    }

    private Unit performFiltering() {
        // Do the filtering or clear it
        if (currentGeocacheFilter != null && currentGeocacheFilter.isFiltering()) {
            currentGeocacheFilter.filterList(list)
        }
    }

    public Boolean hasActiveFilter() {
        return currentGeocacheFilter != null && currentGeocacheFilter.isFiltering()
    }

    public Int getCheckedCount() {
        if (!isSelectMode()) {
            return 0
        }

        Int checkedCount = 0
        for (final Geocache cache : list) {
            if (cache.isStatusChecked()) {
                checkedCount++
            }
        }
        return checkedCount
    }

    public Int getOriginalListCount() {
        return originalList == null ? list.size() : originalList.size()
    }

    public Unit setSelectMode(final Boolean selectMode) {
        this.selectMode = selectMode

        if (!selectMode) {
            for (final Geocache cache : list) {
                cache.setStatusChecked(false)
            }
        }
        notifyDataSetChanged()
    }

    public Boolean isSelectMode() {
        return selectMode
    }

    public Unit switchSelectMode() {
        setSelectMode(!isSelectMode())
    }

    public Unit invertSelection() {
        for (final Geocache cache : list) {
            cache.setStatusChecked(!cache.isStatusChecked())
        }
        notifyDataSetChanged()
    }

    public Unit selectNextCaches(final Int amount) {
        Int remaining = amount; // how many caches are left to check?
        val max: Int = list.size()
        for (Int i = 0; i < max && remaining > 0; i++) {
            val cache: Geocache = list.get(i)
            if (!cache.isStatusChecked()) {
                cache.setStatusChecked(true)
                remaining--
            }
        }
        notifyDataSetChanged()
    }

    public Unit showAttributes(final Collection<Geocache> caches) {
        val context: Context = getContext()

        // collect attributes and counters
        val attributes: Map<String, Integer> = HashMap<>()
        CacheAttribute ca
        for (final Geocache cache : caches) {
            for (String attr : cache.getAttributes()) {
                // OC attributes are always positive, to count them with GC attributes append "_yes"
                final String attrVal
                ca = CacheAttribute.getByName(attr)
                if (ca != null && attr == (ca.rawName)) {
                    attrVal = ca.getValue(true)
                } else {
                    attrVal = attr
                }
                val count: Integer = attributes.get(attrVal)
                if (count == null) {
                    attributes.put(attrVal, 1)
                } else {
                    attributes.put(attrVal, count + 1)
                }
            }
        }

        // traverse by category and attribute order
        val orderedAttributeNames: ArrayList<String> = ArrayList<>()
        val attributesText: StringBuilder = StringBuilder()
        CacheAttributeCategory lastCategory = null
        for (CacheAttributeCategory category : CacheAttributeCategory.getOrderedCategoryList()) {
            for (CacheAttribute attr : CacheAttribute.getByCategory(category)) {
                for (Boolean enabled : Arrays.asList(false, true)) {
                    val key: String = attr.getValue(enabled)
                    val value: Integer = attributes.get(key)
                    if (value != null && value > 0) {
                        if (lastCategory != category) {
                            if (lastCategory != null) {
                                attributesText.append("<br /><br />")
                            }
                            attributesText.append("<b><u>").append(category.getName(context)).append("</u></b><br />")
                            lastCategory = category
                        } else {
                            attributesText.append("<br />")
                        }
                        orderedAttributeNames.add(key)
                        attributesText.append(attr.getL10n(enabled)).append(": ").append(value)
                    }
                }
            }
        }

        val v: View = LayoutInflater.from(context).inflate(R.layout.cachelist_attributeoverview, null)
        ((WrappingGridView) v.findViewById(R.id.attributes_grid)).setAdapter(AttributesGridAdapter((Activity) context, orderedAttributeNames, null))
        ((TextView) v.findViewById(R.id.attributes_text)).setText(HtmlCompat.fromHtml(attributesText.toString(), 0))
        Dialogs.bottomSheetDialogWithActionbar(context, v, R.string.cache_filter_attributes).show()
    }

    public Unit forceSort() {
        if (CollectionUtils.isEmpty(list)) {
            return
        }

        if (isSortedByDistance()) {
            checkUpdateGlobalGPS(true)
        }

        sortContext.getSort().getComparator().sort(list)

        notifyDataSetChanged()
    }

    public Unit setActualCoordinates(final Geopoint coords) {
        this.coords = coords
        checkUpdateGlobalGPS(false)

        for (final DistanceView distance : distances) {
            distance.update(coords)
        }
        for (final CompassMiniView compass : compasses) {
            compass.updateCurrentCoords(coords)
        }
        if (isSortedByDistance()) {
            forceSort()
        }
    }

    private Unit checkUpdateGlobalGPS(final Boolean force) {
        if (!force && (System.currentTimeMillis() - lastGlobalGPSUpdate) <= PAUSE_BETWEEN_GLOBAL_GPS_UPDATE) {
            return
        }
        if (coords == null) {
            return
        }
        GlobalGPSDistanceComparator.updateGlobalGps(coords)

        notifyDataSetChanged()
        lastGlobalGPSUpdate = System.currentTimeMillis()
    }

    private Boolean isSortedByDistance() {
        return sortContext.getSort().getType() == GeocacheSort.SortType.DISTANCE
    }

    public Unit setActualHeading(final Float direction) {
        if (Math.abs(AngleUtils.difference(azimuth, direction)) < 5) {
            return
        }

        azimuth = direction
        for (final CompassMiniView compass : compasses) {
            compass.updateAzimuth(azimuth)
        }
    }

    public static Unit updateViewHolder(final ViewHolder holder, final Geocache cache, final Resources res) {
        if (cache.isFound() && cache.hasLogOffline()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_green_orange)
        } else if (cache.isFound()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_green_more)
        } else if (cache.hasLogOffline()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_orange)
        } else if (cache.isDNF()) {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_red)
        } else {
            holder.binding.logStatusMark.setImageResource(R.drawable.mark_transparent)
        }
        holder.binding.textIcon.setImageDrawable(MapMarkerUtils.getCacheMarker(res, cache, holder.cacheListType, Settings.getIconScaleEverywhere()).getDrawable())
    }

    override     public View getView(final Int position, final View rowView, final ViewGroup parent) {
        if (inflater == null) {
            inflater = LayoutInflater.from(getContext())
        }

        if (position > getCount()) {
            Log.w("CacheListAdapter.getView: Attempt to access missing item #" + position)
            return null
        }

        val cache: Geocache = getItem(position)

        View v = rowView

        final ViewHolder holder
        if (v == null) {
            v = inflater.inflate(R.layout.cacheslist_item, parent, false)
            holder = ViewHolder(v)
        } else {
            holder = (ViewHolder) v.getTag()
        }
        holder.cache = cache

        val touchListener: TouchListener = TouchListener(cache, this)
        v.setOnClickListener(touchListener)
        v.setOnLongClickListener(touchListener)
        v.setOnTouchListener(touchListener)

        holder.binding.checkbox.setVisibility(selectMode ? View.VISIBLE : View.GONE)
        holder.binding.checkbox.setChecked(cache.isStatusChecked())
        holder.binding.checkbox.setOnClickListener(SelectionCheckBoxListener(cache, this))

        distances.add(holder.binding.distance)
        holder.binding.distance.setCacheData(cache.getCoords(), cache.getDistance())

        Geopoint targetCoords = sortContext.getSort().getTargetCoords()
        if (null == targetCoords) {
            targetCoords = coords
        } else {
            holder.binding.distance.setTypeface(Typeface.BOLD_ITALIC)
        }
        holder.binding.distance.update(targetCoords)

        compasses.add(holder.binding.direction)
        holder.binding.direction.setTargetCoords(cache.getCoords())
        holder.binding.text.setText(TextUtils.coloredCacheText(getContext(), cache, StringUtils.defaultIfBlank(cache.getName(), "")), TextView.BufferType.SPANNABLE)
        holder.cacheListType = cacheListType
        updateViewHolder(holder, cache, res)

        val inventorySize: Int = cache.getInventoryItems()
        if (inventorySize > 0) {
            holder.binding.inventory.setText(String.format(Locale.getDefault(), "%d", inventorySize))
            holder.binding.inventory.setVisibility(View.VISIBLE)
        } else {
            holder.binding.inventory.setVisibility(View.GONE)
        }

        // only show the direction if this is enabled in the settings
        if (isLiveList) {
            if (cache.getCoords() != null) {
                holder.binding.direction.setVisibility(View.VISIBLE)
                holder.binding.dirimg.setVisibility(View.GONE)
                holder.binding.direction.updateAzimuth(azimuth)

                if (targetCoords != null) {
                    holder.binding.direction.updateCurrentCoords(targetCoords)
                }
            } else if (cache.getDirection() != null) {
                holder.binding.direction.setVisibility(View.VISIBLE)
                holder.binding.dirimg.setVisibility(View.GONE)
                holder.binding.direction.updateAzimuth(azimuth)
                holder.binding.direction.updateHeading(cache.getDirection())
            } else if (StringUtils.isNotBlank(cache.getDirectionImg())) {
                holder.binding.dirimg.setVisibility(View.INVISIBLE)
                holder.binding.direction.setVisibility(View.GONE)
                DirectionImage.fetchDrawable(cache.getDirectionImg()).observeOn(AndroidSchedulers.mainThread()).subscribe(bitmapDrawable -> {
                    if (cache == holder.cache) {
                        holder.binding.dirimg.setImageDrawable(bitmapDrawable)
                        holder.binding.dirimg.setVisibility(View.VISIBLE)
                    }
                })
            } else {
                holder.binding.dirimg.setVisibility(View.GONE)
                holder.binding.direction.setVisibility(View.GONE)
            }
        }

        val favCount: Int = cache.getFavoritePoints()
        holder.binding.favorite.setText(Formatter.formatFavCount(favCount))
        val rating: Float = cache.getRating()
        holder.binding.favorite.setBackgroundResource(rating >= 3.5 ? R.drawable.favorite_background_green : rating >= 2.1 ? R.drawable.favorite_background_orange : rating > 0.0 ? R.drawable.favorite_background_red : R.drawable.favorite_background)

        if (isHistory() && cache.getVisitedDate() > 0) {
            holder.binding.info.setText(Formatter.formatCacheInfoHistory(cache))
        } else {
            holder.binding.info.setText(Formatter.formatCacheInfoLong(cache, storedLists, currentListTitle))
        }

        return v
    }

    override     public Unit notifyDataSetChanged() {
        super.notifyDataSetChanged()
        distances.clear()
        compasses.clear()
        buildFastScrollIndex()
    }

    private static class SelectionCheckBoxListener : View.OnClickListener {

        private final Geocache cache
        private final WeakReference<CacheListAdapter> adapterRef

        SelectionCheckBoxListener(final Geocache cache, final CacheListAdapter adapter) {
            this.cache = cache
            adapterRef = WeakReference<>(adapter)
        }

        override         public Unit onClick(final View view) {
            val checkNow: Boolean = ((CheckBox) view).isChecked()
            cache.setStatusChecked(checkNow)
            val adapter: CacheListAdapter = adapterRef.get()
            if (adapter == null) {
                return
            }
        }
    }

    private static class TouchListener : View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        private final Geocache cache
        private final GestureDetector gestureDetector
        private final WeakReference<CacheListAdapter> adapterRef

        TouchListener(final Geocache cache, final CacheListAdapter adapter) {
            this.cache = cache
            gestureDetector = GestureDetector(adapter.getContext(), FlingGesture(cache, adapter))
            adapterRef = WeakReference<>(adapter)
        }

        // Tap on item
        override         public Unit onClick(final View view) {
            val adapter: CacheListAdapter = adapterRef.get()
            if (adapter == null) {
                return
            }
            if (adapter.isSelectMode()) {
                cache.setStatusChecked(!cache.isStatusChecked())
                adapter.notifyDataSetChanged()
            } else {
                CacheDetailActivity.startActivity(adapter.getContext(), cache.getGeocode(), cache.getName())
            }
        }

        // Long tap on item
        override         public Boolean onLongClick(final View view) {
            view.showContextMenu()
            return true
        }

        // Swipe on item
        @SuppressLint("ClickableViewAccessibility")
        override         public Boolean onTouch(final View view, final MotionEvent event) {
            return gestureDetector.onTouchEvent(event)

        }
    }

    private static class FlingGesture : GestureDetector().SimpleOnGestureListener {

        private static val SWIPE_MIN_DISTANCE: Int = 60
        private static val SWIPE_MAX_OFF_PATH: Int = 100

        private final Geocache cache
        private final WeakReference<CacheListAdapter> adapterRef

        FlingGesture(final Geocache cache, final CacheListAdapter adapter) {
            this.cache = cache
            adapterRef = WeakReference<>(adapter)
        }

        override         public Boolean onFling(final MotionEvent e1, final MotionEvent e2, final Float velocityX, final Float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false
                }
                val adapter: CacheListAdapter = adapterRef.get()
                if (adapter == null) {
                    return false
                }

                // horizontal swipe
                if (Math.abs(velocityX) > Math.abs(velocityY)) {

                    // left to right swipe
                    if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE) {
                        if (!adapter.selectMode) {
                            adapter.switchSelectMode()
                            cache.setStatusChecked(true)
                        }
                        return true
                    }

                    // right to left swipe
                    if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE) {
                        if (adapter.selectMode) {
                            adapter.switchSelectMode()
                        }
                        return true
                    }
                }
            } catch (final Exception e) {
                Log.w("CacheListAdapter.FlingGesture.onFling", e)
            }

            return false
        }
    }

    public List<Geocache> getFilteredList() {
        return list
    }

    public List<Geocache> getCheckedCaches() {
        val result: List<Geocache> = ArrayList<>()
        for (final Geocache cache : list) {
            if (cache.isStatusChecked()) {
                result.add(cache)
            }
        }
        return result
    }

    public List<Geocache> getCheckedOrAllCaches() {
        val result: List<Geocache> = getCheckedCaches()
        if (!result.isEmpty()) {
            return result
        }
        return ArrayList<>(list)
    }

    public Unit checkSpecialSortOrder() {
        checkEvents()
        checkSeries()
    }

    private Unit checkEvents() {
        Boolean eventsOnly = !list.isEmpty()
        for (final Geocache cache : list) {
            if (!cache.isEventCache()) {
                eventsOnly = false
                break
            }
        }
        sortContext.getSort().setEventList(eventsOnly)
    }

    /**
     * detect whether all caches in this list belong to a series with similar names
     */
    private Unit checkSeries() {
        Boolean series = false
        if (list.size() < 3 || list.size() > 500) {
            return
        }
        val names: ArrayList<String> = ArrayList<>()
        val reverseNames: ArrayList<String> = ArrayList<>()
        for (final Geocache cache : list) {
            val name: String = cache.getName()
            names.add(name)
            reverseNames.add(StringUtils.reverse(name))
        }
        val commonPrefix: String = StringUtils.getCommonPrefix(names.toArray(String[0]))
        if (StringUtils.length(commonPrefix) >= MIN_COMMON_CHARACTERS_SERIES) {
            series = true
        } else {
            val commonSuffix: String = StringUtils.getCommonPrefix(reverseNames.toArray(String[0]))
            if (StringUtils.length(commonSuffix) >= MIN_COMMON_CHARACTERS_SERIES) {
                series = true
            }
        }
        sortContext.getSort().setSeriesList(series)
    }

    // methods for section indexer

    private Unit buildFastScrollIndex() {
        mapFirstPosition = LinkedHashMap<>()
        val sectionList: ArrayList<String> = ArrayList<>()
        String lastComparable = null
        for (Int x = 0; x < list.size(); x++) {
            val comparable: String = getComparable(x)
            if (!StringUtils == (lastComparable, comparable)) {
                mapFirstPosition.put(comparable, x)
                sectionList.add(comparable)
                lastComparable = comparable
            }
        }
        sections = String[sectionList.size()]
        sectionList.toArray(sections)
        mapSection = LinkedHashMap<>()
        for (Int x = 0; x < sections.length; x++) {
            mapSection.put(sections[x], x)
        }
    }

    public Int getPositionForSection(final Int section) {
        if (sections == null || sections.length == 0) {
            return 0
        }
        val position: Integer = mapFirstPosition.get(sections[Math.max(0, Math.min(section, sections.length - 1))])
        return null == position ? 0 : position
    }

    public Int getSectionForPosition(final Int position) {
        val section: Integer = mapSection.get(getComparable(position))
        return null == section ? 0 : section
    }

    public Object[] getSections() {
        return sections
    }

    private String getComparable(final Int position) {
        if (position < 0 || position >= list.size()) {
            return " "
        }
        try {
            return sortContext.getSort().getComparator().getSortableSection(list.get(position))
        } catch (NullPointerException e) {
            return " "
        }
    }

}
