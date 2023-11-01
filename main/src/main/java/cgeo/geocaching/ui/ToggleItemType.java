package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.view.MenuItem;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;


/**
 * Encapsulates the mechanism to toggle text and image.
 *
 * Supports this for menu-item.
 *
 * Class is supposed to be used in Activity to toggle ui-elements.
 */
public enum ToggleItemType {
    FOLLOW_MY_LOCATION(R.drawable.ic_menu_mylocation, R.drawable.ic_menu_mylocation_off, -1, -1),
    LIVE_MODE(R.drawable.ic_menu_sync_enabled, R.drawable.ic_menu_sync_disabled, R.string.map_live_disable, R.string.map_live_enable),
    SELECT_MODE(R.drawable.ic_menu_select_end, R.drawable.ic_menu_select_start,
            R.string.caches_select_mode_exit, R.string.caches_select_mode),
    TOGGLE_SPEECH(R.drawable.ic_menu_text_to_speech_on, R.drawable.ic_menu_text_to_speech_off,
            R.string.cache_menu_speechDeactivate, R.string.cache_menu_speechActivate),
    WAYPOINTS_FROM_NOTE(-1, -1, R.string.cache_menu_allowWaypointExtraction, R.string.cache_menu_preventWaypointsFromNote);

    @DrawableRes
    private final int drawableActiveId;
    @DrawableRes
    private final int drawableInactiveId;
    @StringRes
    private final int stringActiveId;
    @StringRes
    private final int stringInactiveId;

    ToggleItemType(@DrawableRes final int drawableActiveId, @DrawableRes final int drawableInactiveId,
                   @StringRes final int stringActiveId, @StringRes final int stringInactiveId) {
        this.drawableActiveId = drawableActiveId;
        this.drawableInactiveId = drawableInactiveId;
        this.stringActiveId = stringActiveId;
        this.stringInactiveId = stringInactiveId;
    }

    public void toggleMenuItem(@Nullable final MenuItem menuItem, final boolean active) {
        if (null != menuItem) {
            if (-1 != stringActiveId && -1 != stringInactiveId) {
                menuItem.setTitle(active ? stringActiveId : stringInactiveId);
            }
            if (-1 != drawableActiveId && -1 != drawableInactiveId) {
                menuItem.setIcon(active ? drawableActiveId : drawableInactiveId);
            }
        }
    }
}
