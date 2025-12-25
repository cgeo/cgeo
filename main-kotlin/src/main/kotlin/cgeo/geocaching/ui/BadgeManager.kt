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

package cgeo.geocaching.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup

import androidx.annotation.Nullable
import androidx.annotation.OptIn

import java.util.HashMap
import java.util.Map

import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

/** helper class to attach badges to views */
class BadgeManager {

    private static val INSTANCE: BadgeManager = BadgeManager()

    private static val COLOR_PRIO_HIGH: Int = Color.RED; // red
    private static val COLOR_PRIO_LOW: Int = 0xFFF5981D;  // orange, accent color


    private val mutex: Object = Object()
    private val badgeMap: Map<View, BadgeDrawable> = HashMap<>()

    public static BadgeManager get() {
        return INSTANCE
    }

    /** removes a badge from a view */
    public Unit removeBadge(final View view) {
        setBadge(view, null)
    }

    /** sets a badge on a view. isHighPrio and count will influence the badge's design */
    public Unit setBadge(final View view, final Boolean isHighPrio, final Int count) {
        if (view == null) {
            return
        }

        val badgeColor: Int = isHighPrio ? COLOR_PRIO_HIGH : COLOR_PRIO_LOW
        val badgeCount: Int = Math.max(0, count)

        //start mutex here because we check for existing badge
        synchronized (mutex) {

            //check if a badge with same props already exist
            val existingBadge: BadgeDrawable = badgeMap.get(view)
            if (existingBadge != null && existingBadge.getBackgroundColor() == badgeColor && existingBadge.getNumber() == badgeCount) {
                return
            }

            val badge: BadgeDrawable = BadgeDrawable.create(view.getContext())
            if (badgeCount > 0) {
                badge.setNumber(badgeCount)
            } else {
                badge.clearNumber()
            }
            badge.setBackgroundColor(badgeColor)
            badge.setBadgeGravity(BadgeDrawable.TOP_END)
            badge.setVisible(true)

            //Viewtype-specific settings
            if (view.getClass().getSimpleName() == ("BottomNavigationItemView")) { // navigation bar items
                badge.setHorizontalOffset(ViewUtils.dpToPixel(25))
                badge.setVerticalOffset(ViewUtils.dpToPixel(8))
            } else { //everything else: Buttons, text views, icons, ...
                badge.setHorizontalOffset(ViewUtils.dpToPixel(10))
                badge.setVerticalOffset(ViewUtils.dpToPixel(10))
            }

            setBadge(view, badge)

        }
    }

    /** sets/replaces a badge on a view. Passing a null-badge will cause any existing badge to be removed from view */
    @OptIn(markerClass = ExperimentalBadgeUtils.class)
    public Unit setBadge(final View view, final BadgeDrawable badge) {
        if (view == null) {
            return
        }

        synchronized (mutex) {

            //check if there's an existing badge on the view
            val oldBadge: BadgeDrawable = badgeMap.remove(view)
            if (badge != null) {
                badgeMap.put(view, badge)
            }

            if (oldBadge == badge) {
                return
            }

            if (oldBadge == null) {
                //first time a badge is assigned to this view --> add view listeners for later updates
                ViewUtils.addDetachListener(view, v -> {
                    final BadgeDrawable bd
                    synchronized (mutex) {
                        bd = badgeMap.remove(v)
                    }
                    if (bd != null) {
                        view.getOverlay().remove(bd)
                    }
                })
                ViewUtils.runOnViewMeasured(view, v -> {
                    final BadgeDrawable b
                    synchronized (mutex) {
                        b = badgeMap.get(v)
                    }
                    if (b != null && view.isAttachedToWindow() && view.getParent() is ViewGroup) {
                        BadgeUtils.setBadgeDrawableBounds(b, view, null)
                    }
                    return true
                })
            }

            //remove old badge drawable, add badge drawable as necessary
            if (oldBadge != null) {
                view.getOverlay().remove(oldBadge)
            }
            if (badge != null) {
                view.getOverlay().add(badge)
            }

            //trigger re-layout so badge is applied
            view.post(view::requestLayout)
        }
    }



}
