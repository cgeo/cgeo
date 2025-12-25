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

package cgeo.geocaching.activity

import cgeo.geocaching.R
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.OfflineTranslateUtils
import cgeo.geocaching.utils.functions.Action1

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View

import androidx.annotation.NonNull
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

import java.util.LinkedHashMap
import java.util.Map
import java.util.Objects

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

abstract class TabbedViewPagerActivity : AbstractActionBarActivity() {
    @SuppressWarnings("rawtypes")
    private val fragmentMap: Map<Long, TabbedViewPagerFragment> = LinkedHashMap<>()
    /** Maps POSITION to Tab */
    private val fragmentTabMap: Map<Integer, TabLayout.Tab> = LinkedHashMap<>()


    private Long initialPageId
    private Boolean initialPageShown
    private Long currentPageId
    private Long[] orderedPages
    private var viewPager: ViewPager2 = null
    private var onPageChangeListener: Action1<Long> = null
    public final OfflineTranslateUtils.Status translationStatus = OfflineTranslateUtils.Status()

    /**
     * The {@link SwipeRefreshLayout} for this activity. Might be null if page is not refreshable.
     */
    private SwipeRefreshLayout swipeRefreshLayout

    /**
     * Set if the content is refreshable. Defaults to true if the Activity contains a {@link SwipeRefreshLayout}.
     */
    private var isRefreshable: Boolean = true

    protected Unit createViewPager(final Long initialPageId, final Long[] orderedPages, final Action1<Long> onPageChangeListener, final Boolean isRefreshable) {
        this.isRefreshable = isRefreshable
        this.initialPageId = initialPageId
        this.initialPageShown = false
        this.currentPageId = initialPageId
        setOrderedPages(orderedPages)
        this.onPageChangeListener = onPageChangeListener

        setContentView(isRefreshable ? R.layout.tabbed_viewpager_activity_refreshable : R.layout.tabbed_viewpager_activity)

        viewPager = findViewById(R.id.viewpager)
        viewPager.setAdapter(ViewPagerAdapter(this))
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        viewPager.setCurrentItem(pageIdToPosition(currentPageId), false)
        viewPager.setOffscreenPageLimit(10)

        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        if (swipeRefreshLayout != null) {
            val outMetrics: DisplayMetrics = DisplayMetrics()
            getWindowManager().getDefaultDisplay().getMetrics(outMetrics)

            val dpHeight: Float = outMetrics.heightPixels / getResources().getDisplayMetrics().density
            val mDistanceToTriggerSync: Int = (Int) (dpHeight * 0.7)

            // initialize and register listener for pull-to-refresh gesture
            swipeRefreshLayout.setDistanceToTriggerSync(mDistanceToTriggerSync)
            swipeRefreshLayout.setOnRefreshListener(() -> {
                        swipeRefreshLayout.setRefreshing(false)
                        pullToRefreshActionTrigger()
                    }
            )
        }

        TabLayoutMediator(findViewById(R.id.tab_layout), viewPager, (tab, position) -> {
            this.fragmentTabMap.put(position, tab)
            tab.setText(getTitle(getItemId(position)))
        }).attach()
    }


