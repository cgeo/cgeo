package cgeo.geocaching.activity;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchActivity;
import cgeo.geocaching.databinding.ActivityBottomNavigationBinding;
import cgeo.geocaching.maps.DefaultMap;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.IdRes;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;

public class BottomNavigationController {
    private final ActivityBottomNavigationBinding wrapper;
    private final Activity activity;

    public static final @IdRes int MENU_MAP = R.id.page_map;
    public static final @IdRes int MENU_LIST = R.id.page_list;
    public static final @IdRes int MENU_SEARCH = R.id.page_search;
    public static final @IdRes int MENU_NEARBY = R.id.page_nearby;
    public static final @IdRes int MENU_MORE = R.id.page_more;

    public BottomNavigationController(final Activity activity, final @IdRes int menuId, final View contentView) {
        this.activity = activity;

        wrapper = ActivityBottomNavigationBinding.inflate(activity.getLayoutInflater());
        wrapper.activityContent.addView(contentView);

        wrapper.activityBottomNavigation.setSelectedItemId(menuId);
        wrapper.activityBottomNavigation.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);

        updateCacheCounter();

        wrapper.activityBottomNavigation.setOnNavigationItemSelectedListener(item -> {
            final int id = item.getItemId();

            if (id == menuId) {
                return false; // do nothing if the item is already selected
            }

            if (id == MENU_MAP) {
                activity.startActivity(DefaultMap.getLiveMapIntent(activity));
            } else if (id == MENU_LIST) {
                CacheListActivity.startActivityOffline(activity);
            } else if (id == MENU_SEARCH) {
                activity.startActivity(new Intent(activity, SearchActivity.class));
            } else if (id == MENU_NEARBY) {
                activity.startActivity(CacheListActivity.getNearestIntent(activity));
            } else if (id == MENU_MORE) {
                activity.startActivity(new Intent(activity, MainActivity.class));
            } else {
                throw new IllegalStateException("unknown navigation item selected"); // should never happen
            }
            // avoid weired transitions
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            activity.finish();
            return true;
        });

    }

    public View getView() {
        return wrapper.getRoot();
    }

    private void updateCacheCounter() {
        AndroidRxUtils.bindActivity(activity, DataStore.getAllCachesCountObservable()).subscribe(count -> {
            if (count != 0) {
                final BadgeDrawable badge = wrapper.activityBottomNavigation.getOrCreateBadge(MENU_LIST);
                badge.setNumber(count);
            } else {
                wrapper.activityBottomNavigation.removeBadge(MENU_LIST);
            }
        }, throwable -> Log.e("Unable to add bubble count", throwable));
    }
}
