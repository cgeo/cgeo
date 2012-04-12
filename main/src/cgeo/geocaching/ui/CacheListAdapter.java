package cgeo.geocaching.ui;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
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
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CacheListAdapter extends ArrayAdapter<cgCache> {

    final private Resources res;
    /** Resulting list of caches */
    final private List<cgCache> list;
    private CacheListView holder = null;
    private LayoutInflater inflater = null;
    private CacheComparator cacheComparator = null;
    private Geopoint coords = null;
    private float azimuth = 0;
    private long lastSort = 0L;
    private boolean sort = true;
    private int checked = 0;
    private boolean selectMode = false;
    final private static Map<Integer, Drawable> gcIconDrawables = new HashMap<Integer, Drawable>();
    final private Set<CompassMiniView> compasses = new LinkedHashSet<CompassMiniView>();
    final private Set<DistanceView> distances = new LinkedHashSet<DistanceView>();
    final private int[] ratingBcgs = new int[3];
    final private float pixelDensity;
    private static final int SWIPE_MIN_DISTANCE = 60;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    private static final int SWIPE_DISTANCE = 80;
    private static final float SWIPE_OPACITY = 0.5f;
    /**
     * time in milliseconds after which the list may be resorted due to position updates
     */
    private static final int PAUSE_BETWEEN_LIST_SORT = 1000;
    private IFilter currentFilter = null;
    private List<cgCache> originalList = null;
    private final CacheListType cacheListType;

    public CacheListAdapter(final Activity activity, final List<cgCache> list, CacheListType cacheListType) {
        super(activity, 0, list);

        this.res = activity.getResources();
        this.list = list;
        this.cacheListType = cacheListType;
        if (cacheListType == CacheListType.HISTORY) {
            cacheComparator = new VisitComparator();
        }

        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        pixelDensity = metrics.density;

        Drawable modifiedCoordinatesMarker = activity.getResources().getDrawable(R.drawable.marker_usermodifiedcoords);
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

        if (Settings.isLightSkin()) {
            ratingBcgs[0] = R.drawable.favourite_background_red_light;
            ratingBcgs[1] = R.drawable.favourite_background_orange_light;
            ratingBcgs[2] = R.drawable.favourite_background_green_light;
        } else {
            ratingBcgs[0] = R.drawable.favourite_background_red_dark;
            ratingBcgs[1] = R.drawable.favourite_background_orange_dark;
            ratingBcgs[2] = R.drawable.favourite_background_green_dark;
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
        forceSort(coords);
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

    public boolean isFilter() {
        return currentFilter != null;
    }

    public String getFilterName() {
        return currentFilter.getName();
    }

    public int getChecked() {
        return checked;
    }

    public boolean setSelectMode(final boolean status, final boolean clear) {
        selectMode = status;

        if (!selectMode && clear) {
            for (final cgCache cache : list) {
                cache.setStatusChecked(false);
                cache.setStatusCheckedView(false);
            }
            checked = 0;
        } else if (selectMode) {
            for (final cgCache cache : list) {
                cache.setStatusCheckedView(false);
            }
        }
        checkChecked(0);

        notifyDataSetChanged();

        return selectMode;
    }

    public boolean getSelectMode() {
        return selectMode;
    }

    public void switchSelectMode() {
        selectMode = !selectMode;

        if (!selectMode) {
            for (final cgCache cache : list) {
                cache.setStatusChecked(false);
                cache.setStatusCheckedView(false);
            }
            checked = 0;
        } else {
            for (final cgCache cache : list) {
                cache.setStatusCheckedView(false);
            }
        }
        checkChecked(0);

        notifyDataSetChanged();
    }

    public void invertSelection() {
        int check = 0;

        for (cgCache cache : list) {
            if (cache.isStatusChecked()) {
                cache.setStatusChecked(false);
                cache.setStatusCheckedView(false);
            } else {
                cache.setStatusChecked(true);
                cache.setStatusCheckedView(true);

                check++;
            }
        }
        checkChecked(check);

        notifyDataSetChanged();
    }

    public void forceSort(final Geopoint coordsIn) {
        if (CollectionUtils.isEmpty(list) || !sort) {
            return;
        }

        if (isSortedByDistance()) {
            if (coordsIn == null) {
                return;
            }
            Collections.sort(list, new DistanceComparator(coordsIn, list));
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

        if (CollectionUtils.isNotEmpty(list) && (System.currentTimeMillis() - lastSort) > PAUSE_BETWEEN_LIST_SORT && sort && isSortedByDistance()) {
            Collections.sort(list, new DistanceComparator(coordsIn, list));
            notifyDataSetChanged();
            lastSort = System.currentTimeMillis();
        }

        for (final DistanceView distance : distances) {
            distance.update(coordsIn);
        }

        for (final CompassMiniView compass : compasses) {
            compass.updateCoords(coordsIn);
        }
    }

    private boolean isSortedByDistance() {
        return cacheComparator == null || cacheComparator instanceof DistanceComparator;
    }

    public void setActualHeading(Float directionNow) {
        if (directionNow == null) {
            return;
        }

        azimuth = directionNow;

        if (CollectionUtils.isNotEmpty(compasses)) {
            for (CompassMiniView compass : compasses) {
                compass.updateAzimuth(azimuth);
            }
        }
    }

    /**
     * clear all check marks
     *
     * @return
     */
    public boolean resetChecks() {
        if (list.isEmpty()) {
            return false;
        }
        if (checked <= 0) {
            return false;
        }

        boolean status = getSelectMode();
        int cleared = 0;
        for (cgCache cache : list) {
            if (cache.isStatusChecked()) {
                cache.setStatusChecked(false);

                checkChecked(-1);
                cleared++;
            }
        }
        setSelectMode(false, false);
        notifyDataSetChanged();

        return cleared > 0 || status;
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

        cgCache cache = getItem(position);

        View v = rowView;

        if (v == null) {
            v = inflater.inflate(R.layout.cache, null);

            holder = new CacheListView();
            holder.checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            holder.oneInfo = (RelativeLayout) v.findViewById(R.id.one_info);
            holder.oneCheckbox = (RelativeLayout) v.findViewById(R.id.one_checkbox);
            holder.logStatusMark = (ImageView) v.findViewById(R.id.log_status_mark);
            holder.text = (TextView) v.findViewById(R.id.text);
            holder.directionLayout = (RelativeLayout) v.findViewById(R.id.direction_layout);
            holder.distance = (DistanceView) v.findViewById(R.id.distance);
            holder.direction = (CompassMiniView) v.findViewById(R.id.direction);
            holder.dirImgLayout = (RelativeLayout) v.findViewById(R.id.dirimg_layout);
            holder.dirImg = (ImageView) v.findViewById(R.id.dirimg);
            holder.inventory = (RelativeLayout) v.findViewById(R.id.inventory);
            holder.favourite = (TextView) v.findViewById(R.id.favourite);
            holder.info = (TextView) v.findViewById(R.id.info);

            v.setTag(holder);
        } else {
            holder = (CacheListView) v.getTag();
        }

        if (cache.isOwn()) {
            if (Settings.isLightSkin()) {
                holder.oneInfo.setBackgroundResource(R.color.owncache_background_light);
                holder.oneCheckbox.setBackgroundResource(R.color.owncache_background_light);
            } else {
                holder.oneInfo.setBackgroundResource(R.color.owncache_background_dark);
                holder.oneCheckbox.setBackgroundResource(R.color.owncache_background_dark);
            }
        } else {
            if (Settings.isLightSkin()) {
                holder.oneInfo.setBackgroundResource(R.color.background_light);
                holder.oneCheckbox.setBackgroundResource(R.color.background_light);
            } else {
                holder.oneInfo.setBackgroundResource(R.color.background_dark);
                holder.oneCheckbox.setBackgroundResource(R.color.background_dark);
            }
        }

        final touchListener touchLst = new touchListener(cache.getGeocode(), cache.getName(), cache);
        v.setOnClickListener(touchLst);
        v.setOnLongClickListener(touchLst);
        v.setOnTouchListener(touchLst);
        v.setLongClickable(true);

        if (selectMode) {
            if (cache.isStatusCheckedView()) {
                moveRight(holder, cache, true); // move fast when already slided
            } else {
                moveRight(holder, cache, false);
            }
        } else if (cache.isStatusChecked()) {
            holder.checkbox.setChecked(true);
            if (cache.isStatusCheckedView()) {
                moveRight(holder, cache, true); // move fast when already slided
            } else {
                moveRight(holder, cache, false);
            }
        } else {
            holder.checkbox.setChecked(false);
            if (cache.isStatusCheckedView()) {
                moveLeft(holder, cache, false);
            } else {
                holder.oneInfo.clearAnimation();
            }
        }

        holder.checkbox.setOnClickListener(new checkBoxListener(cache));

        distances.add(holder.distance);
        holder.distance.setContent(cache.getCoords());
        compasses.add(holder.direction);
        holder.direction.setContent(cache.getCoords());

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

        if (holder.inventory.getChildCount() > 0) {
            holder.inventory.removeAllViews();
        }

        ImageView tbIcon = null;
        if (cache.getInventoryItems() > 0) {
            tbIcon = (ImageView) inflater.inflate(R.layout.trackable_icon, null);
            tbIcon.setImageResource(R.drawable.trackable_all);

            holder.inventory.addView(tbIcon);
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
                holder.direction.updateCoords(coords);
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
                if (!Settings.isLightSkin()) {
                    int length = dirImg.getWidth() * dirImg.getHeight();
                    int[] pixels = new int[length];
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
        if (Settings.isLightSkin()) {
            favoriteBack = R.drawable.favourite_background_light;
        } else {
            favoriteBack = R.drawable.favourite_background_dark;
        }
        float myVote = cache.getMyVote();
        if (myVote > 0) { // use my own rating for display, if I have voted
            if (myVote >= 4) {
                favoriteBack = ratingBcgs[2];
            } else if (myVote >= 3) {
                favoriteBack = ratingBcgs[1];
            } else if (myVote > 0) {
                favoriteBack = ratingBcgs[0];
            }
        } else {
            float rating = cache.getRating();
            if (rating >= 3.5) {
                favoriteBack = ratingBcgs[2];
            } else if (rating >= 2.1) {
                favoriteBack = ratingBcgs[1];
            } else if (rating > 0.0) {
                favoriteBack = ratingBcgs[0];
            }
        }
        holder.favourite.setBackgroundResource(favoriteBack);

        if (cacheListType == CacheListType.HISTORY && cache.getVisitedDate() > 0) {
            ArrayList<String> infos = new ArrayList<String>();
            infos.add(StringUtils.upperCase(cache.getGeocode()));
            infos.add(cgBase.formatDate(cache.getVisitedDate()));
            infos.add(cgBase.formatTime(cache.getVisitedDate()));
            holder.info.setText(StringUtils.join(infos, Formatter.SEPARATOR));
        } else {
            ArrayList<String> infos = new ArrayList<String>();
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
                infos.add(cgBase.formatShortDate(cache.getHiddenDate().getTime()));
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
        if (!gcIconDrawables.containsKey(hashCode)) {
            // fallback to mystery icon
            hashCode = getIconHashCode(CacheType.MYSTERY, cache.hasUserModifiedCoords() || cache.hasFinalDefined());
        }

        return gcIconDrawables.get(hashCode);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        checked = 0;
        for (cgCache cache : list) {
            if (cache.isStatusChecked()) {
                checked++;
            }
        }

        distances.clear();
        compasses.clear();
    }

    private class checkBoxListener implements View.OnClickListener {

        private cgCache cache = null;

        public checkBoxListener(cgCache cacheIn) {
            cache = cacheIn;
        }

        public void onClick(View view) {
            final boolean checkNow = ((CheckBox) view).isChecked();

            if (checkNow) {
                cache.setStatusChecked(true);
                checked++;
            } else {
                cache.setStatusChecked(false);
                checked--;
            }
        }
    }

    private class touchListener implements View.OnLongClickListener, View.OnClickListener, View.OnTouchListener {

        private String geocode = null;
        private String name = null;
        private cgCache cache = null;
        private boolean touch = true;
        private GestureDetector gestureDetector = null;

        public touchListener(String geocodeIn, String nameIn, cgCache cacheIn) {
            geocode = geocodeIn;
            name = nameIn;
            cache = cacheIn;

            final detectGesture dGesture = new detectGesture(holder, cache);
            gestureDetector = new GestureDetector(dGesture);
        }

        // tap on item
        public void onClick(View view) {
            if (!touch) {
                touch = true;
                return;
            }

            if (getSelectMode() || getChecked() > 0) {
                return;
            }

            // load cache details
            Intent cachesIntent = new Intent(getContext(), CacheDetailActivity.class);
            cachesIntent.putExtra("geocode", geocode);
            cachesIntent.putExtra("name", name);
            getContext().startActivity(cachesIntent);
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

    class detectGesture extends GestureDetector.SimpleOnGestureListener {

        private CacheListView holder = null;
        private cgCache cache = null;

        public detectGesture(CacheListView holderIn, cgCache cacheIn) {
            holder = holderIn;
            cache = cacheIn;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (getSelectMode()) {
                    return false;
                }

                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    // left to right swipe
                    if (cache.isStatusChecked()) {
                        return true;
                    }

                    if (holder != null && holder.oneInfo != null) {
                        checkChecked(+1);
                        holder.checkbox.setChecked(true);
                        cache.setStatusChecked(true);
                        moveRight(holder, cache, false);
                    }

                    return true;
                } else if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    // right to left swipe
                    if (!cache.isStatusChecked()) {
                        return true;
                    }

                    if (holder != null && holder.oneInfo != null) {
                        if (getSelectMode()) {
                            setSelectMode(false, false);
                        }

                        checkChecked(-1);
                        holder.checkbox.setChecked(false);
                        cache.setStatusChecked(false);
                        moveLeft(holder, cache, false);
                    }

                    return true;
                }
            } catch (Exception e) {
                Log.w("CacheListAdapter.detectGesture.onFling: " + e.toString());
            }

            return false;
        }
    }

    private void checkChecked(int cnt) {
        // check how many caches are selected, if any block sorting of list
        checked += cnt;
        sort = !(checked > 0 || getSelectMode());

        if (sort) {
            forceSort(coords);
        }
    }

    private void moveRight(CacheListView holder, cgCache cache, boolean force) {
        if (cache == null) {
            return;
        }

        holder.checkbox.setChecked(cache.isStatusChecked());

        // slide cache info
        final Animation showCheckbox = new TranslateAnimation(0, (int) (SWIPE_DISTANCE * pixelDensity), 0, 0);
        showCheckbox.setRepeatCount(0);
        showCheckbox.setDuration(force ? 0 : 400);
        showCheckbox.setFillEnabled(true);
        showCheckbox.setFillAfter(true);
        showCheckbox.setInterpolator(new AccelerateDecelerateInterpolator());

        // dim cache info
        final Animation dimInfo = new AlphaAnimation(1.0f, SWIPE_OPACITY);
        dimInfo.setRepeatCount(0);
        dimInfo.setDuration(force ? 0 : 400);
        dimInfo.setFillEnabled(true);
        dimInfo.setFillAfter(true);
        dimInfo.setInterpolator(new AccelerateDecelerateInterpolator());

        // animation set (container)
        final AnimationSet selectAnimation = new AnimationSet(true);
        selectAnimation.setFillEnabled(true);
        selectAnimation.setFillAfter(true);

        selectAnimation.addAnimation(showCheckbox);
        selectAnimation.addAnimation(dimInfo);

        holder.oneInfo.startAnimation(selectAnimation);
        cache.setStatusCheckedView(true);
    }

    private void moveLeft(CacheListView holder, cgCache cache, boolean force) {
        if (cache == null) {
            return;
        }

        holder.checkbox.setChecked(cache.isStatusChecked());

        // slide cache info
        final Animation hideCheckbox = new TranslateAnimation((int) (SWIPE_DISTANCE * pixelDensity), 0, 0, 0);
        hideCheckbox.setRepeatCount(0);
        hideCheckbox.setDuration(force ? 0 : 400);
        hideCheckbox.setFillEnabled(true);
        hideCheckbox.setFillAfter(true);
        hideCheckbox.setInterpolator(new AccelerateDecelerateInterpolator());

        // brighten cache info
        final Animation brightenInfo = new AlphaAnimation(SWIPE_OPACITY, 1.0f);
        brightenInfo.setRepeatCount(0);
        brightenInfo.setDuration(force ? 0 : 400);
        brightenInfo.setFillEnabled(true);
        brightenInfo.setFillAfter(true);
        brightenInfo.setInterpolator(new AccelerateDecelerateInterpolator());

        // animation set (container)
        final AnimationSet selectAnimation = new AnimationSet(true);
        selectAnimation.setFillEnabled(true);
        selectAnimation.setFillAfter(true);

        selectAnimation.addAnimation(hideCheckbox);
        selectAnimation.addAnimation(brightenInfo);

        holder.oneInfo.startAnimation(selectAnimation);
        cache.setStatusCheckedView(false);
    }

    public List<cgCache> getFilteredList() {
        return list;
    }
}
