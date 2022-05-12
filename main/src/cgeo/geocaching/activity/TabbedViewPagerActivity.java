package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public abstract class TabbedViewPagerActivity extends AbstractActionBarActivity {
    @SuppressWarnings("rawtypes")
    private final Map<Long, TabbedViewPagerFragment> fragmentMap = new LinkedHashMap<>();
    /** Maps POSITION to Tab */
    private final Map<Integer, TabLayout.Tab> fragmentTabMap = new LinkedHashMap<>();


    private long initialPageId;
    private boolean initialPageShown;
    private long currentPageId;
    private long[] orderedPages;
    private ViewPager2 viewPager = null;
    private Action1<Long> onPageChangeListener = null;

    /**
     * The {@link SwipeRefreshLayout} for this activity. Might be null if page is not refreshable.
     */
    private SwipeRefreshLayout swipeRefreshLayout;

    /**
     * Set if the content is refreshable. Defaults to true if the Activity contains a {@link SwipeRefreshLayout}.
     */
    private boolean isRefreshable = true;

    protected void createViewPager(final long initialPageId, final long[] orderedPages, final Action1<Long> onPageChangeListener, final boolean isRefreshable) {
        this.isRefreshable = isRefreshable;
        this.initialPageId = initialPageId;
        this.initialPageShown = false;
        this.currentPageId = initialPageId;
        setOrderedPages(orderedPages);
        this.onPageChangeListener = onPageChangeListener;

        setContentView(isRefreshable ? R.layout.tabbed_viewpager_activity_refreshable : R.layout.tabbed_viewpager_activity);

        viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
        viewPager.setCurrentItem(pageIdToPosition(currentPageId), false);
        viewPager.setOffscreenPageLimit(10);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        if (swipeRefreshLayout != null) {
            final DisplayMetrics outMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(outMetrics);

            final float dpHeight = outMetrics.heightPixels / getResources().getDisplayMetrics().density;
            final int mDistanceToTriggerSync = (int) (dpHeight * 0.7);

            // initialize and register listener for pull-to-refresh gesture
            swipeRefreshLayout.setDistanceToTriggerSync(mDistanceToTriggerSync);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        pullToRefreshActionTrigger();
                    }
            );
        }

        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager, (tab, position) -> {
            this.fragmentTabMap.put(position, tab);
            tab.setText(getTitle(getItemId(position)));
        }).attach();
    }


    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        /*
        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
        */

        @Override
        public void onPageSelected(final int position) {
            super.onPageSelected(position);
            currentPageId = getItemId(position);
            if (onPageChangeListener != null) {
                onPageChangeListener.call(currentPageId);
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {
            super.onPageScrollStateChanged(state);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setEnabled(state == ViewPager2.SCROLL_STATE_IDLE && isRefreshable);
            }
        }
    };

    @SuppressWarnings("rawtypes")
    protected void setOrderedPages(final long[] orderedPages) {
        this.orderedPages = orderedPages;
        for (TabbedViewPagerFragment fragment : fragmentMap.values()) {
            fragment.notifyDataSetChanged();
        }
        notifyAdapterDataSetChanged();
        if (viewPager != null) {
            // if page requested originally has not been shown yet: try again
            final int i = pageIdToPositionWithErrorcode(initialPageShown ? currentPageId : initialPageId);
            if (i != -1) {
                initialPageShown = true;
            }
            viewPager.setCurrentItem(i != -1 ? i : 0);
        }
    }

    public long getItemId(final int position) {
        return orderedPages[Math.max(0, Math.min(position, orderedPages.length - 1))];
    }

    private int pageIdToPosition(final long page) {
        final int i = pageIdToPositionWithErrorcode(page);
        return i == -1 ? 0 : i;
    }

    // returns -1 if page requested not in orderedPages
    private int pageIdToPositionWithErrorcode(final long page) {
        if (orderedPages == null) {
            return -1;
        }
        for (int i = 0; i < orderedPages.length; i++) {
            if (orderedPages[i] == page) {
                return i;
            }
        }
        return -1;
    }

    protected abstract String getTitle(long pageId);

    @SuppressWarnings("rawtypes")
    protected abstract TabbedViewPagerFragment createNewFragment(long pageId);

    private class ViewPagerAdapter extends FragmentStateAdapter {

        ViewPagerAdapter(final FragmentActivity fa) {
            super(fa);
        }

        @Override
        @NonNull
        @SuppressWarnings("rawtypes")
        public Fragment createFragment(final int position) {
            final long pageId = getItemId(position);
            TabbedViewPagerFragment fragment = fragmentMap.get(pageId);
            if (fragment != null) {
                return fragment;
            }
            fragment = createNewFragment(pageId);
            fragmentMap.put(pageId, fragment);
            return fragment;
        }

        @Override
        public long getItemId(final int position) {
            return orderedPages[Math.max(0, Math.min(position, orderedPages.length - 1))];
        }

        @Override
        public boolean containsItem(final long pageId) {
            for (long orderedPage : orderedPages) {
                if (orderedPage == pageId) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getItemCount() {
            return orderedPages == null ? 0 : orderedPages.length;
        }
    }

    @SuppressWarnings("rawtypes")
    private void notifyAdapterDataSetChanged() {
        if (viewPager != null) {
            final RecyclerView.Adapter adapter = viewPager.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected long getCurrentPageId() {
        return currentPageId;
    }

    protected boolean isCurrentPage(final int pageId) {
        return currentPageId == pageId;
    }

    @SuppressWarnings("rawtypes")
    public void registerFragment(final long pageId, final TabbedViewPagerFragment fragment) {
        fragmentMap.put(pageId, fragment);
    }

    @SuppressWarnings("rawtypes")
    public void reinitializePage(final long pageId) {
        final TabbedViewPagerFragment fragment = fragmentMap.get(pageId);
        if (fragment != null) {
            fragment.notifyDataSetChanged();
        }
        notifyAdapterDataSetChanged();
    }

    @SuppressWarnings("rawtypes")
    protected void reinitializeViewPager() {
        for (TabbedViewPagerFragment fragment : fragmentMap.values()) {
            fragment.notifyDataSetChanged();
        }

        // force update current page, as this is not done automatically by the adapter
        final TabbedViewPagerFragment current = fragmentMap.get(currentPageId);
        if (current != null) {
            current.setContent();
        }

        notifyAdapterDataSetChanged();
    }

    public void reinitializeTitle(final long pageId) {
        final TabLayout.Tab tab = fragmentTabMap.get(pageIdToPosition(pageId));
        final String title = getTitle(pageId);
        if (tab != null && !Objects.equals(tab.getText(), title)) {
            tab.setText(title);
            //triggering adapter change will re-layout the view pager tabs
            viewPager.post(this::notifyAdapterDataSetChanged);
        }
    }

    /**
     * will be called, when refresh is triggered via the pull-to-refresh gesture
     */
    protected void pullToRefreshActionTrigger() {
        // do nothing by default. Should be overwritten by the activity if page is refreshable
    }

    /**
     * set if the pull-to-refresh gesture should be enabled for the displayed content
     */
    protected void setIsContentRefreshable(final boolean isRefreshable) {
        this.isRefreshable = isRefreshable;
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(isRefreshable);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TabbedViewPagerActivity.onCreate");
        if (savedInstanceState != null) {
            initialPageId = savedInstanceState.getLong("initialPageId", 0);
            initialPageShown = savedInstanceState.getBoolean("initialPageShown", false);
            currentPageId = savedInstanceState.getLong("currentPageId", 0);
            orderedPages = savedInstanceState.getLongArray("orderedPages");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("initialPageId", initialPageId);
        outState.putBoolean("initialPageShown", initialPageShown);
        outState.putLong("currentPageId", currentPageId);
        outState.putLongArray("orderedPages", orderedPages);
    }

    @Override
    protected void onDestroy() {
        Log.d("TabbedViewPagerActivity.onDestroy");
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------
    // ab hier lifecycle logging only
    // ---------------------------------------------------------------------------------

    /*
    @Override
    public void onStart() {
        super.onStart();
        Log.e("TabbedViewPagerActivity.onStart");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.e("TabbedViewPagerActivity.onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("TabbedViewPagerActivity.onResume");
    }

    @Override
    public void onPause() {
        Log.e("TabbedViewPagerActivity.onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e("TabbedViewPagerActivity.onStop");
        super.onStop();
    }
    */

}
