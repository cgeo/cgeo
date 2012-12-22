package cgeo.geocaching.ui;

import cgeo.geocaching.activity.AbstractViewPagerActivity.PageViewCreator;

import android.view.View;

/**
 * View creator which destroys the created view on every {@link #notifyDataSetChanged()}.
 *
 * @param <ViewClass>
 */
public abstract class AbstractCachingPageViewCreator<ViewClass extends View> implements PageViewCreator {

    protected ViewClass view;

    @Override
    public final void notifyDataSetChanged() {
        view = null;
    }

    @Override
    public final View getView() {
        if (view == null) {
            view = getDispatchedView();
        }

        return view;
    }

    @Override
    public abstract ViewClass getDispatchedView();
}
