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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.databinding.CheckableItemSelectActivityBinding
import cgeo.geocaching.databinding.InfoItemBinding
import cgeo.geocaching.models.InfoItem
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.DisplayUtils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView

import java.lang.reflect.Field
import java.util.ArrayList

class CheckableItemSelectActivity : AbstractActionBarActivity() {

    private InfoItemListAdapter infoItemListAdapter
    private @StringRes Int prefKey

    protected static class InfoItemViewHolder : AbstractRecyclerViewHolder() {
        private final InfoItemBinding binding

        public InfoItemViewHolder(final View rowView) {
            super(rowView)
            binding = InfoItemBinding.bind(rowView)
        }
    }

    private static class InfoItemListAdapter : ManagedListAdapter()<InfoItem, InfoItemViewHolder> {

        private InfoItemListAdapter(final RecyclerView recyclerView) {
            super(ManagedListAdapter.Config(recyclerView).setSupportDragDrop(true))
        }

        private Unit fillViewHolder(final InfoItemViewHolder holder, final InfoItem item) {
            if (holder.getItemViewType() != 0) {
                holder.binding.title.setText(item.getTitleResId())
                holder.binding.drag.setVisibility(View.VISIBLE)
            } else {
                holder.binding.title.setText(R.string.disabled_elements)
            }
        }

        override         public InfoItemViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(viewType == 0 ? R.layout.info_header : R.layout.info_item, parent, false)
            if (viewType != 0) {
                view.setPaddingRelative(DisplayUtils.getPxFromDp(CgeoApplication.getInstance().getResources(), 20f, 1f), 0, 0, 0)
            }
            val viewHolder: InfoItemViewHolder = InfoItemViewHolder(view)
            registerStartDrag(viewHolder, viewHolder.binding.drag)
            return viewHolder
        }

        override         public Unit onBindViewHolder(final InfoItemViewHolder holder, final Int position) {
            fillViewHolder(holder, getItem(position))
        }

        override         public Int getItemViewType(final Int position) {
            return getItem(position) == null ? 0 : 1
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()
        setUpNavigationEnabled(true)
        val binding: CheckableItemSelectActivityBinding = CheckableItemSelectActivityBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())
        binding.headerSelected.title.setText(R.string.active_elements)

        val bundle: Bundle = getIntent().getExtras()
        setTitle(bundle.getInt(Intents.EXTRA_TITLE))
        this.prefKey = bundle.getInt(Intents.EXTRA_ID)
        val infoItemClass: String = bundle.getString(Intents.EXTRA_CLASS)

        ArrayList<InfoItem> external = null
        try {
            val f: Field = Class.forName(infoItemClass).getDeclaredField(bundle.getString(Intents.EXTRA_FIELD))
            external = (ArrayList<InfoItem>) f.get(null)
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException ignore) {
        }
        assert (external != null)

        val includedItems: ArrayList<Integer> = Settings.getInfoItems(prefKey, 0)

        val allItems: ArrayList<InfoItem> = ArrayList<>()
        for (Int i : includedItems) {
            allItems.add(InfoItem.getById(i, external))
        }
        allItems.add(null); // marker for: disabled elements from here
        for (InfoItem item : external) {
            Boolean found = false
            for (Int i : includedItems) {
                if (item.getId() == i) {
                    found = true
                    break
                }
            }
            if (!found) {
                allItems.add(item)
            }
        }

        infoItemListAdapter = InfoItemListAdapter(binding.recyclerView)
        infoItemListAdapter.setItems(allItems)
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override     public Unit onBackPressed() {
        // @todo should be replaced by setting a OnBackPressedDispatcher
        val selected: ArrayList<Integer> = ArrayList<>()
        for (InfoItem item : infoItemListAdapter.getItems()) {
            if (item == null) {
                break
            }
            selected.add(item.getId())
        }
        Settings.setInfoItems(prefKey, selected)
        finish()
        super.onBackPressed()
    }
}
