package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FilterUtils {

    private FilterUtils() {
        // should not be instantiated
    }

    public static void openFilterActivity(final Activity activity, final GeocacheFilterContext filterContext, final Collection<Geocache> filteredList) {

        GeocacheFilterActivity.selectFilter(
                activity,
                filterContext,
                filteredList, true);
    }

    public static <T extends Activity & FilteredActivity> boolean openFilterList(final T filteredActivity, final GeocacheFilterContext filterContext) {
        final List<GeocacheFilter> filters = new ArrayList<>(GeocacheFilter.Storage.getStoredFilters());

        if (filters.isEmpty()) {
            return false;
        } else {
            final boolean isFilterActive = filterContext.get().isFiltering();
            if (isFilterActive) {
                SimpleDialog.of(filteredActivity).setTitle(R.string.cache_filter_storage_select_clear_title)
                        .setButtons(0, 0, R.string.cache_filter_storage_clear_button)
                        .setSelectionForNeutral(false)
                        .selectSingle(filters, (f, pos) -> TextParam.text(f.getName()), -1, SimpleDialog.SingleChoiceMode.NONE,
                                (f, pos) -> filteredActivity.refreshWithFilter(f),
                                (f, pos) -> {
                                },
                                (f, pos) -> filteredActivity.refreshWithFilter(GeocacheFilter.createEmpty(filterContext.get().isOpenInAdvancedMode()))
                        );
            } else {
                SimpleDialog.of(filteredActivity).setTitle(R.string.cache_filter_storage_select_title)
                        .selectSingle(filters, (f, pos) -> TextParam.text(f.getName()), -1, SimpleDialog.SingleChoiceMode.NONE,
                                (f, pos) -> filteredActivity.refreshWithFilter(f),
                                (f, pos) -> {
                                }
                        );
            }
        }
        return true;
    }

    public static void updateFilterBar(final Activity activity, @Nullable final String filterName) {
        final View filterView = activity.findViewById(R.id.filter_bar);
        if (filterName == null) {
            filterView.setVisibility(View.GONE);
        } else {
            final TextView filterTextView = activity.findViewById(R.id.filter_text);
            filterTextView.setText(filterName);
            filterView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * filterView must exist
     */
    public static void initializeFilterBar(@NonNull final Activity activity, @NonNull final FilteredActivity filteredActivity, final boolean showSpacer) {
        final View filterView = activity.findViewById(R.id.filter_bar);
        filterView.setOnClickListener(v -> filteredActivity.showFilterMenu());
        filterView.setOnLongClickListener(v -> filteredActivity.showSavedFilterList());
        activity.findViewById(R.id.actionBarSpacer).setVisibility(showSpacer ? View.VISIBLE : View.GONE);
    }

    public static void toggleActionBar(@NonNull final AbstractBottomNavigationActivity activity) {
        final boolean actionBarShown = activity.toggleActionBar();
        activity.findViewById(R.id.actionBarSpacer).setVisibility(actionBarShown ? View.VISIBLE : View.GONE);
    }

    public static void initializeFilterMenu(@NonNull final Activity activity, @NonNull final FilteredActivity filteredActivity) {

        new Handler().post(() -> {
            final View filterView = activity.findViewById(R.id.menu_filter);
            if (filterView != null) {
                filterView.setOnLongClickListener(v -> filteredActivity.showSavedFilterList());
            }
        });
    }

}
