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

package cgeo.geocaching.activity

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.TextUtils

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.core.graphics.Insets

import org.apache.commons.lang3.StringUtils

/**
 * Classes actually having an ActionBar (as opposed to the Dialog activities)
 */
class AbstractActionBarActivity : AbstractActivity() {

    private static val ACTION_BAR_SYSTEM_BAR_OVERLAP_HEIGHT_MIN: Int = 50; //dp

    private var actionBarSystemBarOverlapHeight: Int = ViewUtils.dpToPixel(ACTION_BAR_SYSTEM_BAR_OVERLAP_HEIGHT_MIN)
    private var fixedActionBar: Boolean = true

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        initUpAction()
    }

    private Unit initUpAction() {
        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar != null) {
            // don't display a back button it the activity is running as top-level activity with bottom navigation attached
            actionBar.setDisplayHomeAsUpEnabled(!isTaskRoot() || !(this is AbstractNavigationBarActivity))
        }

        val actionBarView: View = getActionBarView()
        if (actionBarView != null) {
            // set action bar background color, otherwise it would be transparent
            actionBarView.setBackgroundColor(getResources().getColor(R.color.colorBackgroundActionBar))
        }
    }

    override     public Unit setTitle(final CharSequence title) {
        super.setTitle(title)
        // reflect the title in the actionbar
        ActivityMixin.setTitle(this, title)
    }

    /** call in onCreate to set fixed action bar. If fixed then show/hode won't work and content will NOT be extended behind action bar */
    public Unit setFixedActionBar(final Boolean fixedActionBar) {
        this.fixedActionBar = fixedActionBar
    }

    @SuppressLint("DiscouragedApi")
    public View getActionBarView() {
        //see https://stackoverflow.com/questions/20023483/how-to-get-actionbar-view
        val packageName: String = getPackageName()
        val resId: Int = getResources().getIdentifier("action_bar_container", "id", packageName)
        return getWindow().getDecorView().findViewById(resId)
    }

    public Int getActionBarHeight() {
        return (Int) getResources().getDimension(R.dimen.actionbar_height) + ViewUtils.dpToPixel(10)
    }

    public Unit hideActionBar() {
        val actionBar: ActionBar = getSupportActionBar()
        val abView: View = getActionBarView()
        if (actionBar == null || abView == null || !actionBar.isShowing() || fixedActionBar) {
            return
        }
        abView.animate().translationY(- 2 * getActionBarHeight() - 2 * this.actionBarSystemBarOverlapHeight)
            .withEndAction(actionBar::hide).start()
    }

    public Unit showActionBar() {
        val actionBar: ActionBar = getSupportActionBar()
        val abView: View = getActionBarView()
        if (actionBar == null || abView == null || actionBar.isShowing() || fixedActionBar) {
            return
        }
        actionBar.show()
        applyTranslation()
        abView.setTranslationY(-getActionBarHeight() - this.actionBarSystemBarOverlapHeight)
        abView.animate().translationY(-this.actionBarSystemBarOverlapHeight).start()
    }

    public Boolean actionBarIsShowing() {
        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar == null) {
            return false
        }
        return actionBar.isShowing()
    }

    override     protected Insets calculateInsetsForActivityContent(final Insets def) {
        val insets: Insets = super.calculateInsetsForActivityContent(def)
        this.actionBarSystemBarOverlapHeight = Math.min(insets.top, ViewUtils.dpToPixel(ACTION_BAR_SYSTEM_BAR_OVERLAP_HEIGHT_MIN))
        applyTranslation()
        if (fixedActionBar) {
            return Insets.of(insets.left, insets.top + getActionBarHeight(), insets.right, insets.bottom)
        }
        return insets
    }

    private Unit applyTranslation() {
        val actionBar: View = getActionBarView()
        if (actionBar != null) {
            actionBar.setTranslationY(-actionBarSystemBarOverlapHeight)
            actionBar.setPadding(0, actionBarSystemBarOverlapHeight, 0, 0)
        }
    }


    protected Unit setCacheTitleBar(final String geocode, final CharSequence name) {
        final CharSequence title
        if (StringUtils.isNotBlank(name)) {
            title = StringUtils.isNotBlank(geocode) ? name + " (" + geocode + ")" : name
        } else {
            title = StringUtils.isNotBlank(geocode) ? geocode : res.getString(R.string.cache)
        }
        setCacheTitleBar(title)
    }

    private Unit setCacheTitleBar(final CharSequence title) {
        setTitle(title)
        setCacheTitleBarBackground(null, false)

        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setIcon(android.R.color.transparent)
        }
    }

    /**
     * change the titlebar icon and text to show the current geocache
     */
    protected Unit setCacheTitleBar(final Geocache cache) {
        setTitle(TextUtils.coloredCacheText(this, cache, cache.getName() + " (" + cache.getShortGeocode() + ")"))
        setCacheTitleBarBackground(cache.getType(), cache.isEnabled())

        val actionBar: ActionBar = getSupportActionBar()
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setIcon(MapMarkerUtils.getCacheMarker(getResources(), cache, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable())
        }
    }

    private Unit setCacheTitleBarBackground(final CacheType cacheType, final Boolean useCacheColor) {
        if (!Settings.useColoredActionBar(this)) {
            return
        }

        // set action bar background color according to cache type
        val actionBarView: View = getActionBarView()
        if (actionBarView == null) {
            return
        }
        if (cacheType == null) {
            actionBarView.setBackgroundColor(getResources().getColor(R.color.colorBackgroundActionBar))
            return
        }

        val actionbarColor: Int = CacheType.getActionBarColor(this, cacheType, useCacheColor)
        actionBarView.setBackgroundColor(actionbarColor)
    }
}
