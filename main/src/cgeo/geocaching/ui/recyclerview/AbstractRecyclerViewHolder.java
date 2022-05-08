package cgeo.geocaching.ui.recyclerview;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Abstract super class for all view holders. It is responsible for the invocation of the view injection code.
 */
public abstract class AbstractRecyclerViewHolder extends RecyclerView.ViewHolder {

    protected AbstractRecyclerViewHolder(final View view) {
        super(view);
    }

}
