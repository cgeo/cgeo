package cgeo.geocaching.filters;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.FilteredActivity;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public class FilterUtils {

    /** Separator used in filter names to define display groups, e.g. "Parent:Child". */
    private static final String NAMED_FILTER_GROUP_SEPARATOR = ":";

    private FilterUtils() {
        // should not be instantiated
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

    /** filterView must exist */
    public static void initializeFilterBar(@Nullable final View filterBarView, @NonNull final FilteredActivity filteredActivity) {
        ViewUtils.setForParentAndChildren(filterBarView,
            v -> filteredActivity.showFilterMenu(),
            v -> filteredActivity.showSavedFilterList());
    }

    public static void initializeFilterMenu(final Activity activity, final int filterMenuId, @NonNull final FilteredActivity filteredActivity) {
        ViewUtils.registerLongClickHandlerForMenuItem(activity, filterMenuId, v -> filteredActivity.showSavedFilterList());
    }

    public static void onClickFilterMenu(@NonNull final FilteredActivity filteredActivity) {
        filteredActivity.showFilterMenu();
    }

    public static void initializeNamedFilterMenu(final Activity activity, final int namedFilterMenuId, @NonNull final FilteredActivity filteredActivity) {
        ViewUtils.registerLongClickHandlerForMenuItem(activity, namedFilterMenuId, v -> filteredActivity.showNamedFilterActivateDeactivate());
    }

    public static void onClickNamedFilterMenu(@NonNull final Activity activity) {
        NamedFilterActivity.startActivity(activity);
    }

    /** opens a dialog to activate/deactivate named filter markers */
    public static void openDialogActivateDeactivateNamedFilters(final Context context) {
        final List<NamedFilter> filters = NamedFilter.getAll();
        final Set<NamedFilter> preSelected = new HashSet<>();
        for (final NamedFilter nf : filters) {
            if (nf.isConditionalMarkerActive()) {
                preSelected.add(nf);
            }
        }

        openDialogMultiselectNamedFilters(context, TextParam.id(R.string.named_filter_activate_deactivate_title), preSelected,
                NamedFilter::activateMarker);
    }

    public static void openDialogMultiselectNamedFilters(final Context context, final TextParam title, final Set<NamedFilter> preselected, final Consumer<Set<NamedFilter>> selectionListener) {
        final List<NamedFilter> filters = NamedFilter.getAll();

        final SimpleDialog.ItemSelectModel<NamedFilter> model = buildGroupedModel(filters);
        model.setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX);

        model.setSelectedItems(preselected);

        SimpleDialog.ofContext(context)
            .setTitle(title)
            .selectMultiple(model, selectionListener);
    }

    /** opens dialog to select a new filter among named filters. Includes options to clear and select previous (if GeocacheFilterContext is provided) */
    public static void openDialogSelectNamedFilter(@NonNull final Context context, @Nullable final TextParam title, @Nullable final GeocacheFilterContext filterContext, @Nullable final Consumer<GeocacheFilter> onFilterSelected) {
        final GeocacheFilter currentFilter = filterContext == null ? null : filterContext.get();
        final boolean isFilterActive = currentFilter != null && currentFilter.isFiltering();
        final GeocacheFilter previousFilter = filterContext == null ? null : filterContext.getPreviousFilter();
        final boolean hasPreviousFilter = previousFilter != null && previousFilter.isFiltering();

        final SimpleDialog.ItemSelectModel<NamedFilter> model = buildGroupedModel(NamedFilter.getAll());

        SimpleDialog.ofContext(context)
            .setTitle(title != null ? title : TextParam.id(R.string.named_filter_select_title))
            .setPositiveButton(null)
            .setNeutralButton(isFilterActive ? TextParam.id(R.string.cache_filter_storage_clear_button) : null)
            .setNegativeButton(hasPreviousFilter ? TextParam.id(R.string.caches_filter_select_last) : null)
            .setButtonClickAction(which -> {
                if (which == DialogInterface.BUTTON_NEUTRAL && isFilterActive) {
                    final GeocacheFilter empty = GeocacheFilter.createEmpty();
                    filterContext.set(empty);
                    if (onFilterSelected != null) {
                        onFilterSelected.accept(empty);
                    }
                } else if (which == DialogInterface.BUTTON_NEGATIVE && hasPreviousFilter) {
                    filterContext.set(previousFilter);
                    if (onFilterSelected != null) {
                        onFilterSelected.accept(previousFilter);
                    }
                }
                return false;
            })
            .selectSingle(model, selectedNamedFilter -> {
                final GeocacheFilter sourceFilter = selectedNamedFilter.getFilter();
                final GeocacheFilter newFilter = sourceFilter == null ? GeocacheFilter.createEmpty() :
                    GeocacheFilter.create(sourceFilter.isOpenInAdvancedMode(), sourceFilter.isIncludeInconclusive(), selectedNamedFilter, sourceFilter.getTree());
                if (filterContext != null) {
                    filterContext.set(newFilter);
                }
                if (onFilterSelected != null) {
                    onFilterSelected.accept(newFilter);
                }
            });
    }

    /** builds basic display model for named filters, initialized to "single-plain". Handles grouping */
    private static SimpleDialog.ItemSelectModel<NamedFilter> buildGroupedModel(final List<NamedFilter> filters) {
        final SimpleDialog.ItemSelectModel<NamedFilter> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItems(filters)
            .setDisplayMapper((f, gi) -> {
                String name = f.getName();
                final String parentGroup = gi == null || gi.getGroup() == null ? "" : gi.getGroup().toString();
                if (name.startsWith(parentGroup + NAMED_FILTER_GROUP_SEPARATOR)) {
                    name = name.substring(parentGroup.length() + NAMED_FILTER_GROUP_SEPARATOR.length());
                }
                return TextParam.text(name);
            }, (f, gi) -> f.getName(), null)
            .setDisplayIconMapper(f -> StringUtils.isNotBlank(f.getMarkerId()) ? ImageParam.emoji(f.getMarkerId(), 30) : ImageParam.id(R.drawable.ic_menu_marker))
            .activateGrouping(f -> getGroupFromFilterName(f.getName()))
            .setGroupPruner(gi -> gi.getSize() >= 2)
            .setGroupGroupMapper(FilterUtils::getGroupFromFilterName)
            .setGroupDisplayMapper(gi -> {
                final String parentGroup = gi.getParent() == null || gi.getParent().getGroup() == null ? "" : gi.getParent().getGroup();
                String name = gi.getGroup();
                if (name.startsWith(parentGroup + NAMED_FILTER_GROUP_SEPARATOR)) {
                    name = name.substring(parentGroup.length() + NAMED_FILTER_GROUP_SEPARATOR.length());
                }
                return TextParam.text("**" + name + "**").setMarkdown(true);
            })
            .setReducedGroupSaver("named_filters", g -> g, g -> g);
        return model;
    }

    private static String getGroupFromFilterName(final String name) {
        if (name == null) {
            return null;
        }
        final int idx = name.lastIndexOf(NAMED_FILTER_GROUP_SEPARATOR);
        return idx <= 0 ? null : name.substring(0, idx);
    }
}

