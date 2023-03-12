package cgeo.geocaching.ui;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.InfoItemBinding;
import cgeo.geocaching.models.InfoItem;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class CheckableItemSelectActivity extends AbstractActionBarActivity {

    private InfoItemListAdapter infoItemListAdapter;
    private @StringRes int prefKey;

    protected static class InfoItemViewHolder extends AbstractRecyclerViewHolder {
        private final InfoItemBinding binding;

        public InfoItemViewHolder(final View rowView) {
            super(rowView);
            binding = InfoItemBinding.bind(rowView);
        }
    }

    private static final class InfoItemListAdapter extends ManagedListAdapter<Element, InfoItemViewHolder> {

        private InfoItemListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView).setSupportDragDrop(true));
        }

        private void fillViewHolder(final InfoItemViewHolder holder, final Element item) {
            holder.binding.title.setText(item.item.getTitleResId());
            if (item.isSelected) {
                holder.binding.remove.setVisibility(View.VISIBLE);
                holder.binding.remove.setOnClickListener((view) -> {
                    item.isSelected = false;
                    fillViewHolder(holder, item);
                });
                holder.binding.add.setVisibility(View.GONE);
            } else {
                holder.binding.add.setVisibility(View.VISIBLE);
                holder.binding.add.setOnClickListener((view) -> {
                    item.isSelected = true;
                    fillViewHolder(holder, item);
                });
                holder.binding.remove.setVisibility(View.GONE);
            }
        }

        @NonNull
        @Override
        public InfoItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.info_item, parent, false);
            final InfoItemViewHolder viewHolder = new InfoItemViewHolder(view);
            registerStartDrag(viewHolder, viewHolder.binding.drag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final InfoItemViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }
    }

    private static final class Element {
        public InfoItem item;
        public boolean isSelected;

        Element(final InfoItem item, final boolean isSelected) {
            this.item = item;
            this.isSelected = isSelected;
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setUpNavigationEnabled(true);

        final Bundle bundle = getIntent().getExtras();
        setTitle(bundle.getInt(Intents.EXTRA_TITLE));
        this.prefKey = bundle.getInt(Intents.EXTRA_ID);
        final String infoItemClass = bundle.getString(Intents.EXTRA_CLASS);

        ArrayList<InfoItem> external = null;
        try {
            Class myClass = null;
            myClass = Class.forName(infoItemClass);
            final Field f = myClass.getDeclaredField(bundle.getString(Intents.EXTRA_FIELD));
            external = (ArrayList<InfoItem>) f.get(null);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException ignore) {
            //
        }
        assert (external != null);

        final RecyclerView listView = new RecyclerView(this, null);
        setContentView(listView);

        final ArrayList<Integer> includedItems = Settings.getInfoItems(prefKey, 0);

        final ArrayList<Element> allItems = new ArrayList<>();
        for (int i : includedItems) {
            allItems.add(new Element(InfoItem.getById(i, external), true));
        }
        for (InfoItem item : external) {
            boolean found = false;
            for (int i : includedItems) {
                if (item.getId() == i) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                allItems.add(new Element(item, false));
            }
        }

        infoItemListAdapter = new InfoItemListAdapter(listView);
        infoItemListAdapter.setItems(allItems);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        final ArrayList<Integer> selected = new ArrayList<>();
        for (Element item : infoItemListAdapter.getItems()) {
            if (item.isSelected) {
                selected.add(item.item.getId());
            }
        }
        Settings.setInfoItems(prefKey, selected);
        finish();
    }
}
