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

package cgeo.geocaching.search

import cgeo.geocaching.R
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.ui.RelativeLayoutWithInterceptTouchEventPossibility
import cgeo.geocaching.utils.ClipboardUtils

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import android.widget.AutoCompleteTextView

import androidx.annotation.NonNull
import androidx.appcompat.widget.SearchView

class SearchUtils {

    private SearchUtils() {
        // utility class
    }

    public static Unit hideKeyboardOnSearchClick(final SearchView searchView, final MenuItem menuSearch) {
        searchView.setOnSuggestionListener(SearchView.OnSuggestionListener() {

            override             public Boolean onSuggestionSelect(final Int position) {
                return false
            }

            override             public Boolean onSuggestionClick(final Int position) {
                // needs to run delayed, as it will otherwise change the SuggestionAdapter cursor which results in inconsistent datasets (see #11803)
                searchView.postDelayed(() -> {
                    menuSearch.collapseActionView()
                    searchView.setIconified(true)
                }, 1000)

                // return false to invoke standard behavior of launching the intent for the search result
                return false
            }
        })

        // Used to collapse searchBar on submit from virtual keyboard
        searchView.setOnQueryTextListener(SearchView.OnQueryTextListener() {
            override             public Boolean onQueryTextSubmit(final String s) {
                menuSearch.collapseActionView()
                searchView.setIconified(true)
                return false
            }

            override             public Boolean onQueryTextChange(final String s) {
                ((BaseSuggestionsAdapter) searchView.getSuggestionsAdapter()).changeQuery(s)
                return true
            }
        })
    }

    public static Unit hideActionIconsWhenSearchIsActive(final Activity activity, final Menu menu, final MenuItem menuSearch) {
        menuSearch.setOnActionExpandListener(MenuItem.OnActionExpandListener() {

            override             public Boolean onMenuItemActionExpand(final MenuItem item) {
                for (Int i = 0; i < menu.size(); i++) {
                    if (menu.getItem(i).getItemId() == R.id.menu_paste_search && ConnectorFactory.containsGeocode(ClipboardUtils.getText())) {
                        menu.getItem(i).setVisible(true)
                    } else if (menu.getItem(i).getItemId() != R.id.menu_gosearch) {
                        menu.getItem(i).setVisible(false)
                    }
                }
                return true
            }

            override             public Boolean onMenuItemActionCollapse(final MenuItem item) {
                activity.invalidateOptionsMenu()
                return true
            }
        })
    }

    public static Unit handleDropDownVisibility(final Activity activity, final SearchView searchView, final MenuItem menuSearch) {
        val searchAutoComplete: AutoCompleteTextView = searchView.findViewById(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete.setOnDismissListener(() -> {
            if (!searchView.isIconified() && searchView.getSuggestionsAdapter().getCount() > 0) {
                searchAutoComplete.showDropDown()
            }
        })
        // only for bottom navigation activities
        val activityViewroot: RelativeLayoutWithInterceptTouchEventPossibility = activity.findViewById(R.id.bottomnavigation_activity_viewroot)
        if (activityViewroot != null) {
            activityViewroot.setOnInterceptTouchEventListener(ev -> {
                if (!searchView.isIconified() && searchView.getSuggestionsAdapter().getCount() > 0) {
                    menuSearch.collapseActionView()
                    searchView.setIconified(true)
                    return true; // intercept touch event to do not trigger an unwanted action
                }
                // In general, we don't want to intercept touch events...
                return false
            })
        }
    }

    public static Unit setSearchViewColor(final SearchView searchView) {
        if (searchView != null) {
            val searchAutoComplete: AutoCompleteTextView = searchView.findViewById(androidx.appcompat.R.id.search_src_text)
            setSearchViewColor(searchAutoComplete)
        }
    }

    public static Unit setSearchViewColor(final AutoCompleteTextView searchAutoCompleteView) {
        if (searchAutoCompleteView != null) {
            searchAutoCompleteView.setTextColor(searchAutoCompleteView.getContext().getResources().getColor(R.color.colorTextActionBar))
            searchAutoCompleteView.setHintTextColor(searchAutoCompleteView.getContext().getResources().getColor(R.color.colorTextActionBar))
        }
    }
}
