package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;

import com.google.android.material.appbar.MaterialToolbar;
import org.apache.commons.lang3.StringUtils;

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
public class AbstractActionBarActivity extends AbstractActivity {

    private boolean fixedActionBar = true;
    private MaterialToolbar toolbar;
    private int statusBarHeight = 0;
    private int actionBarHeightWithStatusBar = 0;
    private boolean showSpacer = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUpAction();
        showActionBar(true);
    }

    @Override
    public void setContentView(@LayoutRes final int layoutResID) {
        Log.e("AbstractActionBarActivity.setContentView: inflating view #" + layoutResID);
        setContentView(LayoutInflater.from(this).inflate(layoutResID, null));
    }

    @Override
    public void setContentView(final View view) {
        assert view != null;
        toolbar = view.findViewById(R.id.appToolbar);
        if (toolbar != null) {
            // use existing toolbar
            Log.e("AbstractActionBarActivity.setContentView: using given view");
            super.setContentView(view);
        } else {
            // add toolbar above given view
            Log.e("AbstractActionBarActivity.setContentView: creating new frame view");
            super.setContentView(R.layout.activity_base_with_toolbar);
            final ViewGroup contentContainer = findViewById(R.id.activity_content_wrapper);
            if (contentContainer != null) {
                contentContainer.addView(view);
                toolbar = findViewById(R.id.appToolbar);
            }
        }
        setSupportActionBar(toolbar);
        initUpAction();
        // initialize the action bar title with the activity title for single source
        ActivityMixin.setTitle(this, getTitle());
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

    /** call in onCreate to set fixed action bar. If fixed then show/hide won't work and content will NOT be extended behind action bar */
    public void setFixedActionBar(final boolean fixedActionBar) {
        this.fixedActionBar = fixedActionBar;
    }

    @Nullable
    public View getActionBarView() {
        return toolbar;
    }

    public void showActionBar(final boolean show) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null || show == actionBar.isShowing()) {
            return;
        }
        if (show || fixedActionBar) {
            actionBar.show();
        } else {
            actionBar.hide();
        }
        showSpacer(show);
    }

    public void showSpacer(final boolean show) {
        showSpacer = show;
        setSpacerHeight();
    }

    public boolean actionBarIsShowing() {
        final ActionBar actionBar = getSupportActionBar();
        return actionBar != null && actionBar.isShowing();
    }

    @Override
    @NonNull
    protected Insets calculateInsetsForActivityContent(@NonNull final Insets def) {
        return calculateInsetsForActivityContentHelper(def);
    }

    @NonNull
    private Insets calculateInsetsForActivityContentHelper(@NonNull final Insets def) {
        final Insets insets = super.calculateInsetsForActivityContent(def);
        statusBarHeight = insets.top;
        final MaterialToolbar t = findViewById(R.id.appToolbar);
        if (t != null) {
            actionBarHeightWithStatusBar = (int) (getResources().getDimension(R.dimen.actionbar_height) + statusBarHeight);
            // add statusbar height (= def.top) as padding to appBar, increasing its size accordingly, and set inset.top = 0
            t.setPadding(t.getPaddingLeft(), statusBarHeight, t.getPaddingRight(), t.getPaddingBottom());
            final ViewGroup.LayoutParams params = t.getLayoutParams();
            params.height = actionBarHeightWithStatusBar;
            t.setLayoutParams(params);
            setSpacerHeight();
            return Insets.of(insets.left, 0, insets.right, insets.bottom);
        }
        return insets;
    }

    @NonNull
    protected Insets calculateInsetsWithToolbarInPortrait(@NonNull final Insets def) {
        final Insets insets = calculateInsetsForActivityContentHelper(def);
        return Insets.of(insets.left, actionBarHeightWithStatusBar, insets.right, insets.bottom);
    }

    private void setSpacerHeight() {
        final View spacer = findViewById(R.id.spacer);
        if (spacer != null) {
            spacer.setPadding(spacer.getPaddingLeft(), statusBarHeight, spacer.getPaddingRight(), spacer.getPaddingBottom());
            final ViewGroup.LayoutParams params = spacer.getLayoutParams();
            params.height = showSpacer ? actionBarHeightWithStatusBar : statusBarHeight;
            spacer.setLayoutParams(params);
            ViewUtils.setVisibility(spacer, showSpacer || !fixedActionBar ? View.VISIBLE : View.GONE);
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
