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
    private static final int AUTO_DISABLE_ON_IDLE = 1000;   // is ms

    private long mLastScroll = 0;
    private int mScrollState = 0;
    private int mFlingStartPos = -1;
    private ListView listView;

    public FastScrollListener(final ListView listView) {
        this.listView = listView;
    }

    @Override
    public void onScrollStateChanged(final AbsListView absListView, final int state) {
        if (state == SCROLL_STATE_IDLE && listView.isFastScrollEnabled()) {
            listView.postDelayed(() -> {
                if ((System.currentTimeMillis() - mLastScroll) > AUTO_DISABLE_ON_IDLE - 100) {
                    listView.setFastScrollEnabled(false);
                    mFlingStartPos = -1;
                }
            }, AUTO_DISABLE_ON_IDLE);
        }
        mScrollState = state;
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
        mLastScroll = System.currentTimeMillis();
        if (mScrollState == SCROLL_STATE_FLING && !listView.isFastScrollEnabled()) {
            if (mFlingStartPos < 0) {
                mFlingStartPos = firstVisibleItem;
            } else if (Math.abs(firstVisibleItem - mFlingStartPos) >= MIN_COVERED_ENTRIES) {
                // must have moved at least xx entries up or down in fling mode, before fastscroll gets enabled
                listView.setFastScrollEnabled(true);
            }
        }
    }
}
