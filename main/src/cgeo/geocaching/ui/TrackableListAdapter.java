package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.TextUtils;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;

public class TrackableListAdapter extends RecyclerView.Adapter<TrackableListAdapter.ViewHolder> {

    public interface TrackableClickListener {
        void onTrackableClicked(Trackable trackable);
    }

    @NonNull private final List<Trackable> trackables;
    @NonNull private final TrackableClickListener trackableClickListener;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.trackable_image_brand) ImageView imageBrand;
        @BindView(R.id.trackable_name) TextView name;

        ViewHolder(final View view) {
            super(view);
        }
    }

    public TrackableListAdapter(@NonNull final List<Trackable> trackables, @NonNull final TrackableClickListener trackableClickListener) {
        this.trackables = trackables;
        this.trackableClickListener = trackableClickListener;
    }

    @Override
    public int getItemCount() {
        return trackables.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.trackable_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.itemView.setOnClickListener(view1 -> trackableClickListener.onTrackableClicked(trackables.get(viewHolder.getAdapterPosition())));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Trackable trackable = trackables.get(position);

        holder.imageBrand.setImageResource(trackable.getIconBrand());
        holder.name.setText(TextUtils.stripHtml(trackable.getName()));
        if (trackable.isMissing()) {
            holder.name.setPaintFlags(holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

}
