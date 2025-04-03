package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.os.Build;
import android.view.MenuItem;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;


/**
 * Encapsulates the mechanism to toggle text and image.
 * <br>
 * Supports this for menu-item.
 * <br>
 * Class is supposed to be used in Activity to toggle ui-elements.
 */
public enum ToggleItemType {
    FOLLOW_MY_LOCATION(R.drawable.ic_menu_mylocation, R.drawable.ic_menu_mylocation_off, -1, -1, -1, -1),
    LIVE_MODE(R.drawable.ic_menu_sync_enabled, R.drawable.ic_menu_sync_disabled, R.string.map_live_disable, R.string.map_live_enable, R.string.map_live_disable, R.string.map_live_enable),
    SELECT_MODE(R.drawable.ic_menu_select_end, R.drawable.ic_menu_select_start,
            R.string.caches_select_mode_exit, R.string.caches_select_mode, R.string.caches_select_mode_exit, R.string.caches_select_mode),
    TOGGLE_SPEECH(R.drawable.ic_menu_text_to_speech_on, R.drawable.ic_menu_text_to_speech_off,
            R.string.cache_menu_speechDeactivate, R.string.cache_menu_speechActivate, R.string.talking_enabled, R.string.talking_disabled),
    WAYPOINTS_FROM_NOTE(-1, -1, R.string.cache_menu_allowWaypointExtraction, R.string.cache_menu_preventWaypointsFromNote,
            R.string.cache_menu_allowWaypointExtraction, R.string.cache_menu_preventWaypointsFromNote);

    @DrawableRes
    private final int drawableActiveId;
    @DrawableRes
    private final int drawableInactiveId;
    @StringRes
    private final int stringActiveId;
    @StringRes
    private final int stringInactiveId;
    @StringRes
    private final int hintActiveId;
    @StringRes
    private final int hintInactiveId;

    ToggleItemType(@DrawableRes final int drawableActiveId, @DrawableRes final int drawableInactiveId,
                   @StringRes final int stringActiveId, @StringRes final int stringInactiveId,
                   @StringRes final int hintActiveId, @StringRes final int hintInactiveId) {
        this.drawableActiveId = drawableActiveId;
        this.drawableInactiveId = drawableInactiveId;
        this.stringActiveId = stringActiveId;
        this.stringInactiveId = stringInactiveId;
        this.hintActiveId = hintActiveId;
        this.hintInactiveId = hintInactiveId;
    }

    public void toggleMenuItem(@Nullable final MenuItem menuItem, final boolean active) {
        if (null != menuItem) {
            if (-1 != stringActiveId && -1 != stringInactiveId) {
                menuItem.setTitle(active ? stringActiveId : stringInactiveId);
            }
            if (-1 != drawableActiveId && -1 != drawableInactiveId) {
                menuItem.setIcon(active ? drawableActiveId : drawableInactiveId);
            }
            if (-1 != hintActiveId && -1 != hintInactiveId && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menuItem.setTooltipText(CgeoApplication.getInstance().getString(active ? hintActiveId : hintInactiveId));
            }
        }
    }
}
