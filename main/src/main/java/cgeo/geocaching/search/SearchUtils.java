package cgeo.geocaching.search;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.utils.ClipboardUtils;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SearchUtils {

    private SearchUtils() {
        // utility class
    }

    public static void hideKeyboardOnSearchClick(final SearchView searchView, final MenuItem menuSearch) {
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

            @Override
            public boolean onSuggestionSelect(final int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(final int position) {
                // needs to run delayed, as it will otherwise change the SuggestionAdapter cursor which results in inconsistent datasets (see #11803)
                searchView.postDelayed(() -> {
                    menuSearch.collapseActionView();
                    searchView.setIconified(true);
                }, 1000);

                // return false to invoke standard behavior of launching the intent for the search result
                return false;
            }
        });

        // Used to collapse searchBar on submit from virtual keyboard
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String s) {
                menuSearch.collapseActionView();
                searchView.setIconified(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String s) {
                ((BaseSuggestionsAdapter) searchView.getSuggestionsAdapter()).changeQuery(s);
                return true;
            }
        });
    }

    public static void hideActionIconsWhenSearchIsActive(final Activity activity, final Menu menu, final MenuItem menuSearch) {
        menuSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(@NonNull final MenuItem item) {
                for (int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getItemId() == R.id.menu_paste_search && ConnectorFactory.containsGeocode(ClipboardUtils.getText())) {
                        menu.getItem(i).setVisible(true);
                    } else if (menu.getItem(i).getItemId() != R.id.menu_gosearch) {
                        menu.getItem(i).setVisible(false);
                    }
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull final MenuItem item) {
                activity.invalidateOptionsMenu();
                return true;
            }
        });
    }

    public static void handleDropDownVisibility(final Activity activity, final SearchView searchView, final MenuItem menuSearch) {
        final AutoCompleteTextView searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setOnDismissListener(() -> {
            if (!searchView.isIconified() && searchView.getSuggestionsAdapter().getCount() > 0) {
                searchAutoComplete.showDropDown();
            }
        });

        // only for bottom navigation activities
        final View.OnTouchListener outsideSearchAutoCompleteTouchListener = (v, event) -> {
            if (MotionEvent.ACTION_DOWN == event.getAction()) {
                return true;
            }
            if (!searchView.isIconified() && searchView.getSuggestionsAdapter().getCount() > 0) {
                menuSearch.collapseActionView();
                searchView.setIconified(true);
                return true; // intercept touch event to do not trigger an unwanted action
            }
            // In general, we don't want to intercept touch events...
            v.performClick();
            return false;
        };
        final View fl = activity.findViewById(R.id.activity_content);
        if (null != fl) {
            fl.setOnTouchListener(outsideSearchAutoCompleteTouchListener);
        }
        final BottomNavigationView bl = activity.findViewById(R.id.activity_navigationBar);
        final Menu blMenu = bl.getMenu();
        for (int i = 0; i < blMenu.size(); i++) {
            bl.setItemOnTouchListener(blMenu.getItem(i).getItemId(), outsideSearchAutoCompleteTouchListener);
        }
    }
    private static boolean isTouchInsideView(final MotionEvent ev, final View view) {
        final int[] location = new int[2];
        view.getLocationOnScreen(location);
        final float x = ev.getRawX();
        final float y = ev.getRawY();
        return x >= location[0] && x <= location[0] + view.getWidth()
                && y >= location[1] && y <= location[1] + view.getHeight();
    }

    public static void setSearchViewColor(final SearchView searchView) {
        if (searchView != null) {
            final AutoCompleteTextView searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            setSearchViewColor(searchAutoComplete);
        }
    }

    public static void setSearchViewColor(final AutoCompleteTextView searchAutoCompleteView) {
        if (searchAutoCompleteView != null) {
            searchAutoCompleteView.setTextColor(searchAutoCompleteView.getContext().getResources().getColor(R.color.colorTextActionBar));
            searchAutoCompleteView.setHintTextColor(searchAutoCompleteView.getContext().getResources().getColor(R.color.colorTextActionBar));
        }
    }
}
