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

import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.functions.Action1

import android.app.Activity

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.List
import java.util.function.Consumer

/**
 * Represents a dialog usable as a context menu for various elements.
 * <br>
 * Supports textual items with or without accompanying icon.
 */
class ContextMenuDialog {

    private final Activity activity
    private String title

    private val items: List<Item> = ArrayList<>()
    private Consumer<Integer> dialogClickAction

    public ContextMenuDialog setTitle(final String title) {
        this.title = title
        return this
    }

    public ContextMenuDialog(final Activity activity) {
        this.activity = activity
    }

    public ContextMenuDialog addItem(final String text) {
        addItem(text, 0)
        return this
    }

    public ContextMenuDialog addItem(final String text, @DrawableRes final Int icon) {
        addItem(text, icon, null)
        return this
    }

    public ContextMenuDialog addItem(@StringRes final Int textId, @DrawableRes final Int icon, final Action1<Item> selectAction) {
        items.add(Item(this.activity.getResources().getString(textId), icon, selectAction))
        return this
    }

    public ContextMenuDialog addItem(final Int pos, @StringRes final Int textId, @DrawableRes final Int icon, final Action1<Item> selectAction) {
        items.add(pos, Item(this.activity.getResources().getString(textId), icon, selectAction))
        return this
    }

    public ContextMenuDialog addItem(final String text, @DrawableRes final Int icon, final Action1<Item> selectAction) {
        items.add(Item(text, icon, selectAction))
        return this
    }

    public ContextMenuDialog setOnClickAction(final Consumer<Integer> clickAction) {
        this.dialogClickAction = clickAction
        return this
    }

    public Unit show() {

        val atLeastOneElementHasIcon: Boolean = CollectionStream.of(items).filter(i -> i.icon > 0).count() > 0

        val clickListener: Consumer<Item> = (it) -> {
            if (it.selectAction != null) {
                it.selectAction.call(it)
            }
            if (dialogClickAction != null) {
                val pos: Int = this.items.indexOf(it)
                if (pos >= 0) {
                    dialogClickAction.accept(pos)
                }
            }
        }

        final SimpleDialog.ItemSelectModel<ContextMenuDialog.Item> model = SimpleDialog.ItemSelectModel<>()
        model
            .setItems(items)
            .setDisplayMapper((it) -> TextParam.text(it.toString()).setImage(atLeastOneElementHasIcon ? ImageParam.id(it.icon) : null, atLeastOneElementHasIcon ? 30 : 0))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)

        SimpleDialog.of(this.activity).setTitle(this.title == null ? null : TextParam.text(this.title))
                .selectSingle(model, clickListener)
    }


    public static class Item {
        public final String text
        public final Int icon
        public final Action1<Item> selectAction

        public Item(final String text, @DrawableRes final Int icon, final Action1<Item> selectAction) {
            this.text = text
            this.icon = icon
            this.selectAction = selectAction
        }

        override         public String toString() {
            return text
        }
    }

}
