package cgeo.geocaching.search;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.ui.RelativeLayoutWithInterceptTouchEventPossibility;
import cgeo.geocaching.utils.ClipboardUtils;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.widget.SearchView;

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
            public boolean onMenuItemActionExpand(final MenuItem item) {
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
            public boolean onMenuItemActionCollapse(final MenuItem item) {
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
        final RelativeLayoutWithInterceptTouchEventPossibility activityViewroot = activity.findViewById(R.id.bottomnavigation_activity_viewroot);
        if (activityViewroot != null) {
            activityViewroot.setOnInterceptTouchEventListener(ev -> {
                if (!searchView.isIconified() && searchView.getSuggestionsAdapter().getCount() > 0) {
                    menuSearch.collapseActionView();
                    searchView.setIconified(true);
                    return true; // intercept touch event to do not trigger an unwanted action
                }
                // In general, we don't want to intercept touch events...
                return false;
            });
        }
    }

}
