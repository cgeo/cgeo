package cgeo.geocaching.ui;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;

import java.util.HashMap;
import java.util.Map;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;

/** helper class to attach badges to views */
public class BadgeManager {

    private static final BadgeManager INSTANCE = new BadgeManager();

    private static final int COLOR_PRIO_HIGH = Color.RED; // red
    private static final int COLOR_PRIO_LOW = 0xFFF5981D;  // orange, accent color


    private final Object mutex = new Object();
    private final Map<View, BadgeDrawable> badgeMap = new HashMap<>();

    public static BadgeManager get() {
        return INSTANCE;
    }

    /** removes a badge from a view */
    public void removeBadge(@Nullable final View view) {
        setBadge(view, null);
    }

    /** sets a badge on a view. isHighPrio and count will influence the badge's design */
    public void setBadge(@Nullable final View view, final boolean isHighPrio, final int count) {
        if (view == null) {
            return;
        }

        final int badgeColor = isHighPrio ? COLOR_PRIO_HIGH : COLOR_PRIO_LOW;
        final int badgeCount = Math.max(0, count);

        //start mutex here because we check for existing badge
        synchronized (mutex) {

            //check if a badge with same props already exist
            final BadgeDrawable existingBadge = badgeMap.get(view);
            if (existingBadge != null && existingBadge.getBackgroundColor() == badgeColor && existingBadge.getNumber() == badgeCount) {
                return;
            }

            final BadgeDrawable badge = BadgeDrawable.create(view.getContext());
            if (badgeCount > 0) {
                badge.setNumber(badgeCount);
            } else {
                badge.clearNumber();
            }
            badge.setBackgroundColor(badgeColor);
            badge.setBadgeGravity(BadgeDrawable.TOP_END);
            badge.setVisible(true);

            //Viewtype-specific settings
            if (view.getClass().getSimpleName().equals("BottomNavigationItemView")) { // navigation bar items
                badge.setHorizontalOffset(ViewUtils.dpToPixel(25));
                badge.setVerticalOffset(ViewUtils.dpToPixel(8));
            } else { //everything else: Buttons, text views, icons, ...
                badge.setHorizontalOffset(ViewUtils.dpToPixel(10));
                badge.setVerticalOffset(ViewUtils.dpToPixel(10));
            }

            setBadge(view, badge);

        }
    }

    /** sets/replaces a badge on a view. Passing a null-badge will cause any existing badge to be removed from view */
    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    public void setBadge(@Nullable final View view, @Nullable final BadgeDrawable badge) {
        if (view == null) {
            return;
        }

        synchronized (mutex) {

            //check if there's an existing badge on the view
            final BadgeDrawable oldBadge = badgeMap.remove(view);
            if (badge != null) {
                badgeMap.put(view, badge);
            }

            if (oldBadge == badge) {
                return;
            }

            if (oldBadge == null) {
                //first time a badge is assigned to this view --> add view listeners for later updates
                ViewUtils.addDetachListener(view, v -> {
                    final BadgeDrawable bd;
                    synchronized (mutex) {
                        bd = badgeMap.remove(v);
                    }
                    if (bd != null) {
                        view.getOverlay().remove(bd);
                    }
                });
                ViewUtils.runOnViewMeasured(view, v -> {
                    final BadgeDrawable b;
                    synchronized (mutex) {
                        b = badgeMap.get(v);
                    }
                    if (b != null && view.isAttachedToWindow() && view.getParent() instanceof ViewGroup) {
                        BadgeUtils.setBadgeDrawableBounds(b, view, null);
                    }
                    return true;
                });
            }

            //remove old badge drawable, add new badge drawable as necessary
            if (oldBadge != null) {
                view.getOverlay().remove(oldBadge);
            }
            if (badge != null) {
                view.getOverlay().add(badge);
            }

            //trigger re-layout so badge is applied
            view.post(view::requestLayout);
        }
    }



}
