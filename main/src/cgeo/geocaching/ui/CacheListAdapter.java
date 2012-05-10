package cgeo.geocaching.ui;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.IGeoData;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filter.IFilter;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DistanceComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.utils.Log;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Spannable;
import android.text.Spanned;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CacheListAdapter extends ArrayAdapter<cgCache> {

    private LayoutInflater inflater = null;
    private CacheComparator cacheComparator = null;
    private Geopoint coords;
    private float azimuth = 0;
    private long lastSort = 0L;
    private boolean selectMode = false;
    private IFilter currentFilter = null;
    private List<cgCache> originalList = null;

    final private Set<CompassMiniView> compasses = new LinkedHashSet<CompassMiniView>();
    final private Set<DistanceView> distances = new LinkedHashSet<DistanceView>();
    final private CacheListType cacheListType;
    final private Resources res;
    /** Resulting list of caches */
    final private List<cgCache> list;

    private static final int SWIPE_MIN_DISTANCE = 60;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    private static final SparseArray<Drawable> gcIconDrawables = new SparseArray<Drawable>();
    /**
     * time in milliseconds after which the list may be resorted due to position updates
     */
    private static final int PAUSE_BETWEEN_LIST_SORT = 1000;

    private static final int[] RATING_BACKGROUND = new int[3];
    static {
        if (Settings.isLightSkin()) {
            RATING_BACKGROUND[0] = R.drawable.favourite_background_red_light;
            RATING_BACKGROUND[1] = R.drawable.favourite_background_orange_light;
            RATING_BACKGROUND[2] = R.drawable.favourite_background_green_light;
        } else {
            RATING_BACKGROUND[0] = R.drawable.favourite_background_red_dark;
            RATING_BACKGROUND[1] = R.drawable.favourite_background_orange_dark;
            RATING_BACKGROUND[2] = R.drawable.favourite_background_green_dark;
        }
    }

    public CacheListAdapter(final Activity activity, final List<cgCache> list, CacheListType cacheListType) {
        super(activity, 0, list);
        final IGeoData currentGeo = cgeoapplication.getInstance().currentGeo();
        if (currentGeo != null) {
            coords = currentGeo.getCoords();
        }
        this.res = activity.getResources();
        this.list = list;
        this.cacheListType = cacheListType;
        if (cacheListType == CacheListType.HISTORY) {
            cacheComparator = new VisitComparator();
        }

        final Drawable modifiedCoordinatesMarker = activity.getResources().getDrawable(R.drawable.marker_usermodifiedcoords);
        for (final CacheType cacheType : CacheType.values()) {
            // unmodified icon
            int hashCode = getIconHashCode(cacheType, false);
            gcIconDrawables.put(hashCode, activity.getResources().getDrawable(cacheType.markerId));
            // icon with flag for user modified coordinates
            hashCode = getIconHashCode(cacheType, true);
            Drawable[] layers = new Drawable[2];
            layers[0] = activity.getResources().getDrawable(cacheType.markerId);
            layers[1] = modifiedCoordinatesMarker;
            LayerDrawable ld = new LayerDrawable(layers);
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

    public CacheComparator getCacheComparator() {
        return cacheComparator;
    }

    public cgCache findCacheByGeocode(String geocode) {
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
            originalList = new ArrayList<cgCache>(list);

            currentFilter.filter(list);
        }
    }

    /**
     * Called after a user action on the filter menu.
     */
    public void setFilter(final IFilter filter) {
        // Backup current caches list if it isn't backed up yet
        if (originalList == null) {
            originalList = new ArrayList<cgCache>(list);
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
        for (cgCache cache : list) {
            if (cache.isStatusChecked()) {
                checked++;
            }
        }
        return checked;
    }

    public void setSelectMode(final boolean selectMode) {
        this.selectMode = selectMode;

        if (!selectMode) {
            for (final cgCache cache : list) {
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
        for (cgCache cache : list) {
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
            Collections.sort(list, cacheComparator);
        }

        notifyDataSetChanged();
    }

    public void setActualCoordinates(final Geopoint coordsIn) {
        if (coordsIn == null) {
            return;
        }

        coords = coordsIn;
        updateSortByDistance();

        for (final DistanceView distance : distances) {
            distance.update(coordsIn);
        }
        for (final CompassMiniView compass : compasses) {
            compass.updateCurrentCoords(coordsIn);
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
        Collections.sort(list, new DistanceComparator(coords, list));
        notifyDataSetChanged();
        lastSort = System.currentTimeMillis();
    }

    private boolean isSortedByDistance() {
        return cacheComparator == null || cacheComparator instanceof DistanceComparator;
    }

    public void setActualHeading(Float directionNow) {
        if (directionNow == null) {
            return;
        }

        azimuth = directionNow;

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

        final cgCache cache = getItem(position);

        View v = rowView;

        CacheListView holder;
        if (v == null) {
            v = inflater.inflate(R.layout.caches_item, null);

            holder = new CacheListView();
            holder.checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            holder.logStatusMark = (ImageView) v.findViewById(R.id.log_status_mark);
            holder.text = (TextView) v.findViewById(R.id.text);
            holder.directionLayout = (RelativeLayout) v.findViewById(R.id.direction_layout);
            holder.distance = (DistanceView) v.findViewById(R.id.distance);
            holder.direction = (CompassMiniView) v.findViewById(R.id.direction);
            holder.dirImgLayout = (RelativeLayout) v.findViewById(R.id.dirimg_layout);
            holder.dirImg = (ImageView) v.findViewById(R.id.dirimg);
            holder.inventory = (ImageView) v.findViewById(R.id.inventory);
            holder.favourite = (TextView) v.findViewById(R.id.favourite);
            holder.info = (TextView) v.findViewById(R.id.info);

            v.setTag(holder);
        } else {
            holder = (CacheListView) v.getTag();
        }

        final boolean lightSkin = Settings.isLightSkin();

        final TouchListener touchLst = new TouchListener(cache);
        v.setOnClickListener(touchLst);
        v.setOnLongClickListener(touchLst);
        v.setOnTouchListener(touchLst);
        v.setLongClickable(true);

        if (selectMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
        }
        else {
            holder.checkbox.setVisibility(View.GONE);
        }

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

        if (cache.getNameSp() == null) {
            cache.setNameSp((new Spannable.Factory()).newSpannable(cache.getName()));
            if (cache.isDisabled() || cache.isArchived()) { // strike
                cache.getNameSp().setSpan(new StrikethroughSpan(), 0, cache.getNameSp().toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        holder.text.setText(cache.getNameSp(), TextView.BufferType.SPANNABLE);
        holder.text.setCompoundDrawablesWithIntrinsicBounds(getCacheIcon(cache), null, null, null);

        if (cache.getInventoryItems() > 0) {
            holder.inventory.setVisibility(View.VISIBLE);
        } else {
            holder.inventory.setVisibility(View.GONE);
        }

        boolean setDiDi = false;
        if (cache.getCoords() != null) {
            holder.direction.setVisibility(View.VISIBLE);
            holder.direction.updateAzimuth(azimuth);
            if (coords != null) {
                holder.distance.update(coords);
                holder.direction.updateCurrentCoords(coords);
            }
            setDiDi = true;
        } else {
            if (cache.getDistance() != null) {
                holder.distance.setDistance(cache.getDistance());
                setDiDi = true;
            }
            if (cache.getDirection() != null) {
                holder.direction.setVisibility(View.VISIBLE);
                holder.direction.updateAzimuth(azimuth);
                holder.direction.updateHeading(cache.getDirection());
                setDiDi = true;
            }
        }

        if (setDiDi) {
            holder.directionLayout.setVisibility(View.VISIBLE);
            holder.dirImgLayout.setVisibility(View.GONE);
        } else {
            holder.directionLayout.setVisibility(View.GONE);
            holder.distance.clear();

            final Bitmap dirImgPre = BitmapFactory.decodeFile(DirectionImage.getDirectionFile(cache.getGeocode(), false).getPath());
            final Bitmap dirImg;
            if (dirImgPre != null) { // null happens for invalid caches (not yet released)
                dirImg = dirImgPre.copy(Bitmap.Config.ARGB_8888, true);
                dirImgPre.recycle();
            }
            else {
                dirImg = null;
            }

            if (dirImg != null) {
                if (!lightSkin) {
                    final int length = dirImg.getWidth() * dirImg.getHeight();
                    final int[] pixels = new int[length];
                    dirImg.getPixels(pixels, 0, dirImg.getWidth(), 0, 0, dirImg.getWidth(), dirImg.getHeight());
                    for (int i = 0; i < length; i++) {
                        if (pixels[i] == 0xff000000) { // replace black with white
                            pixels[i] = 0xffffffff;
                        }
                    }
                    dirImg.setPixels(pixels, 0, dirImg.getWidth(), 0, 0, dirImg.getWidth(), dirImg.getHeight());
                }

                holder.dirImg.setImageBitmap(dirImg);
                holder.dirImgLayout.setVisibility(View.VISIBLE);
            } else {
                holder.dirImg.setImageBitmap(null);
                holder.dirImgLayout.setVisibility(View.GONE);
            }
        }

        holder.favourite.setText(Integer.toString(cache.getFavoritePoints()));

        int favoriteBack;
        // set default background, neither vote nor rating may be available
        if (lightSkin) {
            favoriteBack = R.drawable.favourite_background_light;
        } else {
            favoriteBack = R.drawable.favourite_background_dark;
        }
        final float myVote = cache.getMyVote();
        if (myVote > 0) { // use my own rating for display, if I have voted
            if (myVote >= 4) {
                favoriteBack = RATING_BACKGROUND[2];
            } else if (myVote >= 3) {
                favoriteBack = RATING_BACKGROUND[1];
            } else if (myVote > 0) {
                favoriteBack = RATING_BACKGROUND[0];
            }
        } else {
            final float rating = cache.getRating();
            if (rating >= 3.5) {
                favoriteBack = RATING_BACKGROUND[2];
            } else if (rating >= 2.1) {
                favoriteBack = RATING_BACKGROUND[1];
            } else if (rating > 0.0) {
                favoriteBack = RATING_BACKGROUND[0];
            }
        }
        holder.favourite.setBackgroundResource(favoriteBack);

        if (cacheListType == CacheListType.HISTORY && cache.getVisitedDate() > 0) {
            final ArrayList<String> infos = new ArrayList<String>();
            infos.add(StringUtils.upperCase(cache.getGeocode()));
            infos.add(Formatter.formatDate(cache.getVisitedDate()));
            infos.add(Formatter.formatTime(cache.getVisitedDate()));
            holder.info.setText(StringUtils.join(infos, Formatter.SEPARATOR));
        } else {
            final ArrayList<String> infos = new ArrayList<String>();
            if (StringUtils.isNotBlank(cache.getGeocode())) {
                infos.add(cache.getGeocode());
            }
            if (cache.hasDifficulty()) {
                infos.add("D " + String.format("%.1f", cache.getDifficulty()));
            }
            if (cache.hasTerrain()) {
                infos.add("T " + String.format("%.1f", cache.getTerrain()));
            }

            // don't show "not chosen" for events and virtuals, that should be the normal case
            if (cache.getSize() != CacheSize.UNKNOWN && cache.showSize()) {
                infos.add(cache.getSize().getL10n());
            } else if (cache.isEventCache() && cache.getHiddenDate() != null) {
                infos.add(Formatter.formatShortDate(cache.getHiddenDate().getTime()));
            }

            if (cache.isPremiumMembersOnly()) {
                infos.add(res.getString(R.string.cache_premium));
            }
            if (cacheListType != CacheListType.OFFLINE && cacheListType != CacheListType.HISTORY && cache.getListId() > 0) {
                infos.add(res.getString(R.string.cache_offline));
            }
            holder.info.setText(StringUtils.join(infos, Formatter.SEPARATOR));
        }

        return v;
    }

    private static Drawable getCacheIcon(cgCache cache) {
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

        private final cgCache cache;

        public SelectionCheckBoxListener(cgCache cache) {
            this.cache = cache;
        }

        public void onClick(View view) {
            final boolean checkNow = ((CheckBox) view).isChecked();
            cache.setStatusChecked(checkNow);
        }
    }

    private class TouchListener implements View.OnLongClickListener, View.OnClickListener, View.OnTouchListener {

        private boolean touch = true;
        private final GestureDetector gestureDetector;
        private final cgCache cache;

        public TouchListener(final cgCache cache) {
            this.cache = cache;
            final FlingGesture dGesture = new FlingGesture(cache);
            gestureDetector = new GestureDetector(getContext(), dGesture);
        }

        // tap on item
        public void onClick(View view) {
            if (!touch) {
                touch = true;
                return;
            }

            if (isSelectMode()) {
                cache.setStatusChecked(!cache.isStatusChecked());
                notifyDataSetChanged();
                return;
            }

            // load cache details
            CacheDetailActivity.startActivity(getContext(), cache.getGeocode(), cache.getName());
        }

        // long tap on item
        public boolean onLongClick(View view) {
            if (!touch) {
                touch = true;
                return true;
            }

            return view.showContextMenu();
        }

        // swipe on item
        public boolean onTouch(View view, MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                touch = false;
                return true;
            }

            return false;
        }
    }

    private class FlingGesture extends GestureDetector.SimpleOnGestureListener {

        private final cgCache cache;

        public FlingGesture(cgCache cache) {
            this.cache = cache;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                // left to right swipe
                if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (!selectMode) {
                        switchSelectMode();
                        cache.setStatusChecked(true);
                    }
                    return true;
                }

                // right to left swipe
                if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (selectMode) {
                        switchSelectMode();
                    }
                    return true;
                }
            } catch (Exception e) {
                Log.w("CacheListAdapter.FlingGesture.onFling: " + e.toString());
            }

            return false;
        }
    }

    public List<cgCache> getFilteredList() {
        return list;
    }

    public List<cgCache> getCheckedCaches() {
        final ArrayList<cgCache> result = new ArrayList<cgCache>();
        for (cgCache cache : list) {
            if (cache.isStatusChecked()) {
                result.add(cache);
            }
        }
        return result;
    }

    public List<cgCache> getCheckedOrAllCaches() {
        final List<cgCache> result = getCheckedCaches();
        if (!result.isEmpty()) {
            return result;
        }
        return new ArrayList<cgCache>(list);
    }

    public int getCheckedOrAllCount() {
        final int checked = getCheckedCount();
        if (checked > 0) {
            return checked;
        }
        return list.size();
    }
}
