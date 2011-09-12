package cgeo.geocaching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.style.StrikethroughSpan;
import android.util.DisplayMetrics;
import android.util.Log;
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
import cgeo.geocaching.filter.cgFilter;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.sorting.CacheComparator;
import cgeo.geocaching.sorting.DistanceComparator;
import cgeo.geocaching.sorting.VisitComparator;
import cgeo.geocaching.utils.CollectionUtils;

public class cgCacheListAdapter extends ArrayAdapter<cgCache> {

	private Resources res = null;
	private List<cgCache> list = null;
	private cgSettings settings = null;
	private cgCacheView holder = null;
	private LayoutInflater inflater = null;
	private Activity activity = null;
	private cgBase base = null;
	private CacheComparator statComparator = null;
	private boolean historic = false;
	private Geopoint coords = null;
	private Double azimuth = Double.valueOf(0);
	private long lastSort = 0L;
	private boolean sort = true;
	private int checked = 0;
	private boolean selectMode = false;
	private static Map<String, Drawable> gcIconDrawables = new HashMap<String, Drawable>();
	private List<cgCompassMini> compasses = new ArrayList<cgCompassMini>();
	private List<cgDistanceView> distances = new ArrayList<cgDistanceView>();
	private int[] ratingBcgs = new int[3];
	private float pixelDensity = 1f;
	private static final int SWIPE_MIN_DISTANCE = 60;
	private static final int SWIPE_MAX_OFF_PATH = 100;
	private static final int SWIPE_DISTANCE = 80;
	private static final float SWIPE_OPACITY = 0.5f;
	private cgFilter currentFilter = null;
	private List<cgCache> originalList = null;

	public cgCacheListAdapter(Activity activityIn, cgSettings settingsIn, List<cgCache> listIn, cgBase baseIn) {
		super(activityIn, 0, listIn);

		res = activityIn.getResources();
		activity = activityIn;
		settings = settingsIn;
		list = listIn;
		base = baseIn;

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		pixelDensity = metrics.density;

		if (gcIconDrawables == null || gcIconDrawables.isEmpty()) {
			for (String cacheType : cgBase.cacheTypesInv.keySet()) {
				gcIconDrawables.put(cacheType, (Drawable) activity.getResources().getDrawable(cgBase.getCacheIcon(cacheType)));
			}
		}

		if (settings.skin == 0) {
			ratingBcgs[0] = R.drawable.favourite_background_red_dark;
			ratingBcgs[1] = R.drawable.favourite_background_orange_dark;
			ratingBcgs[2] = R.drawable.favourite_background_green_dark;
		} else {
			ratingBcgs[0] = R.drawable.favourite_background_red_light;
			ratingBcgs[1] = R.drawable.favourite_background_orange_light;
			ratingBcgs[2] = R.drawable.favourite_background_green_light;
		}
	}

	public void setComparator(CacheComparator comparator) {
		statComparator = comparator;

		forceSort(coords);
	}

	/**
	 * Called when a new page of caches was loaded.
	 */
	public void reFilter(){
		if(currentFilter != null){
			// Back up the list again
			originalList = new ArrayList<cgCache>(list);

			currentFilter.filter(list);
		}
	}

