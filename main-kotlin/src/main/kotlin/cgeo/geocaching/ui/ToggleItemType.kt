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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import android.view.MenuItem

import androidx.annotation.DrawableRes
import androidx.annotation.Nullable
import androidx.annotation.StringRes


/**
 * Encapsulates the mechanism to toggle text and image.
 * <br>
 * Supports this for menu-item.
 * <br>
 * Class is supposed to be used in Activity to toggle ui-elements.
 */
enum class class ToggleItemType {
    FOLLOW_MY_LOCATION(R.drawable.ic_menu_mylocation, R.drawable.ic_menu_mylocation_off, -1, -1, -1, -1),
    LIVE_MODE(R.drawable.ic_menu_sync_enabled, R.drawable.ic_menu_sync_disabled, R.string.map_live_disable, R.string.map_live_enable, R.string.map_live_disable, R.string.map_live_enable),
    SELECT_MODE(R.drawable.ic_menu_select_end, R.drawable.ic_menu_select_start,
            R.string.caches_select_mode_exit, R.string.caches_select_mode, R.string.caches_select_mode_exit, R.string.caches_select_mode),
    TOGGLE_SPEECH(R.drawable.ic_menu_text_to_speech_on, R.drawable.ic_menu_text_to_speech_off,
            R.string.cache_menu_speechDeactivate, R.string.cache_menu_speechActivate, R.string.talking_enabled, R.string.talking_disabled),
    WAYPOINTS_FROM_NOTE(-1, -1, R.string.cache_menu_allowWaypointExtraction, R.string.cache_menu_preventWaypointsFromNote,
            R.string.cache_menu_allowWaypointExtraction, R.string.cache_menu_preventWaypointsFromNote)

    @DrawableRes
    private final Int drawableActiveId
    @DrawableRes
    private final Int drawableInactiveId
    @StringRes
    private final Int stringActiveId
    @StringRes
    private final Int stringInactiveId
    @StringRes
    private final Int hintActiveId
    @StringRes
    private final Int hintInactiveId

    ToggleItemType(@DrawableRes final Int drawableActiveId, @DrawableRes final Int drawableInactiveId,
                   @StringRes final Int stringActiveId, @StringRes final Int stringInactiveId,
                   @StringRes final Int hintActiveId, @StringRes final Int hintInactiveId) {
        this.drawableActiveId = drawableActiveId
        this.drawableInactiveId = drawableInactiveId
        this.stringActiveId = stringActiveId
        this.stringInactiveId = stringInactiveId
        this.hintActiveId = hintActiveId
        this.hintInactiveId = hintInactiveId
    }

    public Unit toggleMenuItem(final MenuItem menuItem, final Boolean active) {
        if (null != menuItem) {
            if (-1 != stringActiveId && -1 != stringInactiveId) {
                menuItem.setTitle(active ? stringActiveId : stringInactiveId)
            }
            if (-1 != drawableActiveId && -1 != drawableInactiveId) {
                menuItem.setIcon(active ? drawableActiveId : drawableInactiveId)
            }
            if (-1 != hintActiveId && -1 != hintInactiveId) {
                menuItem.setTooltipText(CgeoApplication.getInstance().getString(active ? hintActiveId : hintInactiveId))
            }
        }
    }
}
