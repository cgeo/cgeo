package cgeo.geocaching.ui;

import butterknife.Views;

import android.view.View;

/**
 * Abstract super class for all view holders. It is responsible for the invocation of the view injection code and for
 * the tagging of views.
 *
 */
public abstract class AbstractViewHolder {

    protected AbstractViewHolder(View view) {
        Views.inject(this, view);
        view.setTag(this);
    }

}
