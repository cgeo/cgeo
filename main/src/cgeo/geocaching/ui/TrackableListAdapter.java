package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;

public class TrackableListAdapter extends RecyclerView.Adapter<TrackableListAdapter.ViewHolder> {

    public interface TrackableClickListener {
        void onTrackableClicked(final Trackable trackable);
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
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Trackable trackable = trackables.get(position);

        holder.imageBrand.setImageResource(trackable.getIconBrand());
        holder.name.setText(Html.fromHtml(trackable.getName()).toString());
        if (trackable.isMissing()) {
            holder.name.setPaintFlags(holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                trackableClickListener.onTrackableClicked(trackable);
            }
        });
    }

}
