package cgeo.geocaching.activity;

import cgeo.geocaching.R;

import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.android.material.tabs.TabLayoutMediator;
import org.apache.commons.lang3.StringUtils;

public abstract class AVPActivity extends AppCompatActivity {
    protected Map<Integer, AVPFragment> cache = new LinkedHashMap<>();

    private ActionBar actionBar;
    private String prefix;
    private int currentPageId;
    private int[] orderedPages;
    private ViewPager2 viewPager = null;

    protected void createViewPager(final int currentPageId, final int[] orderedPages, final String prefix) {
        this.currentPageId = currentPageId;
        setOrderedPages(orderedPages);
        this.prefix = prefix;

        setContentView(R.layout.avpactivity);
        actionBar = getSupportActionBar();

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
        setActionBarTitle();
        }

        /*
        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
        }
        */
    };

    protected void setOrderedPages(final int[] orderedPages) {
        this.orderedPages = orderedPages;
        this.cache.clear();
    }

    private int positionToPageId(final int position) {
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

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        setActionBarTitle(); // set title here to avoid a race condition in onCreate/configure
        return super.onCreateOptionsMenu(menu);
    }

    protected abstract String getTitle(int page);
    protected abstract AVPFragment getFragment(int page);

    private void setActionBarTitle() {
        if (actionBar != null) {
            actionBar.setTitle((StringUtils.isNotBlank(prefix) ? prefix + " - " : "") + getTitle(currentPageId));
        }
    }

    private class ViewPagerAdapter extends FragmentStateAdapter {
        private final WeakReference<FragmentActivity> fragmentActivityWeakReference;

        ViewPagerAdapter(final FragmentActivity fa) {
            super(fa);
            fragmentActivityWeakReference = new WeakReference<>(fa);
        }

        @Override
        @NonNull
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cache.clear();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

}
