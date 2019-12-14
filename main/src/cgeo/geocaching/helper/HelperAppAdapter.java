package cgeo.geocaching.helper;

import cgeo.geocaching.R;
import cgeo.geocaching.compatibility.Compatibility;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;

final class HelperAppAdapter extends RecyclerView.Adapter<HelperAppAdapter.ViewHolder> {

    @NonNull private final List<HelperApp> helperApps;
    @NonNull private final HelperAppClickListener clickListener;
    @NonNull private final Context context;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.title) protected TextView title;
        @BindView(R.id.image) protected ImageView image;
        @BindView(R.id.description) protected TextView description;

        ViewHolder(final View rowView) {
            super(rowView);
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
        holder.title.setText(resources.getString(app.titleId));
        holder.image.setImageDrawable(Compatibility.getDrawable(resources, app.iconId));
        holder.description.setText(Html.fromHtml(resources.getString(app.descriptionId)));
    }

}

