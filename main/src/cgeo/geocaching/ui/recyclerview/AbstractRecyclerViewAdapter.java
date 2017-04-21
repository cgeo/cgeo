package cgeo.geocaching.ui.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;

/**
 * {@link Adapter} specialization to automatically store the current adapter position as view tag.
 *
 * @param <VH>
 *            View Holder
 */
public abstract class AbstractRecyclerViewAdapter<VH extends AbstractRecyclerViewHolder> extends RecyclerView.Adapter<VH> {

    @Override
    public void onBindViewHolder(final VH viewHolder, final int position) {
        viewHolder.setItemPosition(position);
    }

}
