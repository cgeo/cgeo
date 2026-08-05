package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a dialog usable as a context menu for various elements.
 * <br>
 * Supports textual items with or without accompanying icon.
 */
public class ContextMenuDialog {

    private final Activity activity;
    private String title;

    private final List<Item> items = new ArrayList<>();
    private Action1<Integer> dialogClickAction;

    public ContextMenuDialog setTitle(final String title) {
        this.title = title;
        return this;
    }

    public ContextMenuDialog(final Activity activity) {
        this.activity = activity;
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
        items.add(new Item(this.activity.getResources().getString(textId), icon, selectAction));
        return this;
    }

    public ContextMenuDialog addItem(final int pos, @StringRes final int textId, @DrawableRes final int icon, final Action1<Item> selectAction) {
        items.add(pos, new Item(this.activity.getResources().getString(textId), icon, selectAction));
        return this;
    }

    public ContextMenuDialog addItem(final String text, @DrawableRes final int icon, final Action1<Item> selectAction) {
        items.add(new Item(text, icon, selectAction));
        return this;
    }

    public ContextMenuDialog setOnClickAction(final Action1<Integer> clickAction) {
        this.dialogClickAction = clickAction;
        return this;
    }

    public void show() {

        final boolean atLeastOneElementHasIcon = CollectionStream.of(items).filter(i -> i.icon > 0).count() > 0;

        final Consumer<Item> clickListener = (it) -> {
            if (it.selectAction != null) {
                it.selectAction.call(it);
            }
            if (dialogClickAction != null) {
                final int pos = this.items.indexOf(it);
                if (pos >= 0) {
                    dialogClickAction.call(pos);
                }
            }
        };

        final SimpleDialog.ItemSelectModel<ContextMenuDialog.Item> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(items)
            .setDisplayMapper((it) -> TextParam.text(it.toString()).setImage(atLeastOneElementHasIcon ? ImageParam.id(it.icon) : null, atLeastOneElementHasIcon ? 30 : 0))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

        SimpleDialog.of(this.activity).setTitle(this.title == null ? null : TextParam.text(this.title))
                .selectSingle(model, clickListener);
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
