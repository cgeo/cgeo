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

import cgeo.geocaching.R
import cgeo.geocaching.databinding.TrackableItemBinding
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.utils.TextUtils

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import java.util.List

class TrackableListAdapter : RecyclerView().Adapter<TrackableListAdapter.ViewHolder> {

    interface TrackableClickListener {
        Unit onTrackableClicked(Trackable trackable)
    }

    private final List<Trackable> trackables
    private final TrackableClickListener trackableClickListener

    protected static class ViewHolder : AbstractRecyclerViewHolder() {
        private final TrackableItemBinding binding

        ViewHolder(final View view) {
            super(view)
            binding = TrackableItemBinding.bind(view)
        }
    }

    public TrackableListAdapter(final List<Trackable> trackables, final TrackableClickListener trackableClickListener) {
        this.trackables = trackables
        this.trackableClickListener = trackableClickListener
    }

    override     public Int getItemCount() {
        return trackables.size()
    }

    override     public ViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
        val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.trackable_item, parent, false)
        val viewHolder: ViewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener(view1 -> trackableClickListener.onTrackableClicked(trackables.get(viewHolder.getAdapterPosition())))
        return viewHolder
    }

    override     public Unit onBindViewHolder(final ViewHolder holder, final Int position) {
        val trackable: Trackable = trackables.get(position)

        holder.binding.trackableImageBrand.setImageResource(trackable.getIconBrand())
        holder.binding.trackableName.setText(TextUtils.stripHtml(trackable.getName()))
        if (trackable.isMissing()) {
            holder.binding.trackableName.setPaintFlags(holder.binding.trackableName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG)
        }
    }

}
