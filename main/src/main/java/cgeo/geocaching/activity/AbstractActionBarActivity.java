package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ViewUtils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

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
    protected Insets calculateInsetsForActivityContent(@NonNull final WindowInsetsCompat windowInsets, @NonNull final Insets def) {
        final Insets insets = super.calculateInsetsForActivityContent(windowInsets, def);
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
            actionBar.setBackgroundResource(R.color.colorBackgroundActionBar);
        }
    }


}
