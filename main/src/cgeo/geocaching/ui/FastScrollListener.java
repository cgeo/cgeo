package cgeo.geocaching.ui;

import android.widget.AbsListView;
import android.widget.ListView;

/**
 * Tame default fast scroll behavior a bit:
 * - only activate fast scroll when in FLING mode, not in standard scroll mode;
 * - and only acticate after having covered a certain minimum amount of entries (3) in FLING mode
 * - automatically disable after a certain amount of time (1s) without scrolling
 */
public class FastScrollListener implements AbsListView.OnScrollListener {
    private static final int MIN_COVERED_ENTRIES = 3;       // in number of entries
    private static final int AUTO_DISABLE_ON_IDLE = 500;    // is ms

    private long mLastScroll = 0;
    private int mScrollState = 0;
    private int mFlingStartPos = -1;
    private int mLastFirstVisibleItem = -1;
    private final ListView listView;

    public FastScrollListener(final ListView listView) {
        this.listView = listView;
    }

    @Override
    public void onScrollStateChanged(final AbsListView absListView, final int state) {
        mScrollState = state;
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        mLastScroll = System.currentTimeMillis();
        mLastFirstVisibleItem = firstVisibleItem;
        if (mScrollState == SCROLL_STATE_FLING && !listView.isFastScrollEnabled()) {
            if (mFlingStartPos < 0) {
                mFlingStartPos = firstVisibleItem;
            } else if (Math.abs(firstVisibleItem - mFlingStartPos) >= MIN_COVERED_ENTRIES) {
                // must have moved at least xx entries up or down in fling mode, before fastscroll gets enabled
                listView.setFastScrollEnabled(true);
                listView.setFastScrollAlwaysVisible(true);
                listView.postDelayed(this::checkScrollState, AUTO_DISABLE_ON_IDLE);
            }
        }
    }

    private void checkScrollState() {
        if (listView.getFirstVisiblePosition() != mLastFirstVisibleItem || (Math.abs(System.currentTimeMillis() - mLastScroll) < AUTO_DISABLE_ON_IDLE)) {
            listView.postDelayed(this::checkScrollState, AUTO_DISABLE_ON_IDLE);
        } else {
            listView.setFastScrollEnabled(false);
            listView.setFastScrollAlwaysVisible(false);
            mFlingStartPos = -1;
        }
    }
}
