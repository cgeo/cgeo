package cgeo.geocaching.ui;

import cgeo.geocaching.activity.AbstractViewPagerActivity.PageViewCreator;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ListView;

/**
 * {@link PageViewCreator} for {@link ListView}, which can save scroll state on purging a page from the
 * {@link ViewPager}, and restore the state on re-recreation.
 * 
 */
public abstract class AbstractCachingListViewPageViewCreator extends AbstractCachingPageViewCreator<ListView> {
    private static final String STATE_POSITION_FROM_TOP = "positionFromTop";
    private static final String STATE_POSITION = "position";

    /**
     * Get the state of the current view
     *
     * @return the state encapsulated in a bundle
     */
    @Override
    public Bundle getViewState() {
        if (view == null) {
            return null;
        }
        int position = view.getFirstVisiblePosition();
        View child = view.getChildAt(0);
        int positionFromTop = (child == null) ? 0 : child.getTop();
        Bundle state = new Bundle();
        state.putInt(STATE_POSITION, position);
        state.putInt(STATE_POSITION_FROM_TOP, positionFromTop);
        return state;
    }

    /**
     * Restore a previously stored state of the view
     *
     */
    @Override
    public void setViewState(Bundle state) {
        if (view == null) {
            return;
        }
        int logViewPosition = state.getInt(STATE_POSITION);
        int logViewPositionFromTop = state.getInt(STATE_POSITION_FROM_TOP);
        view.setSelectionFromTop(logViewPosition, logViewPositionFromTop);
        return;
    }

}
