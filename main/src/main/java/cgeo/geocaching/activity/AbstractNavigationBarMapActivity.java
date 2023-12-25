package cgeo.geocaching.activity;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CachePopupFragment;
import cgeo.geocaching.R;
import cgeo.geocaching.WaypointPopupFragment;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.sidesheet.SideSheetBehavior;
import com.google.android.material.sidesheet.SideSheetCallback;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public abstract class AbstractNavigationBarMapActivity extends AbstractNavigationBarActivity {

    private static final String TAG_MAPDETAILS_FRAGMENT = "mapdetails_fragment";
    private static final String TAG_SWIPE_FRAGMENT = "swipetoopen_fragment";

    private final ViewTreeObserver.OnGlobalLayoutListener[] layoutListeners = new ViewTreeObserver.OnGlobalLayoutListener[1];

    @Override
    public void onBackPressed() {
        // try to remove map details fragment first
        if (sheetRemoveFragment()) {
            return;
        }
        super.onBackPressed();
    }

    // ---------------------------------------------------------------------------------------------
    // Handling of cache/waypoint details fragments

    protected abstract void clearSheetInfo();

    public void sheetShowDetails(@Nullable final UnifiedMapViewModel.SheetInfo sheetInfo) {
        if  (sheetInfo == null || StringUtils.isBlank(sheetInfo.geocode)) {
            return;
        }
        if (sheetInfo.waypointId <= 0) {
            sheetConfigureFragment(CachePopupFragment.newInstance(sheetInfo.geocode), () -> CacheDetailActivity.startActivity(this, sheetInfo.geocode));
        } else {
            sheetConfigureFragment(WaypointPopupFragment.newInstance(sheetInfo.geocode, sheetInfo.waypointId), () -> CacheDetailActivity.startActivity(this, sheetInfo.geocode));
        }
    }

    private void sheetConfigureFragment(final Fragment fragment, final Runnable onUpSwipeAction) {
        final FrameLayout fl = findViewById(R.id.detailsfragment);

        Optional.ofNullable(fragment.getView())
                .map(v -> (LinearLayout) v.findViewById(R.id.swipeInfo))
                .ifPresent(hint -> hint.setVisibility(View.GONE));

        final ViewGroup.LayoutParams params = fl.getLayoutParams();
        final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        final boolean isBottomSheet = behavior instanceof BottomSheetBehavior;

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.detailsfragment, fragment, TAG_MAPDETAILS_FRAGMENT).commit();

        if (isBottomSheet) { // portrait mode uses BottomSheet
            final BottomSheetBehavior<FrameLayout> b = BottomSheetBehavior.from(fl);
            b.setHideable(true);
            b.setSkipCollapsed(false);
            b.setState(BottomSheetBehavior.STATE_COLLAPSED);

            ft.runOnCommit(() -> {
                final View view = fragment.requireView();
                // set the height of collapsed state and enable swipeinfo
                synchronized (layoutListeners) {
                    if (layoutListeners[0] != null) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListeners[0]);
                    }
                    layoutListeners[0] = () -> {
                        final LinearLayout hint = view.findViewById(R.id.swipeInfo);
                        if (hint != null) {
                            if (hint.getVisibility() == View.GONE) {
                                b.setPeekHeight(view.getHeight());
                                hint.setVisibility(View.VISIBLE);
                            }
                        }
                    };
                    view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListeners[0]);
                }
            });

            final Activity that = this;
            final BottomSheetBehavior.BottomSheetCallback[] callbackStore = new BottomSheetBehavior.BottomSheetCallback[] { null };
            callbackStore[0] = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        sheetRemoveFragment();
                        b.removeBottomSheetCallback(callbackStore[0]);
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED && onUpSwipeAction != null) {
                        onUpSwipeAction.run();
                        ActivityMixin.overrideTransitionToFade(that);
                    }
                }

                @Override
                public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                    // nothing
                }
            };
            b.addBottomSheetCallback(callbackStore[0]);

        } else { // landscape mode uses SideSheet
            final SideSheetBehavior<FrameLayout> b = SideSheetBehavior.from(fl);
            b.setState(SideSheetBehavior.STATE_EXPANDED);

            final SideSheetCallback[] callbackStore = new SideSheetCallback[] { null };
            callbackStore[0] = new SideSheetCallback() {
                @Override
                public void onStateChanged(@NonNull final View sheet, final int newState) {
                    if (newState == SideSheetBehavior.STATE_HIDDEN) {
                        sheetRemoveFragment();
                        b.removeCallback(callbackStore[0]);
                    }
                }

                @Override
                public void onSlide(@NonNull final View sheet, final float slideOffset) {
                    // nothing
                }
            };
            b.addCallback(callbackStore[0]);
        }

        fl.setVisibility(View.VISIBLE);
    }

    /** removes fragment and view for mapdetails view; returns true, if view got removed */
    public boolean sheetRemoveFragment() {
        clearSheetInfo();
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f1 = fm.findFragmentByTag(TAG_MAPDETAILS_FRAGMENT);
        if (f1 != null) {
            fm.beginTransaction().remove(f1).commit();
        }
        final Fragment f2 = fm.findFragmentByTag(TAG_SWIPE_FRAGMENT);
        if (f2 != null) {
            fm.beginTransaction().remove(f2).commit();
        }
        final FrameLayout v = findViewById(R.id.detailsfragment);
        if (v != null && v.getVisibility() != View.GONE) {

            final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) v.getLayoutParams()).getBehavior();
            if (behavior instanceof BottomSheetBehavior) {
                final BottomSheetBehavior<FrameLayout> b = BottomSheetBehavior.from(v);
                b.setState(BottomSheetBehavior.STATE_HIDDEN); // close correctly as it will otherwise conflict with up-swipe behaviour implementation
            }

            v.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    // manage onStart lifecycle for SheetInfo:
    // - read current value
    // - reset details fragment
    // - restore current value
    // - open new sheet (if non-empty)
    public void sheetManageLifecycleOnStart(@Nullable final UnifiedMapViewModel.SheetInfo sheetInfo, @NonNull final Action1<UnifiedMapViewModel.SheetInfo> setSheetInfo) {
        sheetRemoveFragment();
        setSheetInfo.call(sheetInfo);
        sheetShowDetails(sheetInfo);
    }

}
