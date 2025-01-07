package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.style.StyleSpan;
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

    public static <T extends AbstractActivity & FilteredActivity> boolean openFilterList(final T filteredActivity, final GeocacheFilterContext filterContext) {
        final List<GeocacheFilter> filters = new ArrayList<>(GeocacheFilter.Storage.getStoredFilters());
        final boolean isFilterActive = filterContext.get().isFiltering();

        if (filters.isEmpty() && !isFilterActive) {
            filteredActivity.showToast(R.string.cache_filter_storage_load_delete_nofilter_message);
            return false;
        }

        if (filters.isEmpty()) {
            final TextParam message = TextParam.concat(TextParam.id(R.string.cache_filter_storage_load_delete_nofilter_message),
                    TextParam.text(System.lineSeparator()),
                    TextParam.id(R.string.cache_filter_storage_clear_message));
                SimpleDialog.of(filteredActivity).setTitle(R.string.cache_filter_storage_clear_title)
                        .setPositiveButton(TextParam.id(R.string.cache_filter_storage_clear_button))
                        .setMessage(message)
                        .confirm(() ->
                                filteredActivity.refreshWithFilter(GeocacheFilter.createEmpty(filterContext.get().isOpenInAdvancedMode()))
                        );
                return true;
            }

        final SimpleDialog.ItemSelectModel<GeocacheFilter> model = new SimpleDialog.ItemSelectModel<>();
        model
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
                .setItems(filters)
                .setDisplayMapper((f) -> TextParam.text(f.getName()))
                .activateGrouping(f -> getGroupFromFilterName(f.getName()))
                .setGroupDisplayMapper(gi -> TextParam.text("**" + gi.getGroup() + "** *(" + gi.getContainedItemCount() + ")*").setMarkdown(true));

        if (isFilterActive) {
            SimpleDialog.of(filteredActivity).setTitle(R.string.cache_filter_storage_select_clear_title)
                    .setNeutralButton(TextParam.id(R.string.cache_filter_storage_clear_button))
                    .setNeutralAction(() ->
                            filteredActivity.refreshWithFilter(GeocacheFilter.createEmpty(filterContext.get().isOpenInAdvancedMode()))
                    ).selectSingle(model, filteredActivity::refreshWithFilter);
        } else {
            SimpleDialog.of(filteredActivity).setTitle(R.string.cache_filter_storage_select_title)
                    .selectSingle(model, filteredActivity::refreshWithFilter);
        }
        return true;
    }

    public static String getGroupFromFilterName(final String group) {
        if (group == null) {
            return null;
        }
        final int idx = group.lastIndexOf(":");
        return idx <= 0 ? null : group.substring(0, idx);
    }

    public static void setFilterText(@NonNull final TextView viewField, @Nullable final String filterName, @Nullable final Boolean filterChanged) {
        if (filterName != null) {
            if (filterChanged == Boolean.TRUE) {
                viewField.setText(TextUtils.setSpan(filterName, new StyleSpan(Typeface.ITALIC)));
            } else {
                viewField.setText(filterName);
            }
        } else {
            viewField.setText("");
        }
    }

    public static void updateFilterBar(final Activity activity, @Nullable final String filterName, @Nullable final Boolean filterChanged) {
        final View filterView = activity.findViewById(R.id.filter_bar);
        if (filterName == null) {
            filterView.setVisibility(View.GONE);
        } else {
            final TextView filterTextView = activity.findViewById(R.id.filter_text);
            setFilterText(filterTextView, filterName, filterChanged);
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

        new Handler().post(() -> {
            final View filterView = activity.findViewById(R.id.menu_filter);
            if (filterView != null) {
                filterView.setOnLongClickListener(v -> filteredActivity.showSavedFilterList());
            }
        });
    }

}