    private final ViewPager2.OnPageChangeCallback pageChangeCallback = ViewPager2.OnPageChangeCallback() {

        override         public Unit onPageSelected(final Int position) {
            super.onPageSelected(position)
            currentPageId = getItemId(position)
            if (onPageChangeListener != null) {
                onPageChangeListener.call(currentPageId)
            }
        }

        override         public Unit onPageScrollStateChanged(final Int state) {
            super.onPageScrollStateChanged(state)
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setEnabled(state == ViewPager2.SCROLL_STATE_IDLE && isRefreshable)
            }
        }
    }

    @SuppressWarnings("rawtypes")
    protected Unit setOrderedPages(final Long[] orderedPages) {
        this.orderedPages = orderedPages
        for (TabbedViewPagerFragment fragment : fragmentMap.values()) {
            fragment.notifyDataSetChanged()
        }
        notifyAdapterDataSetChanged()
        if (viewPager != null) {
            // if page requested originally has not been shown yet: try again
            val i: Int = pageIdToPositionWithErrorcode(initialPageShown ? currentPageId : initialPageId)
            if (i != -1) {
                initialPageShown = true
            }
            viewPager.setCurrentItem(i != -1 ? i : 0)
        }
    }

    public Long getItemId(final Int position) {
        return orderedPages[Math.max(0, Math.min(position, orderedPages.length - 1))]
    }

    private Int pageIdToPosition(final Long page) {
        val i: Int = pageIdToPositionWithErrorcode(page)
        return i == -1 ? 0 : i
    }

    // returns -1 if page requested not in orderedPages
    private Int pageIdToPositionWithErrorcode(final Long page) {
        if (orderedPages == null) {
            return -1
        }
        for (Int i = 0; i < orderedPages.length; i++) {
            if (orderedPages[i] == page) {
                return i
            }
        }
        return -1
    }

    protected abstract String getTitle(Long pageId)

    @SuppressWarnings("rawtypes")
    protected abstract TabbedViewPagerFragment createNewFragment(Long pageId)

    private class ViewPagerAdapter : FragmentStateAdapter() {

        ViewPagerAdapter(final FragmentActivity fa) {
            super(fa)
        }

        override         @SuppressWarnings("rawtypes")
        public Fragment createFragment(final Int position) {
            val pageId: Long = getItemId(position)
            TabbedViewPagerFragment fragment = fragmentMap.get(pageId)
            if (fragment != null) {
                return fragment
            }
            fragment = createNewFragment(pageId)
            fragmentMap.put(pageId, fragment)
            return fragment
        }

        override         public Long getItemId(final Int position) {
            return orderedPages[Math.max(0, Math.min(position, orderedPages.length - 1))]
        }

        override         public Boolean containsItem(final Long pageId) {
            for (Long orderedPage : orderedPages) {
                if (orderedPage == pageId) {
                    return true
                }
            }
            return false
        }

        override         public Int getItemCount() {
            return orderedPages == null ? 0 : orderedPages.length
        }
    }

    @SuppressWarnings("rawtypes")
    private Unit notifyAdapterDataSetChanged() {
        if (viewPager != null) {
            final RecyclerView.Adapter adapter = viewPager.getAdapter()
            if (adapter != null) {
                adapter.notifyDataSetChanged()
            }
        }
    }

    protected Long getCurrentPageId() {
        return currentPageId
    }

    @SuppressWarnings("rawtypes")
    public Unit registerFragment(final Long pageId, final TabbedViewPagerFragment fragment) {
        fragmentMap.put(pageId, fragment)
    }

    @SuppressWarnings("rawtypes")
    public Unit reinitializePage(final Long pageId) {
        val fragment: TabbedViewPagerFragment = fragmentMap.get(pageId)
        if (fragment != null) {
            fragment.notifyDataSetChanged()
        }
        notifyAdapterDataSetChanged()
    }

    @SuppressWarnings("rawtypes")
    protected Unit reinitializeViewPager() {
        for (TabbedViewPagerFragment fragment : fragmentMap.values()) {
            fragment.notifyDataSetChanged()
        }

        // force update current page, as this is not done automatically by the adapter
        val current: TabbedViewPagerFragment = fragmentMap.get(currentPageId)
        if (current != null) {
            current.setContent()
        }

        notifyAdapterDataSetChanged()
    }

    public Unit reinitializeTitle(final Long pageId) {
        final TabLayout.Tab tab = fragmentTabMap.get(pageIdToPosition(pageId))
        val title: String = getTitle(pageId)
        if (tab != null && !Objects == (tab.getText(), title)) {
            tab.setText(title)
            //triggering adapter change will re-layout the view pager tabs
            viewPager.post(this::notifyAdapterDataSetChanged)
        }
    }

    protected Unit scrollToBottom() {
        findViewById(R.id.detailScroll).post(() -> ((NestedScrollView) findViewById(R.id.detailScroll)).fullScroll(View.FOCUS_DOWN))
    }

    /**
     * will be called, when refresh is triggered via the pull-to-refresh gesture
     */
    protected Unit pullToRefreshActionTrigger() {
        // do nothing by default. Should be overwritten by the activity if page is refreshable
    }

    /**
     * set if the pull-to-refresh gesture should be enabled for the displayed content
     */
    protected Unit setIsContentRefreshable(final Boolean isRefreshable) {
        this.isRefreshable = isRefreshable
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(isRefreshable)
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        Log.d("TabbedViewPagerActivity.onCreate")
        if (savedInstanceState != null) {
            initialPageId = savedInstanceState.getLong("initialPageId", 0)
            initialPageShown = savedInstanceState.getBoolean("initialPageShown", false)
            currentPageId = savedInstanceState.getLong("currentPageId", 0)
            orderedPages = savedInstanceState.getLongArray("orderedPages")
        }
    }

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putLong("initialPageId", initialPageId)
        outState.putBoolean("initialPageShown", initialPageShown)
        outState.putLong("currentPageId", currentPageId)
        outState.putLongArray("orderedPages", orderedPages)
    }

    override     protected Unit onDestroy() {
        Log.d("TabbedViewPagerActivity.onDestroy")
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        }
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------
    // lifecycle logging only - for testing purposes
    // ---------------------------------------------------------------------------------

    /*
    override     public Unit onStart() {
        super.onStart()
        Log.e("TabbedViewPagerActivity.onStart")
    }

    override     public Unit onRestart() {
        super.onRestart()
        Log.e("TabbedViewPagerActivity.onRestart")
    }

    override     public Unit onResume() {
        super.onResume()
        Log.e("TabbedViewPagerActivity.onResume")
    }

    override     public Unit onPause() {
        Log.e("TabbedViewPagerActivity.onPause")
        super.onPause()
    }

    override     public Unit onStop() {
        Log.e("TabbedViewPagerActivity.onStop")
        super.onStop()
    }
    */

}
