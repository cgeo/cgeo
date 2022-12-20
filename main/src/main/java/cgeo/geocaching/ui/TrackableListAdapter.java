package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.TrackableItemBinding;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.TextUtils;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrackableListAdapter extends RecyclerView.Adapter<TrackableListAdapter.ViewHolder> {

    public interface TrackableClickListener {
        void onTrackableClicked(Trackable trackable);
    }

    @NonNull private final List<Trackable> trackables;
    @NonNull private final TrackableClickListener trackableClickListener;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        private final TrackableItemBinding binding;

        ViewHolder(final View view) {
            super(view);
            binding = TrackableItemBinding.bind(view);
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
    @NonNull
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.trackable_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.itemView.setOnClickListener(view1 -> trackableClickListener.onTrackableClicked(trackables.get(viewHolder.getAdapterPosition())));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Trackable trackable = trackables.get(position);

        holder.binding.trackableImageBrand.setImageResource(trackable.getIconBrand());
        holder.binding.trackableName.setText(TextUtils.stripHtml(trackable.getName()));
        if (trackable.isMissing()) {
            holder.binding.trackableName.setPaintFlags(holder.binding.trackableName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

}
