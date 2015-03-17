package cgeo.geocaching.ui;

import butterknife.ButterKnife;

import android.view.View;

/**
 * Abstract super class for all view holders. It is responsible for the invocation of the view injection code and for
 * the tagging of views.
 *
 */
public abstract class AbstractViewHolder {

    protected AbstractViewHolder(final View view) {
        ButterKnife.inject(this, view);
        view.setTag(this);
    }

}
