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

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.utils.functions.Action1

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger

import org.apache.commons.lang3.tuple.Triple

class SimplePopupMenu {
    private final Context context

    private val itemListeners: Map<Integer, OnItemClickListener> = HashMap<>()
    private final List<Triple<Integer, CharSequence, Drawable>> additionalMenuItems = ArrayList<>()

    private val uniqueIdProvider: AtomicInteger = AtomicInteger(23435)

    private Activity activity
    private Point point
    private Int padding

    private View view

    private @MenuRes Int menuRes

    private OnCreatePopupMenuListener onCreateListener
    private PopupMenu.OnDismissListener onDismissListener
    private PopupMenu.OnMenuItemClickListener onItemClickListener

    private SimplePopupMenu(final Context context) {
        this.context = context
    }

    public static SimplePopupMenu forView(final View view) {
        val popupMenu: SimplePopupMenu = SimplePopupMenu(view.getContext())
        popupMenu.view = view
        return popupMenu
    }

    public static SimplePopupMenu of(final Activity activity) {
        val popupMenu: SimplePopupMenu = SimplePopupMenu(activity)
        popupMenu.activity = activity
        return popupMenu
    }

    /**
     * Will only have an effect if no view is set as anchor. Both values are in px.
     */
    public SimplePopupMenu setPosition(final Point point, final Int padding) {
        this.point = point
        this.padding = padding
        return this
    }

    public SimplePopupMenu addItemClickListener(final @IdRes Int id, final OnItemClickListener listener) {
        this.itemListeners.put(id, listener)
        return this
    }

    public SimplePopupMenu addMenuItem(final Int uniqueId, final CharSequence title) {
        this.additionalMenuItems.add(Triple.of(uniqueId, title, null))
        return this
    }

    public SimplePopupMenu addMenuItem(final Int uniqueId, final CharSequence title, final Drawable icon) {
        this.additionalMenuItems.add(Triple.of(uniqueId, title, icon))
        return this
    }

    public SimplePopupMenu addMenuItem(final Int uniqueId, final CharSequence title, final @DrawableRes Int icon) {
        this.additionalMenuItems.add(Triple.of(uniqueId, title, AppCompatResources.getDrawable(context, icon)))
        return this
    }

    public SimplePopupMenu addMenuItem(final CharSequence title, @DrawableRes final Int drawable, final Action1<MenuItem> clickAction) {
        val uniqueId: Int = uniqueIdProvider.addAndGet(1)
        addMenuItem(uniqueId, title, drawable)
        addItemClickListener(uniqueId, clickAction::call)
        return this
    }

    public SimplePopupMenu setMenuContent(final @MenuRes Int menuRes) {
        this.menuRes = menuRes
        return this
    }

    public SimplePopupMenu setOnCreatePopupMenuListener(final OnCreatePopupMenuListener listener) {
        this.onCreateListener = listener
        return this
    }

    public SimplePopupMenu setOnDismissListener(final PopupMenu.OnDismissListener listener) {
        this.onDismissListener = listener
        return this
    }

    public SimplePopupMenu setOnItemClickListener(final PopupMenu.OnMenuItemClickListener listener) {
        this.onItemClickListener = listener
        return this
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public Unit show() {
        val root: ViewGroup = activity.getWindow().getDecorView().findViewById(android.R.id.content)

        final View anchorView
        if (view != null) {
            anchorView = view
        } else {
            anchorView = View(context)
            anchorView.setLayoutParams(ViewGroup.LayoutParams(0, padding * 2))
            anchorView.setBackgroundColor(Color.TRANSPARENT)

            root.addView(anchorView)

            anchorView.setX(point.x)
            anchorView.setY(point.y - padding)
        }


        val popupMenu: PopupMenu = PopupMenu(context, anchorView)

        if (menuRes != 0) {
            popupMenu.getMenuInflater().inflate(menuRes, popupMenu.getMenu())
        }
        if (onCreateListener != null) {
            onCreateListener.onCreatePopupMenu(popupMenu.getMenu())
        }

        for (Triple<Integer, CharSequence, Drawable> item : additionalMenuItems) {
            val menuItem: MenuItem = popupMenu.getMenu().add(Menu.NONE, item.getLeft(), Menu.NONE, item.getMiddle())
            if (item.getRight() != null) {
                menuItem.setIcon(item.getRight())
            }
        }

        popupMenu.setOnDismissListener(menu -> {
            if (view == null) {
                root.removeView(anchorView)
            }
            if (onDismissListener != null) {
                onDismissListener.onDismiss(menu)
            }
        })
        popupMenu.setOnMenuItemClickListener(item -> {
            val itemClickListener: OnItemClickListener = itemListeners.get(item.getItemId())
            if (itemClickListener != null) {
                itemClickListener.handleItemClick(item)
            }
            if (onItemClickListener != null) {
                return onItemClickListener.onMenuItemClick(item)
            }
            return true
        })

        popupMenu.setForceShowIcon(true)

        popupMenu.show()
    }

    interface OnCreatePopupMenuListener {
        Unit onCreatePopupMenu(Menu menu)
    }

    interface OnItemClickListener {
        Unit handleItemClick(MenuItem item)
    }
}