	/**
	 * Called after a user action on the filter menu.
	 */
	public void setFilter(cgFilter filter){
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

	public void clearFilter() {
		if (originalList != null) {
			list.clear();
			list.addAll(originalList);

			currentFilter = null;
		}

		notifyDataSetChanged();
	}

	public boolean isFilter() {
		if (currentFilter != null) {
			return true;
		} else {
			return false;
		}
	}

	public void setHistoric(boolean historicIn) {
		historic = historicIn;

		if (historic) {
			statComparator = new VisitComparator();
		} else {
			statComparator = null;
		}
	}

	public int getChecked() {
		return checked;
	}

	public boolean setSelectMode(boolean status, boolean clear) {
		selectMode = status;

		if (selectMode == false && clear) {
			for (cgCache cache : list) {
				cache.statusChecked = false;
				cache.statusCheckedView = false;
			}
			checked = 0;
		} else if (selectMode) {
			for (cgCache cache : list) {
				cache.statusCheckedView = false;
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

		if (selectMode == false) {
			for (cgCache cache : list) {
				cache.statusChecked = false;
				cache.statusCheckedView = false;
			}
			checked = 0;
		} else if (selectMode) {
			for (cgCache cache : list) {
				cache.statusCheckedView = false;
			}
		}
		checkChecked(0);

		notifyDataSetChanged();
	}

	public void invertSelection() {
		int check = 0;

		for (cgCache cache : list) {
			if (cache.statusChecked) {
				cache.statusChecked = false;
				cache.statusCheckedView = false;
			} else {
				cache.statusChecked = true;
				cache.statusCheckedView = true;

				check++;
			}
		}
		checkChecked(check);

		notifyDataSetChanged();
	}

	public void forceSort(final Geopoint coordsIn) {
		if (list == null || list.isEmpty()) {
			return;
		}
		if (sort == false) {
			return;
		}

		try {
			if (statComparator != null) {
				Collections.sort((List<cgCache>) list, statComparator);
			} else {
				if (coordsIn == null) {
					return;
				}

				final DistanceComparator dstComparator = new DistanceComparator(coordsIn);
				Collections.sort((List<cgCache>) list, dstComparator);
			}
			notifyDataSetChanged();
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgCacheListAdapter.setActualCoordinates: failed to sort caches in list");
		}
	}

	public void setActualCoordinates(final Geopoint coordsIn) {
		if (coordsIn == null) {
			return;
		}

		coords = coordsIn;

		if (list != null && list.isEmpty() == false && (System.currentTimeMillis() - lastSort) > 1000 && sort) {
			try {
				if (statComparator != null) {
					Collections.sort((List<cgCache>) list, statComparator);
				} else {
					final DistanceComparator dstComparator = new DistanceComparator(coordsIn);
					Collections.sort((List<cgCache>) list, dstComparator);
				}
				notifyDataSetChanged();
			} catch (Exception e) {
				Log.w(cgSettings.tag, "cgCacheListAdapter.setActualCoordinates: failed to sort caches in list");
			}

			lastSort = System.currentTimeMillis();
		}

		if (CollectionUtils.isNotEmpty(distances)) {
			for (cgDistanceView distance : distances) {
				distance.update(coordsIn);
			}
		}

		if (CollectionUtils.isNotEmpty(compasses)) {
			for (cgCompassMini compass : compasses) {
				compass.updateCoords(coordsIn);
			}
		}
	}

	public void setActualHeading(Double azimuthIn) {
		if (azimuthIn == null) {
			return;
		}

		azimuth = azimuthIn;

		if (CollectionUtils.isNotEmpty(compasses)) {
			for (cgCompassMini compass : compasses) {
				compass.updateAzimuth(azimuth);
			}
		}
	}

	/**
	 * clear all check marks
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
			if (cache.statusChecked) {
				cache.statusChecked = false;

				checkChecked(-1);
				cleared++;
			}
		}
		setSelectMode(false, false);
		notifyDataSetChanged();

		if (cleared > 0 || status) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public View getView(int position, View rowView, ViewGroup parent) {
		if (inflater == null) {
			inflater = ((Activity) getContext()).getLayoutInflater();
		}

		if (position > getCount()) {
			Log.w(cgSettings.tag, "cgCacheListAdapter.getView: Attempt to access missing item #" + position);
			return null;
		}

		cgCache cache = getItem(position);

		if (rowView == null) {
			rowView = (View) inflater.inflate(R.layout.cache, null);

			holder = new cgCacheView();
			holder.oneCache = (RelativeLayout) rowView.findViewById(R.id.one_cache);
			holder.checkbox = (CheckBox) rowView.findViewById(R.id.checkbox);
			holder.oneInfo = (RelativeLayout) rowView.findViewById(R.id.one_info);
			holder.oneCheckbox = (RelativeLayout) rowView.findViewById(R.id.one_checkbox);
			holder.logStatusMark = (ImageView) rowView.findViewById(R.id.log_status_mark);
			holder.oneCache = (RelativeLayout) rowView.findViewById(R.id.one_cache);
			holder.text = (TextView) rowView.findViewById(R.id.text);
			holder.directionLayout = (RelativeLayout) rowView.findViewById(R.id.direction_layout);
			holder.distance = (cgDistanceView) rowView.findViewById(R.id.distance);
			holder.direction = (cgCompassMini) rowView.findViewById(R.id.direction);
			holder.dirImgLayout = (RelativeLayout) rowView.findViewById(R.id.dirimg_layout);
			holder.dirImg = (ImageView) rowView.findViewById(R.id.dirimg);
			holder.inventory = (RelativeLayout) rowView.findViewById(R.id.inventory);
			holder.favourite = (TextView) rowView.findViewById(R.id.favourite);
			holder.info = (TextView) rowView.findViewById(R.id.info);

			rowView.setTag(holder);
		} else {
			holder = (cgCacheView) rowView.getTag();
		}

		if (cache.own) {
			if (settings.skin == 1) {
				holder.oneInfo.setBackgroundResource(R.color.owncache_background_light);
				holder.oneCheckbox.setBackgroundResource(R.color.owncache_background_light);
			} else {
				holder.oneInfo.setBackgroundResource(R.color.owncache_background_dark);
				holder.oneCheckbox.setBackgroundResource(R.color.owncache_background_dark);
			}
		} else {
			if (settings.skin == 1) {
				holder.oneInfo.setBackgroundResource(R.color.background_light);
				holder.oneCheckbox.setBackgroundResource(R.color.background_light);
			} else {
				holder.oneInfo.setBackgroundResource(R.color.background_dark);
				holder.oneCheckbox.setBackgroundResource(R.color.background_dark);
			}
		}

		final touchListener touchLst = new touchListener(cache.geocode, cache.name, cache);
		rowView.setOnClickListener(touchLst);
		rowView.setOnLongClickListener(touchLst);
		rowView.setOnTouchListener(touchLst);
		rowView.setLongClickable(true);

		if (selectMode) {
			if (cache.statusCheckedView) {
				moveRight(holder, cache, true); // move fast when already slided
			} else {
				moveRight(holder, cache, false);
			}
		} else if (cache.statusChecked) {
			holder.checkbox.setChecked(true);
			if (cache.statusCheckedView) {
				moveRight(holder, cache, true); // move fast when already slided
			} else {
				moveRight(holder, cache, false);
			}
		} else {
			holder.checkbox.setChecked(false);
			if (cache.statusCheckedView == false) {
				holder.oneInfo.clearAnimation();
			} else {
				moveLeft(holder, cache, false);
			}
		}

		holder.checkbox.setOnClickListener(new checkBoxListener(cache));

		if (distances.contains(holder.distance) == false) {
			distances.add(holder.distance);
		}
		holder.distance.setContent(base, cache.coords);
		if (compasses.contains(holder.direction) == false) {
			compasses.add(holder.direction);
		}
		holder.direction.setContent(cache.coords);

		if (cache.found && cache.logOffline) {
		    holder.logStatusMark.setImageResource(R.drawable.mark_green_red);
		    holder.logStatusMark.setVisibility(View.VISIBLE);
		} else if (cache.found) {
		    holder.logStatusMark.setImageResource(R.drawable.mark_green);
		    holder.logStatusMark.setVisibility(View.VISIBLE);
		} else if (cache.logOffline) {
            holder.logStatusMark.setImageResource(R.drawable.mark_red);
		    holder.logStatusMark.setVisibility(View.VISIBLE);
        } else {
		    holder.logStatusMark.setVisibility(View.GONE);
		}

		if (cache.nameSp == null) {
			cache.nameSp = (new Spannable.Factory()).newSpannable(cache.name);
			if (cache.disabled || cache.archived) { // strike
				cache.nameSp.setSpan(new StrikethroughSpan(), 0, cache.nameSp.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		holder.text.setText(cache.nameSp, TextView.BufferType.SPANNABLE);
		if (gcIconDrawables.containsKey(cache.type)) { // cache icon
			holder.text.setCompoundDrawablesWithIntrinsicBounds(gcIconDrawables.get(cache.type), null, null, null);
		} else { // unknown cache type, "mystery" icon
			holder.text.setCompoundDrawablesWithIntrinsicBounds(gcIconDrawables.get("mystery"), null, null, null);
		}

		if (holder.inventory.getChildCount() > 0) {
			holder.inventory.removeAllViews();
		}

		ImageView tbIcon = null;
		if (cache.inventoryItems > 0) {
			tbIcon = (ImageView) inflater.inflate(R.layout.trackable_icon, null);
			tbIcon.setImageResource(R.drawable.trackable_all);

			holder.inventory.addView(tbIcon);
			holder.inventory.setVisibility(View.VISIBLE);
		} else {
			holder.inventory.setVisibility(View.GONE);
		}

		boolean setDiDi = false;
		if (cache.coords != null) {
			holder.direction.setVisibility(View.VISIBLE);
			holder.direction.updateAzimuth(azimuth);
			if (coords != null) {
				holder.distance.update(coords);
				holder.direction.updateCoords(coords);
			}
			setDiDi = true;
		} else {
			if (cache.distance != null) {
				holder.distance.setDistance(cache.distance);
				setDiDi = true;
			}
			if (cache.direction != null) {
				holder.direction.setVisibility(View.VISIBLE);
				holder.direction.updateAzimuth(azimuth);
				holder.direction.updateHeading(cache.direction);
				setDiDi = true;
			}
		}

		if (setDiDi) {
			holder.directionLayout.setVisibility(View.VISIBLE);
			holder.dirImgLayout.setVisibility(View.GONE);
		} else {
			holder.directionLayout.setVisibility(View.GONE);
			holder.distance.clear();

			Bitmap dirImgPre = null;
			Bitmap dirImg = null;
			try {
				dirImgPre = BitmapFactory.decodeFile(cgSettings.getStorage() + cache.geocode + "/direction.png");
				dirImg = dirImgPre.copy(Bitmap.Config.ARGB_8888, true);

				dirImgPre.recycle();
				dirImgPre = null;
			} catch (Exception e) {
				// nothing
			}

			if (dirImg != null) {
				if (settings.skin == 0) {
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

		if (cache.favouriteCnt != null) {
			holder.favourite.setText(String.format("%d", cache.favouriteCnt));
		} else {
			holder.favourite.setText("---");
		}

		int favoriteBack;
		// set default background, neither vote nor rating may be available
		if (settings.skin == 1) {
			favoriteBack = R.drawable.favourite_background_light;
		} else {
			favoriteBack = R.drawable.favourite_background_dark;
		}
		if (cache.myVote != null && cache.myVote > 0) {
			if (cache.myVote >= 4) {
				favoriteBack = ratingBcgs[2];
			} else if (cache.myVote >= 3) {
				favoriteBack = ratingBcgs[1];
			} else if (cache.myVote > 0) {
				favoriteBack = ratingBcgs[0];
			}
		} else if (cache.rating != null && cache.rating > 0) {
			if (cache.rating >= 3.5) {
				favoriteBack = ratingBcgs[2];
			} else if (cache.rating >= 2.1) {
				favoriteBack = ratingBcgs[1];
			} else if (cache.rating > 0.0) {
				favoriteBack = ratingBcgs[0];
			}
		}
		holder.favourite.setBackgroundResource(favoriteBack);

		StringBuilder cacheInfo = new StringBuilder();
		if (historic && cache.visitedDate != null) {
			cacheInfo.append(base.formatTime(cache.visitedDate));
			cacheInfo.append("; ");
			cacheInfo.append(base.formatDate(cache.visitedDate));
		} else {
			if (StringUtils.isNotBlank(cache.geocode)) {
				cacheInfo.append(cache.geocode);
			}
			if (StringUtils.isNotBlank(cache.size)) {
				if (cacheInfo.length() > 0) {
					cacheInfo.append(" | ");
				}
				cacheInfo.append(cache.size);
			}
			if ((cache.difficulty != null && cache.difficulty > 0f) || (cache.terrain != null && cache.terrain > 0f) || (cache.rating != null && cache.rating > 0f)) {
				if (cacheInfo.length() > 0 && ((cache.difficulty != null && cache.difficulty > 0f) || (cache.terrain != null && cache.terrain > 0f))) {
					cacheInfo.append(" |");
				}

				if (cache.difficulty != null && cache.difficulty > 0f) {
					cacheInfo.append(" D:");
					cacheInfo.append(String.format(Locale.getDefault(), "%.1f", cache.difficulty));
				}
				if (cache.terrain != null && cache.terrain > 0f) {
					cacheInfo.append(" T:");
					cacheInfo.append(String.format(Locale.getDefault(), "%.1f", cache.terrain));
				}
			}
			if (cache.members) {
				if (cacheInfo.length() > 0) {
					cacheInfo.append(" | ");
				}
				cacheInfo.append(res.getString(R.string.cache_premium));
			}
			if (cache.reason != null && cache.reason == 1) {
				if (cacheInfo.length() > 0) {
					cacheInfo.append(" | ");
				}
				cacheInfo.append(res.getString(R.string.cache_offline));
			}
		}
		holder.info.setText(cacheInfo.toString());

		return rowView;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();

		checked = 0;
		for (cgCache cache : list) {
			if (cache.statusChecked) {
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
				cache.statusChecked = true;
				checked++;
			} else {
				cache.statusChecked = false;
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
			if (touch == false) {
				touch = true;

				return;
			}

			if (getSelectMode() || getChecked() > 0) {
				return;
			}

			// load cache details
			Intent cachesIntent = new Intent(getContext(), cgeodetail.class);
			cachesIntent.putExtra("geocode", geocode);
			cachesIntent.putExtra("name", name);
			getContext().startActivity(cachesIntent);
		}

		// long tap on item
		public boolean onLongClick(View view) {
			if (touch == false) {
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

		private cgCacheView holder = null;
		private cgCache cache = null;

		public detectGesture(cgCacheView holderIn, cgCache cacheIn) {
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
					if (cache.statusChecked) {
						return true;
					}

					if (holder != null && holder.oneInfo != null) {
						checkChecked(+1);
						holder.checkbox.setChecked(true);
						cache.statusChecked = true;
						moveRight(holder, cache, false);
					}

					return true;
				} else if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > Math.abs(velocityY)) {
					// right to left swipe
					if (cache.statusChecked == false) {
						return true;
					}

					if (holder != null && holder.oneInfo != null) {
						if (getSelectMode()) {
							setSelectMode(false, false);
						}

						checkChecked(-1);
						holder.checkbox.setChecked(false);
						cache.statusChecked = false;
						moveLeft(holder, cache, false);
					}

					return true;
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "cgCacheListAdapter.detectGesture.onFling: " + e.toString());
			}

			return false;
		}
	}

	private void checkChecked(int cnt) {
		// check how many caches are selected, if any block sorting of list
		boolean statusChecked = false;
		boolean statusSort = false;
		checked += cnt;

		if (checked > 0) {
			statusChecked = false;
		} else {
			statusChecked = true;
		}

		if (getSelectMode()) {
			statusSort = false;
		} else {
			statusSort = true;
		}

		if (statusChecked == false || statusSort == false) {
			sort = false;
		} else {
			sort = true;
		}

		if (sort) {
			forceSort(coords);
		}
	}

	private void moveRight(cgCacheView holder, cgCache cache, boolean force) {
		if (cache == null) {
			return;
		}

		try {
			holder.checkbox.setChecked(cache.statusChecked);

			// slide cache info
			Animation showCheckbox = new TranslateAnimation(0, (int) (SWIPE_DISTANCE * pixelDensity), 0, 0);
			showCheckbox.setRepeatCount(0);
			if (force) {
				showCheckbox.setDuration(0);
			} else {
				showCheckbox.setDuration(400);
			}
			showCheckbox.setFillEnabled(true);
			showCheckbox.setFillAfter(true);
			showCheckbox.setInterpolator(new AccelerateDecelerateInterpolator());

			// dim cache info
			Animation dimInfo = new AlphaAnimation(1.0f, SWIPE_OPACITY);
			dimInfo.setRepeatCount(0);
			if (force) {
				dimInfo.setDuration(0);
			} else {
				dimInfo.setDuration(400);
			}
			dimInfo.setFillEnabled(true);
			dimInfo.setFillAfter(true);
			dimInfo.setInterpolator(new AccelerateDecelerateInterpolator());

			// animation set (container)
			AnimationSet selectAnimation = new AnimationSet(true);
			selectAnimation.setFillEnabled(true);
			selectAnimation.setFillAfter(true);

			selectAnimation.addAnimation(showCheckbox);
			selectAnimation.addAnimation(dimInfo);

			holder.oneInfo.startAnimation(selectAnimation);
			cache.statusCheckedView = true;
		} catch (Exception e) {
			// nothing
		}
	}

	private void moveLeft(cgCacheView holder, cgCache cache, boolean force) {
		if (cache == null) {
			return;
		}

		try {
			holder.checkbox.setChecked(cache.statusChecked);

			// slide cache info
			Animation hideCheckbox = new TranslateAnimation((int) (SWIPE_DISTANCE * pixelDensity), 0, 0, 0);
			hideCheckbox.setRepeatCount(0);
			if (force) {
				hideCheckbox.setDuration(0);
			} else {
				hideCheckbox.setDuration(400);
			}
			hideCheckbox.setFillEnabled(true);
			hideCheckbox.setFillAfter(true);
			hideCheckbox.setInterpolator(new AccelerateDecelerateInterpolator());

			// brighten cache info
			Animation brightenInfo = new AlphaAnimation(SWIPE_OPACITY, 1.0f);
			brightenInfo.setRepeatCount(0);
			if (force) {
				brightenInfo.setDuration(0);
			} else {
				brightenInfo.setDuration(400);
			}
			brightenInfo.setFillEnabled(true);
			brightenInfo.setFillAfter(true);
			brightenInfo.setInterpolator(new AccelerateDecelerateInterpolator());

			// animation set (container)
			AnimationSet selectAnimation = new AnimationSet(true);
			selectAnimation.setFillEnabled(true);
			selectAnimation.setFillAfter(true);

			selectAnimation.addAnimation(hideCheckbox);
			selectAnimation.addAnimation(brightenInfo);

			holder.oneInfo.startAnimation(selectAnimation);
			cache.statusCheckedView = false;
		} catch (Exception e) {
			// nothing
		}
	}
}
