package cgeo.geocaching.activity;

import cgeo.geocaching.R;

import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.google.android.material.tabs.TabLayoutMediator;
import org.apache.commons.lang3.StringUtils;

public abstract class AVPActivity extends AppCompatActivity {
    protected static final ArrayList<AVPFragment> pages = new ArrayList<>();
    protected static final ArrayList<Page> pagesSource = new ArrayList<>();

    private ActionBar actionBar;
    private String prefix;
    private int currentPage = 0;

    protected void configure(final int currentPageNum, final String prefix) {
        this.currentPage = currentPageNum;
        this.prefix = prefix;

        setContentView(R.layout.avpactivity);
        actionBar = getSupportActionBar();

        final ViewPager2 viewPager = (ViewPager2) findViewById(R.id.viewpager);
        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            /*
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
            */

            @Override
            public void onPageSelected(final int position) {
                super.onPageSelected(position);
                currentPage = position;
                setActionBarTitle();
            }

            /*
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
            */
        });
        viewPager.setCurrentItem(currentPage);

        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager,
            (tab, position) -> tab.setText(getTitle(position))
        ).attach();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        setActionBarTitle(); // set title here to avoid a race condition in onCreate/configure
        return super.onCreateOptionsMenu(menu);
    }

    protected String getTitle(final int page) {
        return getResources().getString(pagesSource.get(page).resourceId);
    }

    private void setActionBarTitle() {
        if (actionBar != null) {
            actionBar.setTitle((StringUtils.isNotBlank(prefix) ? prefix + " - " : "") + getTitle(currentPage));
        }
    }

    protected static class Page {
        @StringRes
        private final int resourceId;
        private final Class clazz;

        public Page(@StringRes final int resourceId, final Class clazz) {
            this.resourceId = resourceId;
            this.clazz = clazz;
        }
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        private final WeakReference<FragmentActivity> fragmentActivityWeakReference;

        ViewPagerAdapter(final FragmentActivity fa) {
            super(fa);
            fragmentActivityWeakReference = new WeakReference<>(fa);
        }

        @Override
        @NonNull
        public Fragment createFragment(final int page) {
            if (pages.size() > page) {
                return pages.get(page);
            }
            if (pagesSource.size() > page) {
                try {
                    final AVPFragment fragment = (AVPFragment) pagesSource.get(page).clazz.newInstance();
                    fragment.setActivity(fragmentActivityWeakReference.get());
                    pages.add(page, fragment);
                    return fragment;
                } catch (IllegalAccessException | InstantiationException e) {
                    return null;
                }
            }
            throw new IllegalStateException(); // cannot happen, when switch case is enum complete
        }

        @Override
        public int getItemCount() {
            return pagesSource.size();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pages.clear();
    }

}
