package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract activity with the ability to manage pages in a view pager.
 *
 * @param <Page>
 *            Enum listing all available pages of this activity. The pages available at a certain point of time are
 *            defined by overriding {@link #getOrderedPages()}.
 */
public abstract class AbstractViewPagerActivity<Page extends Enum<Page>> extends AbstractActionBarActivity {

    /**
     * A {@link List} of all available pages.
     *
     * TODO Move to adapter
     */
    private final List<Page> pageOrder = new ArrayList<Page>();

    /**
     * Instances of all {@link PageViewCreator}.
     */
    private final Map<Page, PageViewCreator> viewCreators = new HashMap<Page, PageViewCreator>();

    /**
     * Store the states of the page views to be able to persist them when destroyed and reinstantiated again
     */
    private final Map<Page, Bundle> viewStates = new HashMap<Page, Bundle>();
    /**
     * The {@link ViewPager} for this activity.
     */
    private ViewPager viewPager;

    /**
     * The {@link ViewPagerAdapter} for this activity.
     */
    private ViewPagerAdapter viewPagerAdapter;

    /**
     * The {@link TitlePageIndicator} for this activity.
     */
    private TitlePageIndicator titleIndicator;

    public interface PageViewCreator {
        /**
         * Returns a validated view.
         *
         * @return
         */
        public View getDispatchedView(final ViewGroup parentView);

        /**
         * Returns a (maybe cached) view.
         *
         * @return
         */
        public View getView(final ViewGroup parentView);

        /**
         * Handles changed data-sets.
         */
        public void notifyDataSetChanged();

        /**
         * Gets state of the view
         */
        public @Nullable
        Bundle getViewState();

        /**
         * Set the state of the view
         */
        public void setViewState(@NonNull Bundle state);
    }

    /**
     * Page selection interface for the view pager.
     *
     */
    protected interface OnPageSelectedListener {
        public void onPageSelected(int position);
    }

    /**
     * The ViewPagerAdapter for scrolling through pages of the CacheDetailActivity.
     */
    private class ViewPagerAdapter extends PagerAdapter implements TitleProvider {

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            if (position >= pageOrder.size()) {
                return;
            }
            final Page page = pageOrder.get(position);

            // Store the state of the view if the page supports it
            final PageViewCreator creator = viewCreators.get(page);
            if (creator != null) {
                @Nullable
                final
                Bundle state = creator.getViewState();
                if (state != null) {
                    viewStates.put(page, state);
                }
            }

            container.removeView((View) object);
        }

        @Override
        public void finishUpdate(final ViewGroup container) {
        }

        @Override
        public int getCount() {
            return pageOrder.size();
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {

            final Page page = pageOrder.get(position);

            PageViewCreator creator = viewCreators.get(page);

            if (null == creator && null != page) {
                creator = AbstractViewPagerActivity.this.createViewCreator(page);
                viewCreators.put(page, creator);
                viewStates.put(page, new Bundle());
            }

            View view = null;

            try {
                if (null != creator) {
                    // Result from getView() is maybe cached, but it should be valid because the
                    // creator should be informed about data-changes with notifyDataSetChanged()
                    view = creator.getView(container);

                    // Restore the state of the view if the page supports it
                    final Bundle state = viewStates.get(page);
                    if (state != null) {
                        creator.setViewState(state);
                    }

                    container.addView(view, 0);
                }
            } catch (final Exception e) {
                Log.e("ViewPagerAdapter.instantiateItem ", e);
            }
            return view;
        }

        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view == object;
        }

        @Override
        public void restoreState(final Parcelable arg0, final ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(final ViewGroup arg0) {
        }

        @Override
        public int getItemPosition(final Object object) {
            // We are doing the caching. So pretend that the view is gone.
            // The ViewPager will get it back in instantiateItem()
            return POSITION_NONE;
        }

        @Override
        public String getTitle(final int position) {
            final Page page = pageOrder.get(position);
            if (null == page) {
                return "";
            }
            return AbstractViewPagerActivity.this.getTitle(page);
        }

    }

    /**
     * Create the view pager. Call this from the {@link Activity#onCreate} implementation.
     *
     * @param startPageIndex
     *            index of the page shown first
     * @param pageSelectedListener
     *            page selection listener or <code>null</code>
     */
    protected final void createViewPager(final int startPageIndex, final OnPageSelectedListener pageSelectedListener) {
        // initialize ViewPager
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);

        titleIndicator = (TitlePageIndicator) findViewById(R.id.pager_indicator);
        titleIndicator.setViewPager(viewPager);
        if (pageSelectedListener != null) {
            titleIndicator.setOnPageChangeListener(new OnPageChangeListener() {
                @Override
                public void onPageSelected(final int position) {
                    pageSelectedListener.onPageSelected(position);
                }

                @Override
                public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                }

                @Override
                public void onPageScrollStateChanged(final int state) {
                }
            });
        }

        // switch to entry page (last used or 2)
        if (viewPagerAdapter.getCount() < startPageIndex) {
            for (int i = 0; i <= startPageIndex; i++) {
                // we can't switch to a page that is out of bounds, so we add null-pages
                pageOrder.add(null);
            }
        }
        viewPagerAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(startPageIndex, false);
    }

    /**
     * create the view creator for the given page
     *
     * @return new view creator
     */
    protected abstract PageViewCreator createViewCreator(Page page);

    /**
     * get the title for the given page
     */
    protected abstract String getTitle(Page page);

    protected final void reinitializeViewPager() {

        // notify all creators that the data has changed
        for (final PageViewCreator creator : viewCreators.values()) {
            creator.notifyDataSetChanged();
        }
        // reset the stored view states of all pages
        for (final Bundle state : viewStates.values()) {
            state.clear();
        }

        pageOrder.clear();
        final Pair<List<? extends Page>, Integer> pagesAndIndex = getOrderedPages();
        pageOrder.addAll(pagesAndIndex.getLeft());

        // Since we just added pages notifyDataSetChanged needs to be called before we possibly setCurrentItem below.
        //  But, calling it will reset current item and we won't be able to tell if we would have been out of bounds
        final int currentItem = getCurrentItem();

        // notify the adapter that the data has changed
        viewPagerAdapter.notifyDataSetChanged();

        // switch to details page, if we're out of bounds
        final int defaultPage = pagesAndIndex.getRight();
        if (currentItem < 0 || currentItem >= viewPagerAdapter.getCount()) {
            viewPager.setCurrentItem(defaultPage, false);
        }

        // notify the indicator that the data has changed
        titleIndicator.notifyDataSetChanged();
    }

    /**
     * @return the currently available list of ordered pages, together with the index of the default page
     */
    protected abstract Pair<List<? extends Page>, Integer> getOrderedPages();

    public final Page getPage(final int position) {
        return pageOrder.get(position);
    }

    protected final int getPageIndex(final Page page) {
        return pageOrder.indexOf(page);
    }

    protected final PageViewCreator getViewCreator(final Page page) {
        return viewCreators.get(page);
    }

    protected final boolean isCurrentPage(final Page page) {
        return getCurrentItem() == getPageIndex(page);
    }

    protected int getCurrentItem() {
        return viewPager.getCurrentItem();
    }
}
