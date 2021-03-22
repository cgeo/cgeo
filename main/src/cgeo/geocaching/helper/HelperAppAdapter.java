package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.UsefulappsItemBinding;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

final class HelperAppAdapter extends RecyclerView.Adapter<HelperAppAdapter.ViewHolder> {

    @NonNull private final List<HelperApp> helperApps;
    @NonNull private final HelperAppClickListener clickListener;
    @NonNull private final Context context;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        private final UsefulappsItemBinding binding;

        ViewHolder(final View rowView) {
            super(rowView);
            binding = UsefulappsItemBinding.bind(rowView);
        }
    }

    HelperAppAdapter(@NonNull final Context context, @NonNull final HelperApp[] objects, @NonNull final HelperAppClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
        helperApps = Arrays.asList(objects);
    }

    @Override
    public int getItemCount() {
        return helperApps.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.usefulapps_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.itemView.setOnClickListener(view1 -> {
            final HelperApp app = helperApps.get(viewHolder.getAdapterPosition());
            clickListener.onClickHelperApp(app);
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final HelperApp app = helperApps.get(position);
        final Resources resources = context.getResources();
        holder.binding.title.setText(resources.getString(app.titleId));
        holder.binding.image.setImageDrawable(ResourcesCompat.getDrawable(resources, app.iconId, null));
        holder.binding.description.setText(HtmlCompat.fromHtml(resources.getString(app.descriptionId), HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

}

