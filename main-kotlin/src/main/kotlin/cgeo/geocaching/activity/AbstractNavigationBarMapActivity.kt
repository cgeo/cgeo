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

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.CachePopupFragment
import cgeo.geocaching.R
import cgeo.geocaching.SwipeToOpenFragment
import cgeo.geocaching.WaypointPopupFragment
import cgeo.geocaching.network.HttpRequest
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.functions.Action1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.sidesheet.SideSheetCallback
import org.apache.commons.lang3.StringUtils

abstract class AbstractNavigationBarMapActivity : AbstractNavigationBarActivity() {

    private static val TAG_MAPDETAILS_FRAGMENT: String = "mapdetails_fragment"
    private static val TAG_SWIPE_FRAGMENT: String = "swipetoopen_fragment"
    private static Long close429warning = 0

    private final ViewTreeObserver.OnGlobalLayoutListener[] layoutListeners = ViewTreeObserver.OnGlobalLayoutListener[1]

    override     public Unit onCreate(final Bundle savedInstanceState) {
        setFixedActionBar(false)
        super.onCreate(savedInstanceState)
    }

    override     public Unit onBackPressed() {
        // try to remove map details fragment first
        if (sheetRemoveFragment()) {
            return
        }
        super.onBackPressed()
    }

    // ---------------------------------------------------------------------------------------------
    // Handling of cache/waypoint details fragments

    protected abstract Unit clearSheetInfo()

    public Unit sheetShowDetails(final UnifiedMapViewModel.SheetInfo sheetInfo) {
        if  (sheetInfo == null || StringUtils.isBlank(sheetInfo.geocode)) {
            return
        }
        if (sheetInfo.waypointId <= 0) {
            sheetConfigureFragment(CachePopupFragment.newInstance(sheetInfo.geocode), () -> CacheDetailActivity.startActivity(this, sheetInfo.geocode))
        } else {
            sheetConfigureFragment(WaypointPopupFragment.newInstance(sheetInfo.geocode, sheetInfo.waypointId), () -> CacheDetailActivity.startActivity(this, sheetInfo.geocode))
        }
    }

