package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;

import org.apache.commons.lang3.StringUtils;

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
public class AbstractActionBarActivity extends AbstractActivity {

    private static final int ACTION_BAR_SYSTEM_BAR_OVERLAP_HEIGHT_MIN = 50; //dp

    private int actionBarSystemBarOverlapHeight = ViewUtils.dpToPixel(ACTION_BAR_SYSTEM_BAR_OVERLAP_HEIGHT_MIN);
    private boolean fixedActionBar = true;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUpAction();
    }

    private void initUpAction() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // don't display a back button it the activity is running as top-level activity with bottom navigation attached
            actionBar.setDisplayHomeAsUpEnabled(!isTaskRoot() || !(this instanceof AbstractNavigationBarActivity));
        }

        final View actionBarView = getActionBarView();
        if (actionBarView != null) {
            // set action bar background color, otherwise it would be transparent
            actionBarView.setBackgroundColor(getResources().getColor(R.color.colorBackgroundActionBar));
        }
    }

    @Override
    public void setTitle(final CharSequence title) {
        super.setTitle(title);
        // reflect the title in the actionbar
        ActivityMixin.setTitle(this, title);
    }

    /** call in onCreate to set fixed action bar. If fixed then show/hode won't work and content will NOT be extended behind action bar */
    public void setFixedActionBar(final boolean fixedActionBar) {
        this.fixedActionBar = fixedActionBar;
    }

    @Nullable
    @SuppressLint("DiscouragedApi")
    public View getActionBarView() {
        //see https://stackoverflow.com/questions/20023483/how-to-get-actionbar-view
        final String packageName = getPackageName();
        final int resId = getResources().getIdentifier("action_bar_container", "id", packageName);
        return getWindow().getDecorView().findViewById(resId);
    }

    public int getActionBarHeight() {
        return (int) getResources().getDimension(R.dimen.actionbar_height) + ViewUtils.dpToPixel(10);
    }

    public void hideActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        final View abView = getActionBarView();
        if (actionBar == null || abView == null || !actionBar.isShowing() || fixedActionBar) {
            return;
        }
        abView.animate().translationY(- 2 * getActionBarHeight() - 2 * this.actionBarSystemBarOverlapHeight)
            .withEndAction(actionBar::hide).start();
    }

    public void showActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        final View abView = getActionBarView();
        if (actionBar == null || abView == null || actionBar.isShowing() || fixedActionBar) {
            return;
        }
        actionBar.show();
        applyTranslation();
        abView.setTranslationY(-getActionBarHeight() - this.actionBarSystemBarOverlapHeight);
        abView.animate().translationY(-this.actionBarSystemBarOverlapHeight).start();
    }

    public boolean actionBarIsShowing() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return false;
        }
        return actionBar.isShowing();
    }

    @Override
    @NonNull
    protected Insets calculateInsetsForActivityContent(@NonNull final Insets def) {
        final Insets insets = super.calculateInsetsForActivityContent(def);
        this.actionBarSystemBarOverlapHeight = Math.min(insets.top, ViewUtils.dpToPixel(ACTION_BAR_SYSTEM_BAR_OVERLAP_HEIGHT_MIN));
        applyTranslation();
        if (fixedActionBar) {
            return Insets.of(insets.left, insets.top + getActionBarHeight(), insets.right, insets.bottom);
        }
        return insets;
    }

    private void applyTranslation() {
        final View actionBar = getActionBarView();
        if (actionBar != null) {
            actionBar.setTranslationY(-actionBarSystemBarOverlapHeight);
            actionBar.setPadding(0, actionBarSystemBarOverlapHeight, 0, 0);
        }
    }


    protected void setCacheTitleBar(@Nullable final String geocode, @Nullable final CharSequence name) {
        final CharSequence title;
        if (StringUtils.isNotBlank(name)) {
            title = StringUtils.isNotBlank(geocode) ? name + " (" + geocode + ")" : name;
        } else {
            title = StringUtils.isNotBlank(geocode) ? geocode : res.getString(R.string.cache);
        }
        setCacheTitleBar(title);
    }

    private void setCacheTitleBar(@NonNull final CharSequence title) {
        setTitle(title);
        setCacheTitleBarBackground(null, false);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(android.R.color.transparent);
        }
    }

    /**
     * change the titlebar icon and text to show the current geocache
     */
    protected void setCacheTitleBar(@NonNull final Geocache cache) {
        setTitle(TextUtils.coloredCacheText(this, cache, cache.getName() + " (" + cache.getShortGeocode() + ")"));
        setCacheTitleBarBackground(cache.getType(), cache.isEnabled());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable());
        }
    }

    private void setCacheTitleBarBackground(@Nullable final CacheType cacheType, final boolean useCacheColor) {
        if (!Settings.useColoredActionBar(this)) {
            return;
        }

        // set action bar background color according to cache type
        final View actionBarView = getActionBarView();
        if (actionBarView == null) {
            return;
        }
        if (cacheType == null) {
            actionBarView.setBackgroundColor(getResources().getColor(R.color.colorBackgroundActionBar));
            return;
        }

        final boolean isLightSkin = Settings.isLightSkin(this);
        final int actionbarColor = CacheType.getActionBarColor(this, cacheType, useCacheColor, isLightSkin);
        actionBarView.setBackgroundColor(actionbarColor);
    }
}
