package cgeo.geocaching.ui;

import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.view.View;

/**
 * Abstract super class for all view holders. It is responsible for the invocation of the view injection code and for
 * the tagging of views.
 *
 * TODO: Use {@link AbstractRecyclerViewHolder} and the recycler view instead.
 */
public abstract class AbstractViewHolder {

    protected AbstractViewHolder(final View view) {
        view.setTag(this);
    }

}
