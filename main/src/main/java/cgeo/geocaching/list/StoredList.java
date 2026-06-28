package cgeo.geocaching.list;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.ItemGroup;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public final class StoredList extends AbstractList {
    private static final int TEMPORARY_LIST_ID = 0;
    public static final StoredList TEMPORARY_LIST = new StoredList(TEMPORARY_LIST_ID, "<temporary>", EmojiUtils.NO_EMOJI, true, 0); // Never displayed
    public static final int STANDARD_LIST_ID = 1;
    public final boolean preventAskForDeletion;
    /** emoji assigned to this list as its marker, or null/empty for "none" */
    @Nullable public final String emojiMarker;
    private int count; // this value is only valid as long as the list is not changed by other database operations

    public StoredList(final int id, final String title, @Nullable final String emojiMarker, final boolean preventAskForDeletion, final int count) {
        super(id, title, 0);
        this.emojiMarker = emojiMarker;
        this.preventAskForDeletion = preventAskForDeletion;
        this.count = count;
    }

    @Override
    public String getTitleAndCount() {
        return title + " [" + count + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StoredList)) {
            return false;
        }
        return id == ((StoredList) obj).id;
    }

    public static class UserInterface {
        private final WeakReference<Activity> activityRef;

        public static final String GROUP_SEPARATOR = ":";

        public UserInterface(@NonNull final Activity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final int exceptListId) {
            promptForListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.singleton(exceptListId), -1, ListNameMemento.EMPTY);
        }

        public void promptForMultiListSelection(final int titleId, @NonNull final Action1<Set<Integer>> runAfterwards, final boolean onlyConcreteLists, final Set<Integer> currentListIds, final boolean fastStoreOnLastSelection) {
            promptForMultiListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.emptySet(), currentListIds, ListNameMemento.EMPTY, fastStoreOnLastSelection);
        }

        public void promptForMultiListSelection(final int titleId, @NonNull final Action1<Set<Integer>> runAfterwards, final boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Set<Integer> currentListIds, @NonNull final ListNameMemento listNameMemento, final boolean fastStoreOnLastSelection) {
            final Set<Integer> selectedListIds = new HashSet<>(fastStoreOnLastSelection ? Settings.getLastSelectedLists() : currentListIds);
            final List<AbstractList> lists = getMenuLists(onlyConcreteLists, exceptListIds, selectedListIds);
            final Set<AbstractList> selectedListSet =
                    lists.stream().filter(s -> selectedListIds.contains(s.id)).collect(Collectors.toSet());

            final Set<Integer> lastSelectedLists = Settings.getLastSelectedLists();
            //Testing Java 8 Streaming Feature
            final Set<AbstractList> lastSelectedListSet =
                    lists.stream().filter(s -> lastSelectedLists.contains(s.id)).collect(Collectors.toSet());

            // remove from selected which are not available anymore
            final Set<Integer> allListIds = new HashSet<>(lists.size());
            for (AbstractList list : lists) {
                allListIds.add(list.id);
            }
            selectedListIds.retainAll(allListIds);

            if (fastStoreOnLastSelection && !selectedListIds.isEmpty()) {
                runAfterwards.call(selectedListIds);
                return;
            }

            final SimpleDialog.ItemSelectModel<AbstractList> model = new SimpleDialog.ItemSelectModel<>();
            model.setButtonSelectionIsMandatory(true)
                    .setSelectAction(TextParam.id(R.string.cache_list_select_last), () -> {
                        model.setSelectedItems(lastSelectedListSet);
                        configureListDisplay(model, Stream.concat(lastSelectedLists.stream(), selectedListIds.stream()).collect(Collectors.toSet()));
                        return lastSelectedListSet;
                    })
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)
                    .setItems(lists)
                    .setSelectedItems(selectedListSet);
            configureListDisplay(model, selectedListIds);

            SimpleDialog.of(activityRef.get()).setTitle(TextParam.id(titleId))
                .setNegativeButton(null)
                .selectMultiple(model, (selected) -> {
                    selectedListIds.clear();
                    for (AbstractList list : selected) {
                        selectedListIds.add(list.id);
                    }
                    if (selectedListIds.contains(PseudoList.NEW_LIST.id)) {
                        // create new list on the fly
                        promptForListCreation(runAfterwards, selectedListIds, listNameMemento.getTerm());
                    } else {
                        Settings.setLastSelectedLists(selectedListIds);
                        runAfterwards.call(selectedListIds);
                    }
                }
            );
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final Set<Integer> exceptListIds, final int preselectedListId, @Nullable final ListNameMemento listNameMemento) {
            final List<AbstractList> lists = getMenuLists(onlyConcreteLists, exceptListIds, Collections.emptySet());
            final SimpleDialog.ItemSelectModel<AbstractList> model = new SimpleDialog.ItemSelectModel<>();
            model
                .setItems(lists)
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);
            if (preselectedListId >= 0) {
                final Optional<AbstractList> selected = lists.stream().filter(list -> list.id == preselectedListId).findFirst();
                if (selected.isPresent()) {
                    model
                        .setScrollAnchor(selected.get())
                        .setSelectedItems(Collections.singleton(selected.get()))
                        .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO);
                }
            }
            configureListDisplay(model, null);

            SimpleDialog.of(activityRef.get()).setTitle(titleId).selectSingle(model, item -> {
                        if (item == PseudoList.NEW_LIST) {
                            // create new list on the fly
                            promptForListCreation(runAfterwards, listNameMemento == null ? StringUtils.EMPTY : listNameMemento.getTerm());
                        } else {
                            runAfterwards.call(item.id);
                        }
                    }
            );
        }

        private void configureListDisplay(final SimpleDialog.ItemSelectModel<AbstractList> model, final Set<Integer> selectedListIds) {

            // Display for normal items
            model.setDisplayMapper((item, itemGroup) -> {
                String title = item.getTitle();
                if (item instanceof StoredList) {
                    final String parentTitle = itemGroup == null || itemGroup.getGroup() == null ? "" : itemGroup.getGroup().toString();
                    if (title.startsWith(parentTitle + GROUP_SEPARATOR)) {
                        title = title.substring(parentTitle.length() + 1);
                    }
                    return TextParam.text(title + " [" + ((StoredList) item).count + "]");
                }
                return TextParam.text(item.getTitleAndCount());
            }, (item, itemGroup) -> item.getTitle(), null);
            model.setDisplayIconMapper((item) -> UserInterface.getImageForList(item, false));

            // GROUPING
            model.activateGrouping(item -> getGroupFromList(item, selectedListIds))
                    .setGroupGroupMapper(UserInterface::getGroupFromGroup)
                    .setItemGroupComparator(getGroupAwareListSorter(selectedListIds))
                    .setGroupDisplayMapper(gi -> {
                        final String parentGroup = gi.getParent() == null || gi.getParent().getGroup() == null ? "" : gi.getParent().getGroup();
                        String title = gi.getGroup();
                        if (title.startsWith(parentGroup + GROUP_SEPARATOR)) {
                            title = title.substring(parentGroup.length() + 1);
                        }
                        return TextParam.text("**" + title + "** *(" + gi.getContainedItemCount() + ")*").setMarkdown(true);
                    })
                    .setGroupDisplayIconMapper(gi -> gi.getItems().isEmpty() ? null : getImageForList(gi.getItems().get(0), true))
                    .setGroupPruner(gi -> gi.getSize() >= 2)
                    .setReducedGroupSaver("storedlist", g -> g, g -> g);
        }

        private Comparator<Object> getGroupAwareListSorter(final Set<Integer> selectedIds) {
            final Collator collator = Collator.getInstance();
            return CommonUtils.getNullHandlingComparator((p1, p2) -> collator.compare(getSortString(p1, selectedIds), getSortString(p2, selectedIds)), true);
        }

        private String getSortString(final Object item, final Set<Integer> selectedIds) {
            //Stored list sorting:
            // A. If displayed: "Create new"
            // B. If displayed: "Stored",
            // C. If multiselect: preselected lists in alphabetic order (those are not grouped)
            // D. Grouped and ungrouped user lists w/o selected items (each level sorted alphabetically, groups and lists intermixed)
            // E. If displayed: "All"
            // F. If displayed: "History"

            if (item instanceof ItemGroup) {
                return "D-" + ((ItemGroup<?, ?>) item).getGroup().toString();
            }
            if (!(item instanceof AbstractList)) {
                //should never happen, just a safety guard
                return "X-" + item;
            }

            final AbstractList list = (AbstractList) item;

            if (list.id == PseudoList.NEW_LIST.id) {
              return "A";
            }
            if (list.id == STANDARD_LIST_ID) {
                return "B";
            }
            if (list.id == PseudoList.ALL_LIST.id) {
                return "E";
            }
            if (list.id == PseudoList.HISTORY_LIST.id) {
                return "F";
            }
            if (selectedIds != null && selectedIds.contains(list.id)) {
                return "C-" + list.getTitle();
            }
            return "D-" + list.getTitle();
        }

        public static ImageParam getImageForList(final AbstractList item, final boolean isGroup) {
            if (item instanceof StoredList) {
                if (item.id == STANDARD_LIST_ID) {
                    return ImageParam.id(R.drawable.ic_menu_save);
                } else if (StringUtils.isNotBlank(((StoredList) item).emojiMarker)) {
                    return ImageParam.emoji(((StoredList) item).emojiMarker, 30);
                }
            } else if (item instanceof PseudoList) {
                return ImageParam.id(((PseudoList) item).drawableId);
            }
            if (isGroup) {
                return ImageParam.id(R.drawable.downloader_folder);
            }
            return ImageParam.id(R.drawable.ic_menu_list);
        }

        private static String getGroupFromList(final AbstractList item, final Set<Integer> selectedIds) {
            //only stored lists can have groups
            if (!(item instanceof StoredList)) {
                return null;
            }
            // selected lists are not in a group
            if (selectedIds != null && selectedIds.contains(item.id)) {
                return null;
            }
            return getGroupFromGroup(item.getTitle());
        }

        private static String getGroupFromGroup(final String group) {
            if (group == null) {
                return null;
            }
            final int idx = group.lastIndexOf(GROUP_SEPARATOR);
            return idx <= 0 ? null : group.substring(0, idx);
        }

        public static List<AbstractList> getMenuLists(final boolean onlyConcreteLists, final int exceptListId) {
            return getMenuLists(onlyConcreteLists, Collections.singleton(exceptListId), Collections.emptySet());
        }

        private static List<AbstractList> getMenuLists(final boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Set<Integer> selectedLists) {
            final List<AbstractList> lists = new ArrayList<>(DataStore.getLists());

            if (exceptListIds.contains(STANDARD_LIST_ID)) {
                lists.remove(DataStore.getList(STANDARD_LIST_ID));
            }

            for (final Integer exceptListId : exceptListIds) {
                if (exceptListId >= DataStore.customListIdOffset) {
                    lists.remove(DataStore.getList(exceptListId));
                }
            }

            if (!onlyConcreteLists) {
                if (!exceptListIds.contains(PseudoList.ALL_LIST.id)) {
                    lists.add(PseudoList.ALL_LIST);
                }
                if (!exceptListIds.contains(PseudoList.HISTORY_LIST.id)) {
                    lists.add(PseudoList.HISTORY_LIST);
                }
            }
            if (!exceptListIds.contains(PseudoList.NEW_LIST.id)) {
                lists.add(0, PseudoList.NEW_LIST);
            }
            return lists;
        }

        public void promptForListCreation(@NonNull final Action1<Integer> runAfterwards, final String newListName) {
            // We need to update the list cache by creating a new StoredList object here.
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, listName -> {
                final Activity activity = activityRef.get();
                if (activity == null) {
                    return;
                }
                final int newId = DataStore.createList(listName);
                new StoredList(newId, listName, EmojiUtils.NO_EMOJI, false, 0);

                if (newId >= DataStore.customListIdOffset) {
                    runAfterwards.call(newId);
                } else {
                    ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.list_dialog_create_err));
                }
            });
        }

        public void promptForListCreation(@NonNull final Action1<Set<Integer>> runAfterwards, final Set<Integer> selectedLists, final String newListName) {
            // We need to update the list cache by creating a new StoredList object here.
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, listName -> {
                final Activity activity = activityRef.get();
                if (activity == null) {
                    return;
                }
                final int newId = DataStore.createList(listName);
                new StoredList(newId, listName, EmojiUtils.NO_EMOJI, false, 0);

                if (newId >= DataStore.customListIdOffset) {
                    selectedLists.remove(PseudoList.NEW_LIST.id);
                    selectedLists.add(newId);
                    Settings.setLastSelectedLists(selectedLists);
                    runAfterwards.call(selectedLists);
                } else {
                    ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.list_dialog_create_err));
                }
            });
        }

        private void handleListNameInput(final String defaultValue, final int dialogTitle, final int buttonTitle, final Action1<String> runnable) {
            final Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            final View menu = LayoutInflater.from(activity).inflate(R.layout.createlist, null);
            final TextInputLayout parentList = menu.findViewById(R.id.parentList);
            final AutoCompleteTextView parentListView = menu.findViewById(R.id.parentListView);
            final TextInputEditText listname = menu.findViewById(R.id.title);

            final String current = defaultValue != null ? defaultValue.substring(defaultValue.lastIndexOf(GROUP_SEPARATOR) + 1).trim() : "";
            final String oldPrefix = defaultValue != null ? defaultValue.substring(0, defaultValue.length() - current.length()) : "";

            final List<String> hierarchies = DataStore.getListHierarchy();
            hierarchies.add(0, LocalizationUtils.getString(R.string.init_custombnitem_none)); // overwrite empty entry
            hierarchies.add(1, LocalizationUtils.getString(R.string.list_create_parent));
            parentList.setVisibility(View.VISIBLE);
            parentListView.setText(Strings.CS.endsWith(oldPrefix, GROUP_SEPARATOR) ? oldPrefix.substring(0, oldPrefix.length() - 1) : oldPrefix);
            parentListView.setAdapter(new NewListAdapter(activity, R.layout.createlist_item, hierarchies));

            ((EditText) menu.findViewById(R.id.title)).setText(current);
            final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
                    .setTitle(dialogTitle)
                    .setPositiveButton(buttonTitle, ((d, which) -> {
                            // same logic as in updateButtonState()
                            String prefix = "";
                        final String temp = ((AutoCompleteTextView) Objects.requireNonNull(((AlertDialog) d).findViewById(R.id.parentListView))).getText().toString().trim();
                            if (Strings.CS.equals(temp, LocalizationUtils.getString(R.string.list_create_parent))) {
                                prefix = Objects.requireNonNull(((TextInputEditText) Objects.requireNonNull(((AlertDialog) d).findViewById(R.id.newParent))).getText()).toString().trim();
                            } else if (!Strings.CS.equals(temp, LocalizationUtils.getString(R.string.init_custombnitem_none))) {
                                prefix = temp;
                            }
                            prefix += (prefix.isEmpty() || Strings.CS.endsWith(prefix, GROUP_SEPARATOR) ? "" : GROUP_SEPARATOR);
                            runnable.call(handleListNameInputHelper(prefix, ((EditText) Objects.requireNonNull(((AlertDialog) d).findViewById(R.id.title))).getText().toString().trim()));
                        }))
                    .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                    .setView(menu);
            Keyboard.show(activity, menu.findViewById(R.id.title));
            final AlertDialog dialog = builder.show();
            ((NewListAdapter) parentListView.getAdapter()).setNewParentInput(dialog.findViewById(R.id.newParentWrapper));

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            final String oldListname = Objects.requireNonNull(listname.getText()).toString();
            parentListView.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateButtonState(dialog, oldPrefix, s.toString(), oldListname, listname.getText().toString(), false)));
            ((TextInputEditText) Objects.requireNonNull(dialog.findViewById(R.id.newParent))).addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateButtonState(dialog, oldPrefix, parentListView.getText().toString(), oldListname, listname.getText().toString(), false)));
            listname.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateButtonState(dialog, oldPrefix, parentListView.getText().toString(), oldListname, s.toString(), false)));

            ViewUtils.closeKeyboardOnLosingFocus(activity, listname);
            ViewUtils.closeKeyboardOnLosingFocus(activity, menu.findViewById(R.id.newParent));
        }

        private static void updateButtonState(final AlertDialog dialog, @Nullable final String oldPrefix, @Nullable final String newPrefix, @Nullable final String oldListname, @Nullable final String newListname, final boolean ignoreEmptyPrefix) {
            boolean unchanged = true;
            boolean blocked = false;
            if (newListname != null && oldListname != null) {
                final String tempListname = newListname.trim();
                unchanged = Strings.CS.equals(oldListname, tempListname);
                blocked = tempListname.isEmpty();
            }

            if (!blocked && oldPrefix != null && newPrefix != null) {
                // same logic as in handleListNameInput:builder.setPositiveButton() above
                String prefix = "";
                final String temp = newPrefix.trim();
                if (Strings.CS.equals(temp, LocalizationUtils.getString(R.string.list_create_parent))) {
                    prefix = Objects.requireNonNull(((TextInputEditText) Objects.requireNonNull(dialog.findViewById(R.id.newParent))).getText()).toString().trim();
                    blocked = prefix.isEmpty() && !ignoreEmptyPrefix;
                } else if (!Strings.CS.equals(temp, LocalizationUtils.getString(R.string.init_custombnitem_none))) {
                    prefix = temp;
                }

                final String prefixWithSeparator = prefix + (prefix.isEmpty() || Strings.CS.endsWith(prefix, GROUP_SEPARATOR) ? "" : GROUP_SEPARATOR);
                unchanged = unchanged && (Strings.CS.equals(oldPrefix, prefix) || Strings.CS.equals(oldPrefix, prefixWithSeparator));
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!unchanged && !blocked);
        }

        public static String handleListNameInputHelper(final String selectedPrefix, final String selectedTitle) {
            final String prefix = removeSeparatorsAndTrim(selectedPrefix);
            final String title = removeSeparatorsAndTrim(selectedTitle);
            return title.isEmpty() ? prefix : prefix.isEmpty() ? title : prefix + GROUP_SEPARATOR + title;
        }

        private static String removeSeparatorsAndTrim(final String input) {
            return Arrays.stream(input.split(GROUP_SEPARATOR, -1))
                    .map(String::trim)
                    .filter(segment -> !segment.isEmpty())
                    .collect(Collectors.joining(GROUP_SEPARATOR));
        }

        public void promptForListRename(final int listId, @NonNull final Runnable runAfterRename) {
            final StoredList list = DataStore.getList(listId);
            handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, listName -> {
                DataStore.renameList(listId, listName);
                runAfterRename.run();
            });
        }

        public void promptForParentListRename(final int listId, final Runnable runAfterRename) {
            final Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            final List<String> hierarchies = DataStore.getListHierarchy();
            if (hierarchies.isEmpty()) {
                return;
            }

            if (StringUtils.isEmpty(hierarchies.get(0))) {
                hierarchies.remove(0);
            }

            final View menu = LayoutInflater.from(activity).inflate(R.layout.createlist, null);
            final TextInputLayout parentList = menu.findViewById(R.id.parentList);
            final AutoCompleteTextView parentListView = menu.findViewById(R.id.parentListView);
            final TextInputEditText title = menu.findViewById(R.id.title);

            final StoredList list = DataStore.getList(listId);
            final String parentName = StringUtils.defaultIfEmpty(getGroupFromList(list, null), hierarchies.get(0));

            parentList.setVisibility(View.VISIBLE);
            parentList.setHint(R.string.rename_from);
            parentListView.setText(parentName);
            parentListView.setAdapter(new ArrayAdapter<>(activity, R.layout.createlist_item, hierarchies));
            parentListView.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
                ((EditText) menu.findViewById(R.id.title)).setText(s);
            }));

            ((TextInputLayout) menu.findViewById(R.id.titleWrapper)).setHint(R.string.rename_to);
            title.setText(parentName);

            final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
                    .setTitle(R.string.list_menu_rename_parent_lists)
                    .setPositiveButton(android.R.string.ok, ((d, which) -> {
                        final String from = parentListView.getText().toString();
                        final String to = removeSeparatorsAndTrim(Objects.requireNonNull(title.getText()).toString());
                        if (!Strings.CS.equals(from, to)) {
                            SimpleDialog.of(activity).setTitle(R.string.list_menu_rename_parent_lists).setMessage(TextParam.text(
                                    LocalizationUtils.getString(R.string.list_confirm_rename, from, to, to.isEmpty() ? LocalizationUtils.getString(R.string.list_confirm_no_hierarchy) : ""))
                                ).confirm(() -> {
                                    DataStore.renameListPrefix(from + GROUP_SEPARATOR, to + (to.isEmpty() ? "" : GROUP_SEPARATOR));
                                    runAfterRename.run();
                                });
                            }
                        }))
                    .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                    .setView(menu);
            Keyboard.show(activity, title);
            final AlertDialog dialog = builder.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            title.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateButtonState(dialog, parentListView.getText().toString(), title.getText().toString(), null, null, true)));

            ViewUtils.closeKeyboardOnLosingFocus(activity, title);
        }
    }


    /**
     * Get the list title.
     */
    @Override
    @NonNull
    public String getTitle() {
        return title;
    }

    @Override
    public int getNumberOfCaches() {
        return count;
    }

    @Override
    public void updateNumberOfCaches() {
        count = DataStore.getList(id).count;
    }

    /**
     * Return the given list, if it is a concrete list. Return the default list otherwise.
     */
    public static int getConcreteList(final int listId) {
        if (listId == PseudoList.ALL_LIST.id || listId == TEMPORARY_LIST.id || listId == PseudoList.HISTORY_LIST.id) {
            return STANDARD_LIST_ID;
        }
        return listId;
    }

    @Override
    public boolean isConcrete() {
        return true;
    }

    /** enable/disable given input field (for new parent list name) on tapping the "create new parent" entry (= entry on position 1) */
    private static class NewListAdapter extends ArrayAdapter<String> {

        View newParentInput = null;

        NewListAdapter(final @NonNull Context context, final int resource, final @NonNull List<String> objects) {
            super(context, resource, objects);
        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public View getView(final int position, final @Nullable View convertView, final @NonNull ViewGroup parent) {
            final View v = super.getView(position, convertView, parent);
            v.setOnTouchListener((view, motionEvent) -> {
                ViewUtils.setVisibility(newParentInput, position == 1 ? View.VISIBLE : View.GONE); // pos 1 is "new parent list"
                return false;
            });
            return v;
        }

        public void setNewParentInput(final View newParentInput) {
            this.newParentInput = newParentInput;
        }
    }
}
