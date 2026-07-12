package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.HierarchylisthelperUpsertBinding;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class HierarchicalListHelper<T> {

    public static final String SEPARATOR = ":";

    public interface HierarchyAccessor<T> {

        @NonNull
        String getFullName(T item);

        @Nullable
        default TextParam getDisplayText(T item, String simpleName) {
            return null;
        }

        boolean supportsMarkers();

        @Nullable
        default String getMarker(T item) {
            return null;
        }

        default String getGroupSortString(String groupName) {
            return null;
        }
    }

    private final HierarchyAccessor<T> accessor;

    private HierarchicalListHelper(final HierarchyAccessor<T> accessor) {
        this.accessor = accessor;
    }

    public static <T> HierarchicalListHelper<T> of(final HierarchyAccessor<T> accessor) {
        return new HierarchicalListHelper<>(accessor);
    }

    public SimpleItemListModel<T>.GroupingOptions<String> configureDisplay(final SimpleDialog.ItemSelectModel<T> model) {

        // Display for normal items
        model.setDisplayMapper((item, itemGroup) -> {
            final String fullName = accessor.getFullName(item);
            final String simple = getSimpleName(fullName);
            final TextParam tp = accessor.getDisplayText(item, simple);
            return tp != null ? tp : TextParam.text(simple);
        }, (item, itemGroup) -> accessor.getFullName(item), null);
        if (accessor.supportsMarkers()) {
            model.setDisplayIconMapper(this::getMarkerImage);
        }

        // GROUPING
        final SimpleItemListModel<T>.GroupingOptions<String> groupingOptions =
        model.activateGrouping(this::getParent)
            .setGroupGroupMapper(this::getParent)
            .setItemGroupComparator(getGroupListSorter())
            .setGroupDisplayMapper(gi -> {
                final String parentGroup = gi.getParent() == null || gi.getParent().getGroup() == null ? "" : gi.getParent().getGroup();
                String title = gi.getGroup();
                if (title.startsWith(parentGroup + SEPARATOR)) {
                    title = title.substring(parentGroup.length() + 1);
                }
                return TextParam.text("**" + title + "** *(" + gi.getContainedItemCount() + ")*").setMarkdown(true);
            })
            .setGroupPruner(gi -> gi.getSize() >= 2);
        if (accessor.supportsMarkers()) {
            groupingOptions.setGroupDisplayIconMapper(gi -> gi.getItems().isEmpty() ? null : getMarkerImage(gi.getItems().get(0)));
        }
        return groupingOptions;
    }

    public void upsertItem(final Context ctx, final Collection<T> items, @Nullable final T currentItem, final int dialogTitle, final int buttonTitle, final BiConsumer<String, String> onNewName) {

        final HierarchylisthelperUpsertBinding binding = HierarchylisthelperUpsertBinding.inflate(LayoutInflater.from(ctx));
        final Pair<List<String>, Map<String, String>> groupsAndMarkers = calculateGroups(items);
        final Map<String, String> markerMap = new HashMap<>(groupsAndMarkers.second);

        //parent list
        final String none = LocalizationUtils.getString(R.string.init_custombnitem_none);
        final String initialGroup = currentItem == null ? null : getParent(accessor.getFullName(currentItem));
        final String[] rawListStorage = new String[] { initialGroup };
        setParentListInView(initialGroup, none, markerMap, binding);
        binding.parentList.setOnClickListener(v -> {
            final List<String> hierarchies = new ArrayList<>(groupsAndMarkers.first);
            hierarchies.add(0, none);
            final SimpleDialog.ItemSelectModel<String> model = new SimpleDialog.ItemSelectModel<>();
            model
                .setItems(hierarchies)
                .setDisplayMapper(TextParam::text);
            if (accessor.supportsMarkers()) {
                model.setDisplayIconMapper(g -> groupToImageParam(g, markerMap));
            }
            SimpleDialog.ofContext(ctx).selectSingle(model, str -> {
                rawListStorage[0] = none.equals(str) ? null : str;
                setParentListInView(rawListStorage[0], none, markerMap, binding);
            });
        });

        //add new parent list
        binding.newParentLayout.setVisibility(View.GONE);
        binding.newParentAdd.setOnClickListener(v -> {
            binding.newParentAdd.setEnabled(false);
            binding.newParentDelete.setEnabled(true);
            binding.newParentLayout.setVisibility(View.VISIBLE);
            binding.newParent.requestFocus();
            Keyboard.show(ctx, binding.newParent);
        });
        binding.newParentDelete.setOnClickListener(v -> {
            binding.newParentAdd.setEnabled(true);
            binding.newParentDelete.setEnabled(false);
            binding.newParentLayout.setVisibility(View.GONE);
        });

        //name field
        if (currentItem != null) {
            binding.name.setText(getSimpleName(accessor.getFullName(currentItem)));
        }
        //marker
        binding.marker.setVisibility(accessor.supportsMarkers() ? View.VISIBLE : View.GONE);
        EmojiUtils.initializeEmojiMarkerButton(binding.marker, accessor.getMarker(currentItem));

        final SimpleDialog dialog = SimpleDialog.ofContext(ctx)
            .setTitle(dialogTitle)
            .setButtons(buttonTitle, android.R.string.cancel)
            .setCustomView(binding);

        dialog.confirm(() -> {
            if (StringUtils.isBlank(binding.name.getText())) {
                return;
            }
            //collect new name from dialog
            String name = binding.name.getText().toString().trim();
            if (binding.newParentLayout.getVisibility() == View.VISIBLE && !StringUtils.isBlank(binding.newParent.getText())) {
                name = binding.newParent.getText().toString().trim() + SEPARATOR + name;
            }
            if (!StringUtils.isBlank(rawListStorage[0])) {
                name = rawListStorage[0] + SEPARATOR + name;
            }

            onNewName.accept(name, accessor.supportsMarkers() && binding.marker.getText() != null ? binding.marker.getText().toString() : null);
        });

        //disable create/update button on empty name
        final Runnable checker = () -> dialog.getDialog().getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!StringUtils.isBlank(binding.name.getText()));
        checker.run();
        binding.name.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> checker.run()));
    }

    private void setParentListInView(final String text, final String none, final Map<String, String> markerMap, final HierarchylisthelperUpsertBinding binding) {
        if (text == null) {
            binding.parentList.setText(none);
            return;
        }
        final String emoji = markerMap.get(text);
        binding.parentList.setText(emoji == null ? "" : emoji + " " + text);
    }

    private ImageParam groupToImageParam(final String text, final Map<String, String> markerMap) {
        return StringUtils.isBlank(text) || !markerMap.containsKey(text) ? null : ImageParam.emoji(markerMap.get(text), 30);
    }

    @Nullable
    private ImageParam getMarkerImage(final T item) {
        final String marker = accessor.getMarker(item);
        return marker == null ? null : ImageParam.emoji(marker, 30);
    }

    @NonNull
    private String getSimpleName(@NonNull final Object obj) {
        final String str = String.valueOf(obj);
        final int idx = str.lastIndexOf(SEPARATOR);
        return idx <= 0 ? str : str.substring(idx + 1);
    }

    @Nullable
    private String getParent(@NonNull final Object obj) {
        final String str = String.valueOf(obj);
        final int idx = str.lastIndexOf(SEPARATOR);
        return idx <= 0 ? null : str.substring(0, idx);
    }

    private Comparator<Object> getGroupListSorter() {
        final Collator collator = Collator.getInstance();
        return CommonUtils.getNullHandlingComparator((g1, g2) -> {
            final String s1 = accessor.getGroupSortString(String.valueOf(g1));
            final String s2 = accessor.getGroupSortString(String.valueOf(g2));
            return collator.compare(s1 != null ? s1 : String.valueOf(g1), s2 != null ? s2 : String.valueOf(g2));
        }, true);
    }

    private Pair<List<String>, Map<String, String>> calculateGroups(final Collection<T> items) {

        final NavigableMap<String, String> markerMap = items.stream().collect(Collectors.toMap(
            accessor::getFullName,
            item -> Objects.toString(accessor.getMarker(item), ""),
            (existing, replacement) -> existing,
            TreeMap::new));
        markerMap.replaceAll((key, value) -> "".equals(value) ? null : value);


        final List<String> sortedGroups = new ArrayList<>(markerMap.keySet()).stream()
                .flatMap(key -> {
                    final List<String> subGroups = new ArrayList<>();
                    String group = getParent(key);
                    while (group != null) {
                        subGroups.add(group);

                        final String marker = markerMap.get(group);
                        if (StringUtils.isBlank(marker)) {
                            markerMap.put(group, markerMap.get(key));
                        }

                        group = getParent(group);
                    }
                    return subGroups.stream();
                })
                .distinct()
                .sorted()
                .toList();

        return new Pair<>(sortedGroups, markerMap);
    }



}
