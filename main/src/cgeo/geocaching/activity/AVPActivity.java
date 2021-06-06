package cgeo.geocaching.activity;

import android.view.View;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.functions.Action1;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.android.material.tabs.TabLayoutMediator;

public abstract class AVPActivity extends AbstractActionBarActivity {
    protected Map<Integer, AVPFragment> cache = new LinkedHashMap<>();

    private int currentPageId;
    private int[] orderedPages;
    private ViewPager2 viewPager = null;
    private Action1<Integer> onPageChangeListener = null;

    protected void createViewPager(final int initialPageId, final int[] orderedPages, final Action1<Integer> onPageChangeListener) {
        this.currentPageId = initialPageId;
        setOrderedPages(orderedPages);
        this.onPageChangeListener = onPageChangeListener;

        setContentView(R.layout.avpactivity);

        viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
        viewPager.setCurrentItem(pageIdToPosition(currentPageId));

        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager, (tab, position) -> tab.setText(getTitle(positionToPageId(position)))).attach();
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        /*
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
        */

        @Override
        public void onPageSelected(final int position) {
            super.onPageSelected(position);
            currentPageId = positionToPageId(position);
            if (onPageChangeListener != null) {
                onPageChangeListener.call(currentPageId);
            }
        }

        /*
        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
        }
        */
    };

    @SuppressWarnings("rawtypes")
    protected void setOrderedPages(final int[] orderedPages) {
        this.orderedPages = orderedPages;
        for (AVPFragment fragment : cache.values()) {
            fragment.notifyDataSetChanged();
        }
        this.cache.clear();
        if (viewPager != null) {
            viewPager.setCurrentItem(pageIdToPosition(currentPageId));
        }
    }

    public int positionToPageId(final int position) {
        return orderedPages[Math.max(0, Math.min(position, orderedPages.length - 1))];
    }

    private int pageIdToPosition(final int page) {
        if (orderedPages == null) {
            return 0;
        }
        for (int i = 0; i < orderedPages.length; i++) {
            if (orderedPages[i] == page) {
                return i;
            }
        }
        return 0;
    }

    protected abstract String getTitle(int pageId);
    @SuppressWarnings("rawtypes")
    protected abstract AVPFragment getFragment(int pageId);

    private class ViewPagerAdapter extends FragmentStateAdapter {
        private final WeakReference<FragmentActivity> fragmentActivityWeakReference;

        ViewPagerAdapter(final FragmentActivity fa) {
            super(fa);
            fragmentActivityWeakReference = new WeakReference<>(fa);
        }

        @Override
        @NonNull
        @SuppressWarnings("rawtypes")
        public Fragment createFragment(final int position) {
            final int pageId = positionToPageId(position);
            AVPFragment fragment = cache.get(pageId);
            if (fragment != null) {
                return fragment;
            }
            fragment = getFragment(pageId);
            fragment.setActivity(fragmentActivityWeakReference.get());
            cache.put(pageId, fragment);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return orderedPages == null ? 0 : orderedPages.length;
        }
    }

    @SuppressWarnings("rawtypes")
    private void notifyAdapterDataSetChanged() {
        final RecyclerView.Adapter adapter = viewPager.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    protected int getCurrentPageId() {
        return currentPageId;
    }

    protected boolean isCurrentPage(final int pageId) {
        return currentPageId == pageId;
    }

    @SuppressWarnings("rawtypes")
    protected void reinitializePage(final int pageId) {
        final AVPFragment fragment = cache.get(pageId);
        if (fragment != null) {
            fragment.notifyDataSetChanged();
        }
        notifyAdapterDataSetChanged();
        cache.remove(pageId);
    }

    @SuppressWarnings("rawtypes")
    protected void reinitializeViewPager() {
        for (AVPFragment fragment : cache.values()) {
            fragment.notifyDataSetChanged();
        }
        notifyAdapterDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        cache.clear();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        super.onDestroy();
    }


    // =======================================================================
    // transition methods - probably to be replaced later
    // =======================================================================

    protected void setIsContentRefreshable(final boolean hey) {
        // @todo mb: What to do with this?
    }

    public void pullToRefreshActionTrigger() {
        // @todo mb: how to trigger that method?
    }

}
