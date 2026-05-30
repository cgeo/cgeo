package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.models.NamedFilter;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class NamedFilterUtils {

    /** Separator used in filter names to define display groups, e.g. "Parent::Child". */
    private static final String GROUP_SEPARATOR = "::";

    private NamedFilterUtils() {
        // utility class
    }

    public static void openSingleSelectDialog(final Activity activity, final String title, final Consumer<NamedFilter> onSelected) {
        final List<NamedFilter> filters = NamedFilter.getAll();

        final SimpleDialog.ItemSelectModel<NamedFilter> model = buildGroupedModel(filters);

        SimpleDialog.of(activity)
            .setTitle(TextParam.text(title))
            .setPositiveButton(null)
            .selectSingle(model, onSelected);
    }

    public static void openMultiSelectDialog(final Activity activity, final Consumer<List<NamedFilter>> onConfirm) {
        final List<NamedFilter> filters = NamedFilter.getAll();

        final SimpleDialog.ItemSelectModel<NamedFilter> model = buildGroupedModel(filters);
        model.setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX);

        SimpleDialog.of(activity)
                .setTitle(TextParam.id(R.string.named_filter_select_title))
                .selectMultiple(model, selected -> {
                    final List<NamedFilter> selectedList = new ArrayList<>(selected);
                    onConfirm.accept(selectedList);
                });
    }

    public static void openActivateDeactivateDialog(final Activity activity) {
        final List<NamedFilter> filters = NamedFilter.getAll();

        final SimpleDialog.ItemSelectModel<NamedFilter> model = buildGroupedModel(filters);
        model.setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX);

        final Set<NamedFilter> preSelected = new HashSet<>();
        for (final NamedFilter nf : filters) {
            if (nf.isConditionalMarkerActive()) {
                preSelected.add(nf);
            }
        }
        model.setSelectedItems(preSelected);

        SimpleDialog.of(activity)
            .setTitle(TextParam.id(R.string.named_filter_activate_deactivate_title))
            .selectMultiple(model, selected -> {
                final List<NamedFilter> updated = new ArrayList<>(filters);
                for (final NamedFilter nf : updated) {
                    nf.setConditionalMarkerActive(selected.contains(nf));
                }
                NamedFilter.storeAll(updated);
            });
    }

    /**
     * Opens a single-select dialog for named filters and, upon selection, replaces the given
     * filter context with a {@link NamedFilterGeocacheFilter} referencing the selected named filter.
     * After setting the filter the {@code onFilterApplied} runnable is invoked.
     */
    public static void applySelectedNamedFilter(final Activity activity, final String title,
                                                final GeocacheFilterContext filterContext,
                                                final Runnable onFilterApplied) {
        openSingleSelectDialog(activity, title, selectedFilter -> {
            final NamedFilterGeocacheFilter namedRef = GeocacheFilterType.NAMED_FILTER.create();
            namedRef.setNamedFilterId(selectedFilter.getId());
            final GeocacheFilter newFilter = GeocacheFilter.create(false, false, namedRef);
            filterContext.set(newFilter);
            onFilterApplied.run();
        });
    }

    /**
     * Creates a {@link GeocacheFilter} that wraps the given named filter by reference
     * (i.e. a single {@link NamedFilterGeocacheFilter} pointing to the named filter's ID).
     */
    public static GeocacheFilter createNamedFilterReference(final NamedFilter namedFilter) {
        final NamedFilterGeocacheFilter namedRef = GeocacheFilterType.NAMED_FILTER.create();
        namedRef.setNamedFilterId(namedFilter.getId());
        return GeocacheFilter.create(false, false, namedRef);
    }

    public static GeocacheFilter getAsFilter(final NamedFilter nf) {
        if (nf == null) {
            return GeocacheFilter.createEmpty();
        }
        final NamedFilterGeocacheFilter nff = GeocacheFilterType.NAMED_FILTER.create();
        nff.setNamedFilterId(nf.getId());
        return GeocacheFilter.createEmpty(true).and(nff);
    }

    private static SimpleDialog.ItemSelectModel<NamedFilter> buildGroupedModel(final List<NamedFilter> filters) {
        final SimpleDialog.ItemSelectModel<NamedFilter> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            .setItems(filters)
            .setDisplayMapper((f, gi) -> {
                String name = f.getName();
                final String parentGroup = gi == null || gi.getGroup() == null ? "" : gi.getGroup().toString();
                if (name.startsWith(parentGroup + GROUP_SEPARATOR)) {
                    name = name.substring(parentGroup.length() + GROUP_SEPARATOR.length());
                }
                return TextParam.text(name);
            }, (f, gi) -> f.getName(), null)
            .setDisplayIconMapper(f -> f.getMarkerId() > 0 ? ImageParam.emoji(f.getMarkerId(), 30) : ImageParam.id(R.drawable.ic_menu_filter))
            .activateGrouping(f -> getGroupFromFilterName(f.getName()))
            .setGroupPruner(gi -> gi.getSize() >= 2)
            .setGroupGroupMapper(NamedFilterUtils::getGroupFromFilterName)
            .setGroupDisplayMapper(gi -> {
                final String parentGroup = gi.getParent() == null || gi.getParent().getGroup() == null ? "" : gi.getParent().getGroup();
                String name = gi.getGroup();
                if (name.startsWith(parentGroup + GROUP_SEPARATOR)) {
                    name = name.substring(parentGroup.length() + GROUP_SEPARATOR.length());
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
        final int idx = name.lastIndexOf(GROUP_SEPARATOR);
        return idx <= 0 ? null : name.substring(0, idx);
    }
}

