package cgeo.geocaching.list;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.ItemGroup;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public final class StoredList extends AbstractList {
    private static final int TEMPORARY_LIST_ID = 0;
    public static final StoredList TEMPORARY_LIST = new StoredList(TEMPORARY_LIST_ID, "<temporary>", EmojiUtils.NO_EMOJI, true, 0); // Never displayed
    public static final int STANDARD_LIST_ID = 1;
    public final int markerId;
    public final boolean preventAskForDeletion;
    private final int count; // this value is only valid as long as the list is not changed by other database operations

    public StoredList(final int id, final String title, final int markerId, final boolean preventAskForDeletion, final int count) {
        super(id, title);
        this.markerId = markerId;
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
        private final Resources res;

        private static final String GROUP_SEPARATOR = ":";

        private static final String SYSTEM_GROUP_PREFIX = "system-group-";
        private static final String SELECTED_GROUP_PREFIX = "selected-group-";

        public UserInterface(@NonNull final Activity activity) {
            this.activityRef = new WeakReference<>(activity);
            res = CgeoApplication.getInstance().getResources();
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
                    .setSelectAction(TextParam.id(R.string.cache_list_select_last), () -> lastSelectedListSet)
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

            //Display for normal items
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


            //GROUPING
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
            return CommonUtils.getNullHandlingComparator((p1, p2) -> getSortString(p1, selectedIds).compareTo(getSortString(p2, selectedIds)), true);
        }

        private String getSortString(final Object item, final Set<Integer> selectedIds) {
            //Group sorting:
            // A. "Create new",
            // B. "Stored",
            // C. lists with selected items (those are not grouped)
            // D. Grouped and ungrouped lists w/o selected items (sorted alphabetically)
            // E. "All"
            // F. "History"

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
                } else if (((StoredList) item).markerId > 0) {
                    return ImageParam.emoji(((StoredList) item).markerId, 30);
                }
            } else if (item instanceof PseudoList) {
                if (item.id == PseudoList.ALL_LIST.id) {
                    return ImageParam.id(R.drawable.ic_menu_list_group);
                } else if (item.id == PseudoList.HISTORY_LIST.id) {
                    return ImageParam.id(R.drawable.ic_menu_recent_history);
                }
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
            //selected lists are not in a group
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
            final List<AbstractList> lists = new ArrayList<>(getSortedLists(selectedLists));

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

        @NonNull
        private static List<StoredList> getSortedLists(final Set<Integer> selectedLists) {
            final Collator collator = Collator.getInstance();
            final List<StoredList> lists = DataStore.getLists();
            Collections.sort(lists, (lhs, rhs) -> {
                if (selectedLists.contains(lhs.id) && !selectedLists.contains(rhs.id)) {
                    return -1;
                }
                if (selectedLists.contains(rhs.id) && !selectedLists.contains(lhs.id)) {
                    return 1;
                }
                // have the standard list at the top
                if (lhs.id == STANDARD_LIST_ID) {
                    return -1;
                }
                if (rhs.id == STANDARD_LIST_ID) {
                    return 1;
                }
                // otherwise sort alphabetical
                return collator.compare(lhs.getTitle(), rhs.getTitle());
            });
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
                    ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err));
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
                    ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err));
                }
            });
        }

        private void handleListNameInput(final String defaultValue, final int dialogTitle, final int buttonTitle, final Action1<String> runnable) {
            final Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            SimpleDialog.of(activity).setTitle(dialogTitle).setPositiveButton(TextParam.id(buttonTitle))
                    .input(new SimpleDialog.InputOptions().setInitialValue(defaultValue), input -> {
                        if (StringUtils.isNotBlank(input)) {
                            runnable.call(input);
                        }
                    });
        }

        public void promptForListRename(final int listId, @NonNull final Runnable runAfterRename) {
            final StoredList list = DataStore.getList(listId);
            handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, listName -> {
                DataStore.renameList(listId, listName);
                runAfterRename.run();
            });
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

}
