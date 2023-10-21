package cgeo.geocaching.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

public class SimplePopupMenu {
    private final Context context;

    private final Map<Integer, OnItemClickListener> itemListeners = new HashMap<>();
    private final List<Triple<Integer, CharSequence, Drawable>> additionalMenuItems = new ArrayList<>();

    private Activity activity;
    private Point point;
    private int padding;

    private View view;

    private @MenuRes int menuRes;

    private OnCreatePopupMenuListener onCreateListener;
    private PopupMenu.OnDismissListener onDismissListener;
    private PopupMenu.OnMenuItemClickListener onItemClickListener;

    private SimplePopupMenu(final Context context) {
        this.context = context;
    }

    public static SimplePopupMenu forView(final View view) {
        final SimplePopupMenu popupMenu = new SimplePopupMenu(view.getContext());
        popupMenu.view = view;
        return popupMenu;
    }

    public static SimplePopupMenu of(final Activity activity) {
        final SimplePopupMenu popupMenu = new SimplePopupMenu(activity);
        popupMenu.activity = activity;
        return popupMenu;
    }

    /**
     * Will only have an effect if no view is set as anchor. Both values are in px.
     */
    public SimplePopupMenu setPosition(final Point point, final int padding) {
        this.point = point;
        this.padding = padding;
        return this;
    }

    public SimplePopupMenu addItemClickListener(final @IdRes int id, final OnItemClickListener listener) {
        this.itemListeners.put(id, listener);
        return this;
    }

    public SimplePopupMenu addMenuItem(final int uniqueId, final CharSequence title) {
        this.additionalMenuItems.add(Triple.of(uniqueId, title, null));
        return this;
    }

    public SimplePopupMenu addMenuItem(final int uniqueId, final CharSequence title, final Drawable icon) {
        this.additionalMenuItems.add(Triple.of(uniqueId, title, icon));
        return this;
    }

    public SimplePopupMenu addMenuItem(final int uniqueId, final CharSequence title, final @DrawableRes int icon) {
        this.additionalMenuItems.add(Triple.of(uniqueId, title, AppCompatResources.getDrawable(context, icon)));
        return this;
    }

    public SimplePopupMenu setMenuContent(final @MenuRes int menuRes) {
        this.menuRes = menuRes;
        return this;
    }

    public SimplePopupMenu setOnCreatePopupMenuListener(final OnCreatePopupMenuListener listener) {
        this.onCreateListener = listener;
        return this;
    }

    public SimplePopupMenu setOnDismissListener(final PopupMenu.OnDismissListener listener) {
        this.onDismissListener = listener;
        return this;
    }

    public SimplePopupMenu setOnItemClickListener(final PopupMenu.OnMenuItemClickListener listener) {
        this.onItemClickListener = listener;
        return this;
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public void show() {
        final ViewGroup root = activity.getWindow().getDecorView().findViewById(android.R.id.content);

        final View anchorView;
        if (view != null) {
            anchorView = view;
        } else {
            anchorView = new View(context);
            anchorView.setLayoutParams(new ViewGroup.LayoutParams(0, padding * 2));
            anchorView.setBackgroundColor(Color.TRANSPARENT);

            root.addView(anchorView);

            anchorView.setX(point.x);
            anchorView.setY(point.y - padding);
        }


        final PopupMenu popupMenu = new PopupMenu(context, anchorView);

        if (menuRes != 0) {
            popupMenu.getMenuInflater().inflate(menuRes, popupMenu.getMenu());
        }
        if (onCreateListener != null) {
            onCreateListener.onCreatePopupMenu(popupMenu.getMenu());
        }

        for (Triple<Integer, CharSequence, Drawable> item : additionalMenuItems) {
            final MenuItem menuItem = popupMenu.getMenu().add(Menu.NONE, item.getLeft(), Menu.NONE, item.getMiddle());
            if (item.getRight() != null) {
                menuItem.setIcon(item.getRight());
            }
        }

        popupMenu.setOnDismissListener(menu -> {
            if (view == null) {
                root.removeView(anchorView);
            }
            if (onDismissListener != null) {
                onDismissListener.onDismiss(menu);
            }
        });
        popupMenu.setOnMenuItemClickListener(item -> {
            final OnItemClickListener itemClickListener = itemListeners.get(item.getItemId());
            if (itemClickListener != null) {
                itemClickListener.handleItemClick(item);
            }
            if (onItemClickListener != null) {
                return onItemClickListener.onMenuItemClick(item);
            }
            return true;
        });

        popupMenu.setForceShowIcon(true);

        popupMenu.show();
    }

    public interface OnCreatePopupMenuListener {
        void onCreatePopupMenu(Menu menu);
    }

    public interface OnItemClickListener {
        void handleItemClick(MenuItem item);
    }
}
