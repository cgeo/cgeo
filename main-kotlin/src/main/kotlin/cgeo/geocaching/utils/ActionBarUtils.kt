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

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.activity.AbstractNavigationBarActivity
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils

import android.app.Activity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.Window

import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import org.apache.commons.lang3.StringUtils

class ActionBarUtils {

    private ActionBarUtils() {
        // utility class
    }

    /** use this instead of AbstractBottomNavigationActivity.setContentView for being able to use a action bar toogle */
    public static Unit setContentView(final AbstractNavigationBarActivity activity, final View contentView, final Boolean showSpacer) {
        activity.setContentView(contentView)
        showActionBarSpacer(activity, showSpacer)
    }

    /** use this instead of AbstractBottomNavigationActivity.setContentView for being able to use a action bar toogle */
    public static Unit setContentView(final AbstractNavigationBarActivity activity, @LayoutRes final Int layoutResID, final Boolean showSpacer) {
        activity.setContentView(layoutResID)
        showActionBarSpacer(activity, showSpacer)
    }

    public static Boolean toggleActionBar(final AbstractActionBarActivity activity) {
        if (!Settings.getMapActionbarAutohide()) {
            return true
        }
        val actionBar: View = activity.getActionBarView()
        if (actionBar == null) {
            return true
        }
        val isShown: Boolean = activity.actionBarIsShowing()

        if (!isShown) {
            activity.showActionBar()
        } else {
            activity.hideActionBar()
        }

        // adjust system bars appearance, depending on action bar color and visibility
        ActionBarUtils.setSystemBarAppearance(activity, !isShown)

        val spacer: View = activity.findViewById(R.id.actionBarSpacer)
        val height: Int = !isShown ? 0 : -spacer.getHeight()
        ViewUtils.applyToView(activity.findViewById(R.id.filterbar), view -> view.animate().translationY(height).start())
        ViewUtils.applyToView(activity.findViewById(R.id.distanceinfo), view -> view.animate().translationY(height).start())
        ViewUtils.applyToView(activity.findViewById(R.id.map_progressbar), view -> view.animate().translationY(height).start())

        return !isShown
    }

    private static Unit showActionBarSpacer(final Activity activity, final Boolean showSpacer) {
        activity.findViewById(R.id.actionBarSpacer).setVisibility(showSpacer ? View.VISIBLE : View.GONE)
    }

    public static Unit setSystemBarAppearance(final Activity activity, final Boolean isActionBarShown) {
        val currentWindow: Window = activity.getWindow()
        val windowInsetsController: WindowInsetsControllerCompat = WindowCompat.getInsetsController(currentWindow, currentWindow.getDecorView())

        // set light/dark system bars depending on action bar colors
        val isLightSkin: Boolean = Settings.isLightSkin(activity)
        if (isLightSkin) {
            windowInsetsController.setAppearanceLightStatusBars(!isActionBarShown)
            windowInsetsController.setAppearanceLightNavigationBars(true)
        } else {
            windowInsetsController.setAppearanceLightStatusBars(false)
            windowInsetsController.setAppearanceLightNavigationBars(false)
        }
    }

    public static Unit setSubtitle(final AbstractActionBarActivity activity, final CharSequence subtitleText) {
        val supportActionBar: ActionBar = activity.getSupportActionBar()
        if (supportActionBar == null || StringUtils.isEmpty(subtitleText)) {
            return
        }

        val titleString: SpannableString = getSpannedTitle(subtitleText, R.color.colorTextHintActionBar)
        supportActionBar.setSubtitle(titleString)
    }

    public static Unit setTitle(final AbstractActionBarActivity activity, final CharSequence titleText) {
        val supportActionBar: ActionBar = activity.getSupportActionBar()
        if (supportActionBar == null || StringUtils.isEmpty(titleText)) {
            return
        }

        val titleString: SpannableString = getSpannedTitle(titleText, R.color.colorTextActionBar)
        supportActionBar.setTitle(titleString)
    }

    // @todo remove after switching map ActionBar to Toolbar
    // workaround for colored ActionBar titles/subtitles
    // Checking for an existing span of the given class
    private static SpannableString getSpannedTitle(final CharSequence spanText, final @ColorRes Int colorRes) {
        // // If a Spanned is already present, check whether a ForegroundColorSpan covers the entire text
        if (TextUtils.hasSpanCoveringWholeText(spanText, ForegroundColorSpan.class)) {
            return SpannableString(spanText)
        }

        // Create span with actionbar text color
        val titleString: SpannableString = SpannableString(spanText)
        val color: Int = ColorUtils.colorFromResource(colorRes)
        titleString.setSpan(ForegroundColorSpan(color), 0, titleString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return titleString
    }
}