    private Unit sheetConfigureFragment(final Fragment fragment, final Runnable onUpSwipeAction) {
        val fl: FrameLayout = findViewById(R.id.detailsfragment)
        final ViewGroup.LayoutParams params = fl.getLayoutParams()
        final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior()
        val isBottomSheet: Boolean = behavior is BottomSheetBehavior

        val ft: FragmentTransaction = getSupportFragmentManager().beginTransaction()
        val swipeToOpenFragment: SwipeToOpenFragment = isBottomSheet ? SwipeToOpenFragment() : null
        if (isBottomSheet) {
            ft.replace(R.id.detailsfragment, swipeToOpenFragment, TAG_SWIPE_FRAGMENT)
            ft.add(R.id.detailsfragment, fragment, TAG_MAPDETAILS_FRAGMENT)
        } else {
            ft.replace(R.id.detailsfragment, fragment, TAG_MAPDETAILS_FRAGMENT)
        }
        ft.commit()

        if (isBottomSheet) { // portrait mode uses BottomSheet
            val b: BottomSheetBehavior<FrameLayout> = BottomSheetBehavior.from(fl)
            b.setHideable(true)
            b.setSkipCollapsed(false)
            b.setPeekHeight(0); // temporary set to 0 to avoid bumping. Gets updated once view is loaded.
            b.setState(BottomSheetBehavior.STATE_COLLAPSED)

            ft.runOnCommit(() -> {
                val view: View = fragment.requireView()
                // make bottom sheet fill whole screen
                swipeToOpenFragment.requireView().setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels)
                // set the height of collapsed state to height of the details fragment
                synchronized (layoutListeners) {
                    if (layoutListeners[0] != null) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListeners[0])
                    }
                    layoutListeners[0] = () -> b.setPeekHeight(view.getHeight())
                    view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListeners[0])
                }
            })

            val that: Activity = this
            final BottomSheetBehavior.BottomSheetCallback callback = BottomSheetBehavior.BottomSheetCallback() {
                override                 public Unit onStateChanged(final View bottomSheet, final Int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        sheetRemoveFragment()
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED && onUpSwipeAction != null) {
                        onUpSwipeAction.run()
                        ActivityMixin.overrideTransitionToFade(that)
                        ActivityMixin.postDelayed(() -> sheetRemoveFragment(), 500)
                    }
                }

                override                 public Unit onSlide(final View bottomSheet, final Float slideOffset) {
                    swipeToOpenFragment.setExpansion(slideOffset, fragment.getView())
                }
            }

            b.addBottomSheetCallback(callback)
            swipeToOpenFragment.setOnStopCallback(() -> b.removeBottomSheetCallback(callback))
        } else { // landscape mode uses SideSheet
            val b: SideSheetBehavior<FrameLayout> = SideSheetBehavior.from(fl)
            b.setState(SideSheetBehavior.STATE_EXPANDED)
            final SideSheetCallback[] callbackStore = SideSheetCallback[] { null }

            val callback: SideSheetCallback = SideSheetCallback() {
                override                 public Unit onStateChanged(final View sheet, final Int newState) {
                    if (newState == SideSheetBehavior.STATE_HIDDEN) {
                        sheetRemoveFragment()
                        if (callbackStore[0] != null) {
                            b.removeCallback(callbackStore[0])
                        }
                    }
                }

                override                 public Unit onSlide(final View sheet, final Float slideOffset) {
                    // nothing
                }
            }
            callbackStore[0] = callback
            b.addCallback(callback)
        }

        fl.setVisibility(View.VISIBLE)
    }

    /** removes fragment and view for mapdetails view; returns true, if view got removed */
    public Boolean sheetRemoveFragment() {
        //check activity state
        if (isFinishing() || isDestroyed()) {
            return false
        }
        clearSheetInfo()
        val fm: FragmentManager = getSupportFragmentManager()
        val f1: Fragment = fm.findFragmentByTag(TAG_MAPDETAILS_FRAGMENT)
        if (f1 != null) {
            try {
                fm.beginTransaction().remove(f1).commitNowAllowingStateLoss()
            } catch (IllegalStateException ignore) {
                // ignore fm error on sheet removal
            }
        }
        val f2: Fragment = fm.findFragmentByTag(TAG_SWIPE_FRAGMENT)
        if (f2 != null) {
            try {
                fm.beginTransaction().remove(f2).commitNowAllowingStateLoss()
            } catch (IllegalStateException ignore) {
                // ignore fm error on sheet removal
            }
        }
        val v: FrameLayout = findViewById(R.id.detailsfragment)
        if (v != null && v.getVisibility() != View.GONE) {

            final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) v.getLayoutParams()).getBehavior()
            if (behavior is BottomSheetBehavior) {
                val b: BottomSheetBehavior<FrameLayout> = BottomSheetBehavior.from(v)
                b.setState(BottomSheetBehavior.STATE_HIDDEN); // close correctly as it will otherwise conflict with up-swipe behaviour implementation
            }

            v.setVisibility(View.GONE)
            return true
        }
        return false
    }

    // manage onStart lifecycle for SheetInfo:
    // - read current value
    // - reset details fragment
    // - restore current value
    // - open sheet (if non-empty)
    public Unit sheetManageLifecycleOnStart(final UnifiedMapViewModel.SheetInfo sheetInfo, final Action1<UnifiedMapViewModel.SheetInfo> setSheetInfo) {
        sheetRemoveFragment()
        setSheetInfo.call(sheetInfo)
        sheetShowDetails(sheetInfo)
    }

    // handling of http429 warning message
    protected Unit add429observer() {
        getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, HttpRequest.HTTP429) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                synchronized (this) {
                    if (close429warning == 0) {
                        val v: ImageView = findViewById(R.id.live_map_status)
                        if (v != null) {
                            v.setImageResource(R.drawable.warning)
                            v.getBackground().setTint(getResources().getColor(R.color.colorAccent))
                            v.setOnClickListener(v1 -> SimpleDialog.ofContext(AbstractNavigationBarMapActivity.this).setMessage(TextParam.text(String.format(getString(R.string.live_map_status_http429), intent.getStringExtra(HttpRequest.HTTP429_ADDRESS)))).show())
                            Handler(Looper.getMainLooper()).post(() -> v.setVisibility(View.VISIBLE))
                        }
                    }
                    close429warning = System.currentTimeMillis() + 1000; // show for 1s
                    Handler(Looper.getMainLooper()).postDelayed(() -> {
                        synchronized (this) {
                            if (System.currentTimeMillis() > close429warning) {
                                val v: View = findViewById(R.id.live_map_status)
                                if (v != null) {
                                    v.setVisibility(View.GONE)
                                }
                                close429warning = 0
                            }
                        }
                    }, 5000)
                }
            }
        })

    }
}
