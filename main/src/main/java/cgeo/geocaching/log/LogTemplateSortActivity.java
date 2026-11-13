package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ActivityLogTemplateSortBinding;
import cgeo.geocaching.databinding.LogTemplateSortItemBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

/** Lets the user reorder the log templates via drag &amp; drop */
public class LogTemplateSortActivity extends AbstractActionBarActivity {

    private LogTemplateAdapter adapter;

    public static void startActivity(@NonNull final Context context) {
        context.startActivity(new Intent(context, LogTemplateSortActivity.class));
    }

    protected static final class LogTemplateViewHolder extends AbstractRecyclerViewHolder {
        final LogTemplateSortItemBinding itemBinding;

        LogTemplateViewHolder(final View view) {
            super(view);
            itemBinding = LogTemplateSortItemBinding.bind(view);
        }
    }

    private static final class LogTemplateAdapter extends ManagedListAdapter<Settings.PrefLogTemplate, LogTemplateViewHolder> {

        LogTemplateAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView).setSupportDragDrop(true));
        }

        @NonNull
        @Override
        public LogTemplateViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_template_sort_item, parent, false);
            final LogTemplateViewHolder holder = new LogTemplateViewHolder(view);
            registerStartDrag(holder, holder.itemBinding.dragHandle);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull final LogTemplateViewHolder holder, final int position) {
            final Settings.PrefLogTemplate item = getItem(position);
            if (item == null) {
                return;
            }
            holder.itemBinding.templateTitle.setText(item.getTitle());
            holder.itemBinding.templateText.setText(StringUtils.normalizeSpace(item.getText()));
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(R.string.init_log_template_sort_title);

        final ActivityLogTemplateSortBinding binding = ActivityLogTemplateSortBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new LogTemplateAdapter(binding.logTemplateList);
        adapter.setItems(Settings.getLogTemplates());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_named_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_save) {
            Settings.putLogTemplates(new ArrayList<>(adapter.getItems()));
            finish();
            return true;
        } else if (itemId == R.id.menu_item_cancel || itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
