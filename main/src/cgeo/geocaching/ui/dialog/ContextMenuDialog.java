package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.functions.Action1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a dialog usable as a context menu for various elements.
 *
 * Supports textual items with or without accompanying icon.
 */
public class ContextMenuDialog {

    private final Context context;
    private String title;

    private final List<Item> items = new ArrayList<>();
    private DialogInterface.OnClickListener dialogClickListener;

    public ContextMenuDialog setTitle(final String title) {
        this.title = title;
        return this;
    }

    public ContextMenuDialog(final Context context) {
        this.context = context;
    }

    public ContextMenuDialog addItem(final String text) {
        addItem(text, 0);
        return this;
    }

    public ContextMenuDialog addItem(final String text, @DrawableRes final int icon) {
        addItem(text, icon, null);
        return this;
    }

    public ContextMenuDialog addItem(@StringRes final int textId, @DrawableRes final int icon, final Action1<Item> selectAction) {
        items.add(new Item(this.context.getResources().getString(textId), icon, selectAction));
        return this;
    }

    public ContextMenuDialog addItem(final String text, @DrawableRes final int icon, final Action1<Item> selectAction) {
        items.add(new Item(text, icon, selectAction));
        return this;
    }

    public ContextMenuDialog setOnClickListener(final DialogInterface.OnClickListener dialogClickListener) {
        this.dialogClickListener = dialogClickListener;
        return this;
    }

    public void show() {

        final AlertDialog.Builder alert = new AlertDialog.Builder(this.context);
        if (this.title != null) {
            alert.setTitle(this.title);
        }

        final boolean atLeastOneElementHasIcon = CollectionStream.of(items).filter(i -> i.icon > 0).count() > 0;

        final DialogInterface.OnClickListener clickListener = (dialog, pos) -> {
            dialog.dismiss();
            final Item it = this.items.get(pos);
            if (it.selectAction != null) {
                it.selectAction.call(it);
            }
            if (dialogClickListener != null) {
                dialogClickListener.onClick(dialog, pos);
            }
        };

        if (atLeastOneElementHasIcon) {

            final ListAdapter adapter = new ArrayAdapter<Item>(
                    this.context,
                    android.R.layout.simple_list_item_1, //NOT android.R.layout.select_dialog_item -> makes font size too big
                    android.R.id.text1,
                    items) {
                public View getView(final int position, final View convertView, final ViewGroup parent) {
                    //Use super class to create the View. This also sets the text in field text1
                    final View v = super.getView(position, convertView, parent);

                    final TextView tv = (TextView) v.findViewById(android.R.id.text1);

                    //Put the image on the TextView
                    int iconId = items.get(position).icon;
                    if (iconId <= 0) {
                        //the following drawable is completety transient and only used to create an empty block before the text
                        iconId = android.R.color.transparent;
                    }

                    final int px = Math.round(40 * context.getResources().getDisplayMetrics().density);
                    final Drawable d = ResourcesCompat.getDrawable(context.getResources(), iconId, context.getTheme());
                    d.setBounds(new Rect(0, 0, px, px));
                    tv.setCompoundDrawables(d, null, null, null);

                    //Add margin between image and text (support various screen densities)
                    final int dp5 = (int) (5 * this.getContext().getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);

                    return v;
                }
            };
            alert.setAdapter(adapter, clickListener);
        } else {
            alert.setItems(CollectionStream.of(items).map(i -> i.toString()).toArray(String.class), clickListener);
        }

        alert.create().show();
    }


    public static class Item {
        public final String text;
        public final int icon;
        public final Action1<Item> selectAction;

        public Item(final String text, @DrawableRes final int icon, final Action1<Item> selectAction) {
            this.text = text;
            this.icon = icon;
            this.selectAction = selectAction;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
