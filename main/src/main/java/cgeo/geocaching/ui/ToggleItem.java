package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.view.MenuItem;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;


/**
 * Encapsulates the mechanism to toggle text and image.
 *
 * Supports this for menu-item.
 *
 * Class is supposed to be used in Activity to toggle ui-elements.
 */
public class ToggleItem {
    public static ToggleItem toggleSpeech = new ToggleItem(R.drawable.ic_menu_text_to_speech_on, R.drawable.ic_menu_text_to_speech_off,
            R.string.cache_menu_speechDeactivate, R.string.cache_menu_speechActivate);

    @DrawableRes
    private final int drawableActiveId;
    @DrawableRes
    private final int drawableInactiveId;
    @StringRes
    private final int stringActiveId;
    @StringRes
    private final int stringInactiveId;

    public ToggleItem(@DrawableRes final int drawableActiveId, @DrawableRes final int drawableInactiveId,
                      @StringRes final int stringActiveId, @StringRes final int stringInactiveId) {
        this.drawableActiveId = drawableActiveId;
        this.drawableInactiveId = drawableInactiveId;
        this.stringActiveId = stringActiveId;
        this.stringInactiveId = stringInactiveId;
    }

    public void toggleMenuItem(final MenuItem menuItem, final boolean active) {
        if (null != menuItem) {
            menuItem.setTitle(active ? stringActiveId : stringInactiveId);
            menuItem.setIcon(active ? drawableActiveId : drawableInactiveId);
        }
    }
}


