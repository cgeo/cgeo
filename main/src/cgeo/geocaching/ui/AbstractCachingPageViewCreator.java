package cgeo.geocaching.ui;

import cgeo.geocaching.activity.AbstractViewPagerActivity.PageViewCreator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.os.Bundle;
import android.view.View;

/**
 * View creator which destroys the created view on every {@link #notifyDataSetChanged()}.
 *
 * @param <ViewClass>
 */
public abstract class AbstractCachingPageViewCreator<ViewClass extends View> implements PageViewCreator {

    public ViewClass view;

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
    @SuppressFBWarnings("USM_USELESS_ABSTRACT_METHOD")
    public abstract ViewClass getDispatchedView();

    /**
     * Gets the state of the view but returns an empty state if not overridden
     *
     * @return empty bundle
     */
    @Override
    public @Nullable
    Bundle getViewState() {
        return new Bundle();
    }

    /**
     * Restores the state of the view but just returns if not overridden.
     */
    @Override
    public void setViewState(@NonNull Bundle state) {
        return;
    }
}
