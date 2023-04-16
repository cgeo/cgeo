package cgeo.geocaching.ui;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.CheckableItemSelectActivityBinding;
import cgeo.geocaching.databinding.InfoItemBinding;
import cgeo.geocaching.models.InfoItem;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.DisplayUtils;

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

    private static final class InfoItemListAdapter extends ManagedListAdapter<InfoItem, InfoItemViewHolder> {

        private InfoItemListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView).setSupportDragDrop(true));
        }

        private void fillViewHolder(final InfoItemViewHolder holder, final InfoItem item) {
            if (holder.getItemViewType() != 0) {
                holder.binding.title.setText(item.getTitleResId());
                holder.binding.drag.setVisibility(View.VISIBLE);
            } else {
                holder.binding.title.setText(R.string.disabled_elements);
            }
        }

        @NonNull
        @Override
        public InfoItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(viewType == 0 ? R.layout.info_header : R.layout.info_item, parent, false);
            if (viewType != 0) {
                view.setPaddingRelative(DisplayUtils.getPxFromDp(CgeoApplication.getInstance().getResources(), 20f, 1f), 0, 0, 0);
            }
            final InfoItemViewHolder viewHolder = new InfoItemViewHolder(view);
            registerStartDrag(viewHolder, viewHolder.binding.drag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final InfoItemViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }

        @Override
        public int getItemViewType(final int position) {
            return getItem(position) == null ? 0 : 1;
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setUpNavigationEnabled(true);
        final CheckableItemSelectActivityBinding binding = CheckableItemSelectActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.headerSelected.title.setText(R.string.active_elements);

        final Bundle bundle = getIntent().getExtras();
        setTitle(bundle.getInt(Intents.EXTRA_TITLE));
        this.prefKey = bundle.getInt(Intents.EXTRA_ID);
        final String infoItemClass = bundle.getString(Intents.EXTRA_CLASS);

        ArrayList<InfoItem> external = null;
        try {
            final Field f = Class.forName(infoItemClass).getDeclaredField(bundle.getString(Intents.EXTRA_FIELD));
            external = (ArrayList<InfoItem>) f.get(null);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException ignore) {
        }
        assert (external != null);

        final ArrayList<Integer> includedItems = Settings.getInfoItems(prefKey, 0);

        final ArrayList<InfoItem> allItems = new ArrayList<>();
        for (int i : includedItems) {
            allItems.add(InfoItem.getById(i, external));
        }
        allItems.add(null); // marker for: disabled elements from here
        for (InfoItem item : external) {
            boolean found = false;
            for (int i : includedItems) {
                if (item.getId() == i) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                allItems.add(item);
            }
        }

        infoItemListAdapter = new InfoItemListAdapter(binding.recyclerView);
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
        for (InfoItem item : infoItemListAdapter.getItems()) {
            if (item == null) {
                break;
            }
            selected.add(item.getId());
        }
        Settings.setInfoItems(prefKey, selected);
        finish();
    }
}
