package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Log;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import org.apache.commons.lang3.tuple.Pair;

import android.app.Activity;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

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
public abstract class AbstractViewPagerActivity<Page extends Enum<Page>> extends AbstractActivity {

    protected AbstractViewPagerActivity(String helpTopic) {
        super(helpTopic);
    }

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
        public View getDispatchedView();

        /**
         * Returns a (maybe cached) view.
         *
         * @return
         */
        public View getView();

        /**
         * Handles changed data-sets.
         */
        public void notifyDataSetChanged();
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
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        @Override
        public void finishUpdate(View container) {
        }

        @Override
        public int getCount() {
            return pageOrder.size();
        }

        @Override
        public Object instantiateItem(View container, int position) {
            final Page page = pageOrder.get(position);

            PageViewCreator creator = viewCreators.get(page);

            if (null == creator && null != page) {
                creator = AbstractViewPagerActivity.this.createViewCreator(page);
                viewCreators.put(page, creator);
            }

            View view = null;

            try {
                if (null != creator) {
                    // Result from getView() is maybe cached, but it should be valid because the
                    // creator should be informed about data-changes with notifyDataSetChanged()
                    view = creator.getView();
                    ((ViewPager) container).addView(view, 0);
                }
            } catch (Exception e) {
                Log.e("ViewPagerAdapter.instantiateItem ", e);
            }

            return view;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View arg0) {
        }

        @Override
        public int getItemPosition(Object object) {
            // We are doing the caching. So pretend that the view is gone.
            // The ViewPager will get it back in instantiateItem()
            return POSITION_NONE;
        }

        @Override
        public String getTitle(int position) {
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
    protected final void createViewPager(int startPageIndex, final OnPageSelectedListener pageSelectedListener) {
        // initialize ViewPager
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);

        titleIndicator = (TitlePageIndicator) findViewById(R.id.pager_indicator);
        titleIndicator.setViewPager(viewPager);
        if (pageSelectedListener != null) {
            titleIndicator.setOnPageChangeListener(new OnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    pageSelectedListener.onPageSelected(position);
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageScrollStateChanged(int state) {
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
        for (PageViewCreator creator : viewCreators.values()) {
            creator.notifyDataSetChanged();
        }

        pageOrder.clear();
        final Pair<List<? extends Page>, Integer> pagesAndIndex = getOrderedPages();
        pageOrder.addAll(pagesAndIndex.getLeft());

        // switch to details page, if we're out of bounds
        final int defaultPage = pagesAndIndex.getRight().intValue();
        if (viewPager.getCurrentItem() < 0 || viewPager.getCurrentItem() >= viewPagerAdapter.getCount()) {
            viewPager.setCurrentItem(defaultPage, false);
        }

        // notify the adapter that the data has changed
        viewPagerAdapter.notifyDataSetChanged();

        // notify the indicator that the data has changed
        titleIndicator.notifyDataSetChanged();
    }

    /**
     * @return the currently available list of ordered pages, together with the index of the default page
     */
    protected abstract Pair<List<? extends Page>, Integer> getOrderedPages();

    public final Page getPage(int position) {
        return pageOrder.get(position);
    }

    protected final int getPageIndex(Page page) {
        return pageOrder.indexOf(page);
    }

    protected final PageViewCreator getViewCreator(Page page) {
        return viewCreators.get(page);
    }

    protected final boolean isCurrentPage(Page page) {
        return viewPager.getCurrentItem() == getPageIndex(page);
    }
}
