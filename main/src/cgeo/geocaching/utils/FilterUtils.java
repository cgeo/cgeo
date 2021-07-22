package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import static android.view.View.GONE;

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

    public static boolean openFilterList(final Activity activity, final GeocacheFilterContext filterContext) {
        final List<GeocacheFilter> filters = new ArrayList<>(GeocacheFilter.Storage.getStoredFilters());

        if (filters.isEmpty()) {
            return false;
        } else {
            final boolean isFilterActive = filterContext.get().isFiltering();
            final FilteredActivity filteredActivity = (FilteredActivity) activity;
            if (null != filteredActivity) {
                if (isFilterActive) {
                    SimpleDialog.of(activity).setTitle(R.string.cache_filter_storage_select_clear_title)
                        .setButtons(0, 0, R.string.cache_filter_storage_clear_button)
                        .setSelectionForNeutral(false)
                        .selectSingle(filters, (f, pos) -> TextParam.text(f.getName()), -1, false,
                            (f, pos) -> filteredActivity.refreshWithFilter(f),
                            (f, pos) -> {
                            },
                            (f, pos) -> filteredActivity.refreshWithFilter(GeocacheFilter.createEmpty())
                        );
                } else {
                    SimpleDialog.of(activity).setTitle(R.string.cache_filter_storage_select_title)
                        .selectSingle(filters, (f, pos) -> TextParam.text(f.getName()), -1, false,
                            (f, pos) -> filteredActivity.refreshWithFilter(f),
                            (f, pos) -> {
                            }
                        );
                }
            }
        }
        return true;
    }

    public static void updateFilterBar(final Activity activity, final Collection<String> filterNames) {
        final View filterView = activity.findViewById(R.id.filter_bar);
        if (filterNames.isEmpty()) {
            filterView.setVisibility(GONE);
        } else {
            final TextView filterTextView = activity.findViewById(R.id.filter_text);
            filterTextView.setText(TextUtils.join(", ", filterNames));
            filterView.setVisibility(View.VISIBLE);
        }
    }

    public static void connectFilterBar(final Activity activity) {
        final FilteredActivity filteredActivity = (FilteredActivity) activity;
        if (null != filteredActivity) {
            final View filterView = activity.findViewById(R.id.filter_bar);
            if (null != filterView) {
                filterView.setOnLongClickListener(v -> filteredActivity.showFilterList(null));
            }
        }
    }

    public static void connectFilterMenu(final Activity activity) {

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final FilteredActivity filteredActivity = (FilteredActivity) activity;
                if (null != filteredActivity) {
                    final View view = activity.findViewById(R.id.menu_filter);
                    if (view != null) {
                        view.setOnClickListener(v -> filteredActivity.showFilterMenu(null));
                        view.setOnLongClickListener(v -> filteredActivity.showFilterList(null));
                    }
                }
            }
        });
    }

}
