package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

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

    public static void setFilterText(@NonNull final TextView viewField, @Nullable final String filterName) {
        if (filterName != null) {
            viewField.setText(filterName);
        } else {
            viewField.setText("");
        }
    }

    public static void updateFilterBar(final Activity activity, @Nullable final String filterName) {
        final View filterView = activity.findViewById(R.id.filter_bar);
        if (filterName == null) {
            filterView.setVisibility(View.GONE);
        } else {
            final TextView filterTextView = activity.findViewById(R.id.filter_text);
            setFilterText(filterTextView, filterName);
            filterView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * filterView must exist
     */
    public static void initializeFilterBar(@NonNull final Activity activity, @NonNull final FilteredActivity filteredActivity) {
        ViewUtils.setForParentAndChildren(activity.findViewById(R.id.filter_bar),
                v -> filteredActivity.showFilterMenu(),
                v -> filteredActivity.showSavedFilterList());
    }

    public static void initializeFilterMenu(@NonNull final Activity activity, @NonNull final FilteredActivity filteredActivity) {

        new android.os.Handler().post(() -> {
            final View filterView = activity.findViewById(R.id.menu_filter);
            if (filterView != null) {
                filterView.setOnLongClickListener(v -> filteredActivity.showSavedFilterList());
            }
        });
    }

}

