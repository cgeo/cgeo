package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ActivityLogTemplatesBinding;
import cgeo.geocaching.databinding.LogTemplateItemBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TemplateTextPreference;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import static java.util.UUID.randomUUID;

import org.apache.commons.lang3.StringUtils;

/** Manages the log templates: add, edit, remove and reorder them via drag &amp; drop. All changes are stored immediately. */
public class LogTemplatesActivity extends AbstractActionBarActivity {

    private ActivityLogTemplatesBinding binding;
    private LogTemplateAdapter adapter;

    public static void startActivity(@NonNull final Context context) {
        context.startActivity(new Intent(context, LogTemplatesActivity.class));
    }

    protected static final class LogTemplateViewHolder extends AbstractRecyclerViewHolder {
        final LogTemplateItemBinding itemBinding;

        LogTemplateViewHolder(final View view) {
            super(view);
            itemBinding = LogTemplateItemBinding.bind(view);
        }
    }

    private final class LogTemplateAdapter extends ManagedListAdapter<Settings.PrefLogTemplate, LogTemplateViewHolder> {

        LogTemplateAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView).setSupportDragDrop(true));
        }

        @NonNull
        @Override
        public LogTemplateViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.log_template_item, parent, false);
            final LogTemplateViewHolder holder = new LogTemplateViewHolder(view);
            registerStartDrag(holder, holder.itemBinding.dragHandle);
            holder.itemBinding.templateContent.setOnClickListener(v -> {
                final Settings.PrefLogTemplate template = getItem(holder.getBindingAdapterPosition());
                if (template != null) {
                    editTemplate(template);
                }
            });
            holder.itemBinding.templateDelete.setOnClickListener(v -> {
                final Settings.PrefLogTemplate template = getItem(holder.getBindingAdapterPosition());
                if (template != null) {
                    removeTemplate(template);
                }
            });
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
        setTitle(R.string.init_log_templates);

        binding = ActivityLogTemplatesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new LogTemplateAdapter(binding.logTemplateList);
        binding.addTemplate.setOnClickListener(v -> editTemplate(new Settings.PrefLogTemplate(randomUUID().toString(), "", "")));
        setTemplates(Settings.getLogTemplates());
    }

    @Override
    public void onPause() {
        // persist order changed via drag & drop
        Settings.putLogTemplates(new ArrayList<>(adapter.getItems()));
        super.onPause();
    }

    private void editTemplate(@NonNull final Settings.PrefLogTemplate template) {
        TemplateTextPreference.showEditDialog(this, false, template.getTitle(), template.getText(), (title, text) -> {
            final List<Settings.PrefLogTemplate> templates = new ArrayList<>(adapter.getItems());
            final Settings.PrefLogTemplate changed = new Settings.PrefLogTemplate(template.getKey(), title, text);
            final int index = templates.indexOf(changed);
            if (index >= 0) {
                templates.set(index, changed);
            } else {
                templates.add(changed);
            }
            storeTemplates(templates);
        });
    }

    private void removeTemplate(@NonNull final Settings.PrefLogTemplate template) {
        SimpleDialog.of(this).setTitle(R.string.init_log_template).setMessage(R.string.init_log_template_remove_confirm).confirm(() -> {
            final List<Settings.PrefLogTemplate> templates = new ArrayList<>(adapter.getItems());
            templates.remove(template);
            storeTemplates(templates);
        });
    }

    private void storeTemplates(@NonNull final List<Settings.PrefLogTemplate> templates) {
        Settings.putLogTemplates(templates);
        setTemplates(templates);
    }

    private void setTemplates(@NonNull final List<Settings.PrefLogTemplate> templates) {
        adapter.setItems(templates);
        final boolean empty = templates.isEmpty();
        binding.logTemplateList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
