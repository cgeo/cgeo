package cgeo.geocaching.activity;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.CachePopupFragment;
import cgeo.geocaching.R;
import cgeo.geocaching.SwipeToOpenFragment;
import cgeo.geocaching.WaypointPopupFragment;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel;
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

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

public abstract class AbstractNavigationBarMapActivity extends AbstractNavigationBarActivity {

    private static final String TAG_MAPDETAILS_FRAGMENT = "mapdetails_fragment";
    private static final String TAG_SWIPE_FRAGMENT = "swipetoopen_fragment";
    private static long close429warning = 0;

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
        final ViewGroup.LayoutParams params = fl.getLayoutParams();
        final CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        final boolean isBottomSheet = behavior instanceof BottomSheetBehavior;

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final SwipeToOpenFragment swipeToOpenFragment = isBottomSheet ? new SwipeToOpenFragment() : null;
        if (isBottomSheet) {
            ft.replace(R.id.detailsfragment, swipeToOpenFragment, TAG_SWIPE_FRAGMENT);
            ft.add(R.id.detailsfragment, fragment, TAG_MAPDETAILS_FRAGMENT);
        } else {
            ft.replace(R.id.detailsfragment, fragment, TAG_MAPDETAILS_FRAGMENT);
        }
        ft.commit();

        if (isBottomSheet) { // portrait mode uses BottomSheet
            final BottomSheetBehavior<FrameLayout> b = BottomSheetBehavior.from(fl);
            b.setHideable(true);
            b.setSkipCollapsed(false);
            b.setPeekHeight(0); // temporary set to 0 to avoid bumping. Gets updated once view is loaded.
            b.setState(BottomSheetBehavior.STATE_COLLAPSED);

            ft.runOnCommit(() -> {
                final View view = fragment.requireView();
                // make bottom sheet fill whole screen
                swipeToOpenFragment.requireView().setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
                // set the height of collapsed state to height of the details fragment
                synchronized (layoutListeners) {
                    if (layoutListeners[0] != null) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListeners[0]);
                    }
                    layoutListeners[0] = () -> b.setPeekHeight(view.getHeight());
                    view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListeners[0]);
                }
            });

            final Activity that = this;
            final BottomSheetBehavior.BottomSheetCallback callback = new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        sheetRemoveFragment();
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED && onUpSwipeAction != null) {
                        onUpSwipeAction.run();
                        ActivityMixin.overrideTransitionToFade(that);
                        ActivityMixin.postDelayed(() -> sheetRemoveFragment(), 500);
                    }
                }

                @Override
                public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                    swipeToOpenFragment.setExpansion(slideOffset, fragment.getView());
                }
            };

            b.addBottomSheetCallback(callback);
            swipeToOpenFragment.setOnStopCallback(() -> b.removeBottomSheetCallback(callback));
        } else { // landscape mode uses SideSheet
            final SideSheetBehavior<FrameLayout> b = SideSheetBehavior.from(fl);
            b.setState(SideSheetBehavior.STATE_EXPANDED);
            final SideSheetCallback[] callbackStore = new SideSheetCallback[] { null };

            final SideSheetCallback callback = new SideSheetCallback() {
                @Override
                public void onStateChanged(@NonNull final View sheet, final int newState) {
                    if (newState == SideSheetBehavior.STATE_HIDDEN) {
                        sheetRemoveFragment();
                        if (callbackStore[0] != null) {
                            b.removeCallback(callbackStore[0]);
                        }
                    }
                }

                @Override
                public void onSlide(@NonNull final View sheet, final float slideOffset) {
                    // nothing
                }
            };
            callbackStore[0] = callback;
            b.addCallback(callback);
        }

        fl.setVisibility(View.VISIBLE);
    }

    /** removes fragment and view for mapdetails view; returns true, if view got removed */
    public boolean sheetRemoveFragment() {
        //check activity state
        if (isFinishing() || isDestroyed()) {
            return false;
        }
        clearSheetInfo();
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f1 = fm.findFragmentByTag(TAG_MAPDETAILS_FRAGMENT);
        if (f1 != null) {
            try {
                fm.beginTransaction().remove(f1).commitNowAllowingStateLoss();
            } catch (IllegalStateException ignore) {
                // ignore fm error on sheet removal
            }
        }
        final Fragment f2 = fm.findFragmentByTag(TAG_SWIPE_FRAGMENT);
        if (f2 != null) {
            try {
                fm.beginTransaction().remove(f2).commitNowAllowingStateLoss();
            } catch (IllegalStateException ignore) {
                // ignore fm error on sheet removal
            }
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

    // handling of http429 warning message
    protected void add429observer() {
        getLifecycle().addObserver(new LifecycleAwareBroadcastReceiver(this, HttpRequest.HTTP429) {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                synchronized (this) {
                    if (close429warning == 0) {
                        final ImageView v = findViewById(R.id.live_map_status);
                        if (v != null) {
                            v.setImageResource(R.drawable.warning);
                            v.getBackground().setTint(getResources().getColor(R.color.colorAccent));
                            v.setOnClickListener(v1 -> SimpleDialog.ofContext(AbstractNavigationBarMapActivity.this).setMessage(TextParam.text(String.format(getString(R.string.live_map_status_http429), intent.getStringExtra(HttpRequest.HTTP429_ADDRESS)))).show());
                            new Handler(Looper.getMainLooper()).post(() -> v.setVisibility(View.VISIBLE));
                        }
                    }
                    close429warning = System.currentTimeMillis() + 1000; // show for 1s
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        synchronized (this) {
                            if (System.currentTimeMillis() > close429warning) {
                                final View v = findViewById(R.id.live_map_status);
                                if (v != null) {
                                    v.setVisibility(View.GONE);
                                }
                                close429warning = 0;
                            }
                        }
                    }, 5000);
                }
            }
        });

    }
}
