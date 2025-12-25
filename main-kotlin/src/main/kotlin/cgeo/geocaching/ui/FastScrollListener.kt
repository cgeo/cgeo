// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import android.widget.AbsListView
import android.widget.ListView

/**
 * Tame default fast scroll behavior a bit:
 * - only activate fast scroll when in FLING mode, not in standard scroll mode
 * - and only acticate after having covered a certain minimum amount of entries (3) in FLING mode
 * - automatically disable after a certain amount of time (1s) without scrolling
 */
class FastScrollListener : AbsListView.OnScrollListener {
    private static val MIN_COVERED_ENTRIES: Int = 3;       // in number of entries
    private static val AUTO_DISABLE_ON_IDLE: Int = 500;    // is ms

    private var mLastScroll: Long = 0
    private var mScrollState: Int = 0
    private var mFlingStartPos: Int = -1
    private var mLastFirstVisibleItem: Int = -1
    private final ListView listView

    public FastScrollListener(final ListView listView) {
        this.listView = listView
    }

    override     public Unit onScrollStateChanged(final AbsListView absListView, final Int state) {
        mScrollState = state
    }

    override     public Unit onScroll(final AbsListView view, final Int firstVisibleItem, final Int visibleItemCount, final Int totalItemCount) {
        mLastScroll = System.currentTimeMillis()
        mLastFirstVisibleItem = firstVisibleItem
        if (mScrollState == SCROLL_STATE_FLING && !listView.isFastScrollEnabled()) {
            if (mFlingStartPos < 0) {
                mFlingStartPos = firstVisibleItem
            } else if (Math.abs(firstVisibleItem - mFlingStartPos) >= MIN_COVERED_ENTRIES) {
                // must have moved at least xx entries up or down in fling mode, before fastscroll gets enabled
                listView.setFastScrollEnabled(true)
                listView.setFastScrollAlwaysVisible(true)
                listView.postDelayed(this::checkScrollState, AUTO_DISABLE_ON_IDLE)
            }
        }
    }

    private Unit checkScrollState() {
        if (listView.getFirstVisiblePosition() != mLastFirstVisibleItem || (Math.abs(System.currentTimeMillis() - mLastScroll) < AUTO_DISABLE_ON_IDLE)) {
            listView.postDelayed(this::checkScrollState, AUTO_DISABLE_ON_IDLE)
        } else {
            listView.setFastScrollEnabled(false)
            listView.setFastScrollAlwaysVisible(false)
            mFlingStartPos = -1
        }
    }
}
