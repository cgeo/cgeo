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

package cgeo.geocaching.list

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.CommonUtils
import cgeo.geocaching.utils.EmojiUtils
import cgeo.geocaching.utils.ItemGroup
import cgeo.geocaching.utils.functions.Action1

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog

import java.lang.ref.WeakReference
import java.text.Collator
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashSet
import java.util.List
import java.util.Objects
import java.util.Optional
import java.util.Set
import java.util.stream.Collectors
import java.util.stream.Stream

import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.apache.commons.lang3.StringUtils

class StoredList : AbstractList() {
    private static val TEMPORARY_LIST_ID: Int = 0
    public static val TEMPORARY_LIST: StoredList = StoredList(TEMPORARY_LIST_ID, "<temporary>", EmojiUtils.NO_EMOJI, true, 0); // Never displayed
    public static val STANDARD_LIST_ID: Int = 1
    public final Boolean preventAskForDeletion
    private Int count; // this value is only valid as Long as the list is not changed by other database operations

    public StoredList(final Int id, final String title, final Int markerId, final Boolean preventAskForDeletion, final Int count) {
        super(id, title, markerId)
        this.preventAskForDeletion = preventAskForDeletion
        this.count = count
    }

    override     public String getTitleAndCount() {
        return title + " [" + count + "]"
    }

    override     public Int hashCode() {
        val prime: Int = 31
        Int result = 1
        result = prime * result + id
        return result
    }

    override     public Boolean equals(final Object obj) {
        if (this == obj) {
            return true
        }
        if (!(obj is StoredList)) {
            return false
        }
        return id == ((StoredList) obj).id
    }

    public static class UserInterface {
        private final WeakReference<Activity> activityRef
        private final Resources res

        private static val GROUP_SEPARATOR: String = ":"

        public UserInterface(final Activity activity) {
            this.activityRef = WeakReference<>(activity)
            res = CgeoApplication.getInstance().getResources()
        }

        public Unit promptForListSelection(final Int titleId, final Action1<Integer> runAfterwards, final Boolean onlyConcreteLists, final Int exceptListId) {
            promptForListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.singleton(exceptListId), -1, ListNameMemento.EMPTY)
        }

        public Unit promptForMultiListSelection(final Int titleId, final Action1<Set<Integer>> runAfterwards, final Boolean onlyConcreteLists, final Set<Integer> currentListIds, final Boolean fastStoreOnLastSelection) {
            promptForMultiListSelection(titleId, runAfterwards, onlyConcreteLists, Collections.emptySet(), currentListIds, ListNameMemento.EMPTY, fastStoreOnLastSelection)
        }

        public Unit promptForMultiListSelection(final Int titleId, final Action1<Set<Integer>> runAfterwards, final Boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Set<Integer> currentListIds, final ListNameMemento listNameMemento, final Boolean fastStoreOnLastSelection) {
            val selectedListIds: Set<Integer> = HashSet<>(fastStoreOnLastSelection ? Settings.getLastSelectedLists() : currentListIds)
            val lists: List<AbstractList> = getMenuLists(onlyConcreteLists, exceptListIds, selectedListIds)
            val selectedListSet: Set<AbstractList> =
                    lists.stream().filter(s -> selectedListIds.contains(s.id)).collect(Collectors.toSet())

            val lastSelectedLists: Set<Integer> = Settings.getLastSelectedLists()
            //Testing Java 8 Streaming Feature
            val lastSelectedListSet: Set<AbstractList> =
                    lists.stream().filter(s -> lastSelectedLists.contains(s.id)).collect(Collectors.toSet())

            // remove from selected which are not available anymore
            val allListIds: Set<Integer> = HashSet<>(lists.size())
            for (AbstractList list : lists) {
                allListIds.add(list.id)
            }
            selectedListIds.retainAll(allListIds)

            if (fastStoreOnLastSelection && !selectedListIds.isEmpty()) {
                runAfterwards.call(selectedListIds)
                return
            }

            final SimpleDialog.ItemSelectModel<AbstractList> model = SimpleDialog.ItemSelectModel<>()
            model.setButtonSelectionIsMandatory(true)
                    .setSelectAction(TextParam.id(R.string.cache_list_select_last), () -> {
                        model.setSelectedItems(lastSelectedListSet)
                        configureListDisplay(model, Stream.concat(lastSelectedLists.stream(), selectedListIds.stream()).collect(Collectors.toSet()))
                        return lastSelectedListSet
                    })
                    .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)
                    .setItems(lists)
                    .setSelectedItems(selectedListSet)
            configureListDisplay(model, selectedListIds)

            SimpleDialog.of(activityRef.get()).setTitle(TextParam.id(titleId))
                .setNegativeButton(null)
                .selectMultiple(model, (selected) -> {
                    selectedListIds.clear()
                    for (AbstractList list : selected) {
                        selectedListIds.add(list.id)
                    }
                    if (selectedListIds.contains(PseudoList.NEW_LIST.id)) {
                        // create list on the fly
                        promptForListCreation(runAfterwards, selectedListIds, listNameMemento.getTerm())
                    } else {
                        Settings.setLastSelectedLists(selectedListIds)
                        runAfterwards.call(selectedListIds)
                    }
                }
            )
        }

        public Unit promptForListSelection(final Int titleId, final Action1<Integer> runAfterwards, final Boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Int preselectedListId, final ListNameMemento listNameMemento) {
            val lists: List<AbstractList> = getMenuLists(onlyConcreteLists, exceptListIds, Collections.emptySet())
            final SimpleDialog.ItemSelectModel<AbstractList> model = SimpleDialog.ItemSelectModel<>()
            model
                .setItems(lists)
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
            if (preselectedListId >= 0) {
                val selected: Optional<AbstractList> = lists.stream().filter(list -> list.id == preselectedListId).findFirst()
                if (selected.isPresent()) {
                    model
                        .setScrollAnchor(selected.get())
                        .setSelectedItems(Collections.singleton(selected.get()))
                        .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO)
                }
            }
            configureListDisplay(model, null)

            SimpleDialog.of(activityRef.get()).setTitle(titleId).selectSingle(model, item -> {
                        if (item == PseudoList.NEW_LIST) {
                            // create list on the fly
                            promptForListCreation(runAfterwards, listNameMemento == null ? StringUtils.EMPTY : listNameMemento.getTerm())
                        } else {
                            runAfterwards.call(item.id)
                        }
                    }
            )
        }

        private Unit configureListDisplay(final SimpleDialog.ItemSelectModel<AbstractList> model, final Set<Integer> selectedListIds) {

            //Display for normal items
            model.setDisplayMapper((item, itemGroup) -> {
                String title = item.getTitle()
                if (item is StoredList) {
                    val parentTitle: String = itemGroup == null || itemGroup.getGroup() == null ? "" : itemGroup.getGroup().toString()
                    if (title.startsWith(parentTitle + GROUP_SEPARATOR)) {
                        title = title.substring(parentTitle.length() + 1)
                    }
                    return TextParam.text(title + " [" + ((StoredList) item).count + "]")
                }
                return TextParam.text(item.getTitleAndCount())
            }, (item, itemGroup) -> item.getTitle(), null)
            model.setDisplayIconMapper((item) -> UserInterface.getImageForList(item, false))


            //GROUPING
            model.activateGrouping(item -> getGroupFromList(item, selectedListIds))
                    .setGroupGroupMapper(UserInterface::getGroupFromGroup)
                    .setItemGroupComparator(getGroupAwareListSorter(selectedListIds))
                    .setGroupDisplayMapper(gi -> {
                        val parentGroup: String = gi.getParent() == null || gi.getParent().getGroup() == null ? "" : gi.getParent().getGroup()
                        String title = gi.getGroup()
                        if (title.startsWith(parentGroup + GROUP_SEPARATOR)) {
                            title = title.substring(parentGroup.length() + 1)
                        }
                        return TextParam.text("**" + title + "** *(" + gi.getContainedItemCount() + ")*").setMarkdown(true)
                    })
                    .setGroupDisplayIconMapper(gi -> gi.getItems().isEmpty() ? null : getImageForList(gi.getItems().get(0), true))
                    .setGroupPruner(gi -> gi.getSize() >= 2)
                    .setReducedGroupSaver("storedlist", g -> g, g -> g)
        }


        private Comparator<Object> getGroupAwareListSorter(final Set<Integer> selectedIds) {
            val collator: Collator = Collator.getInstance()
            return CommonUtils.getNullHandlingComparator((p1, p2) -> collator.compare(getSortString(p1, selectedIds), getSortString(p2, selectedIds)), true)
        }

        private String getSortString(final Object item, final Set<Integer> selectedIds) {
            //Stored list sorting:
            // A. If displayed: "Create new"
            // B. If displayed: "Stored",
            // C. If multiselect: preselected lists in alphabetic order (those are not grouped)
            // D. Grouped and ungrouped user lists w/o selected items (each level sorted alphabetically, groups and lists intermixed)
            // E. If displayed: "All"
            // F. If displayed: "History"

            if (item is ItemGroup) {
                return "D-" + ((ItemGroup<?, ?>) item).getGroup().toString()
            }
            if (!(item is AbstractList)) {
                //should never happen, just a safety guard
                return "X-" + item
            }

            val list: AbstractList = (AbstractList) item

            if (list.id == PseudoList.NEW_LIST.id) {
              return "A"
            }
            if (list.id == STANDARD_LIST_ID) {
                return "B"
            }
            if (list.id == PseudoList.ALL_LIST.id) {
                return "E"
            }
            if (list.id == PseudoList.HISTORY_LIST.id) {
                return "F"
            }
            if (selectedIds != null && selectedIds.contains(list.id)) {
                return "C-" + list.getTitle()
            }
            return "D-" + list.getTitle()
        }

        public static ImageParam getImageForList(final AbstractList item, final Boolean isGroup) {
            if (item is StoredList) {
                if (item.id == STANDARD_LIST_ID) {
                    return ImageParam.id(R.drawable.ic_menu_save)
                } else if (((StoredList) item).markerId > 0) {
                    return ImageParam.emoji(((StoredList) item).markerId, 30)
                }
            } else if (item is PseudoList) {
                return ImageParam.id(item.markerId)
            }
            if (isGroup) {
                return ImageParam.id(R.drawable.downloader_folder)
            }
            return ImageParam.id(R.drawable.ic_menu_list)
        }

        private static String getGroupFromList(final AbstractList item, final Set<Integer> selectedIds) {
            //only stored lists can have groups
            if (!(item is StoredList)) {
                return null
            }
            //selected lists are not in a group
            if (selectedIds != null && selectedIds.contains(item.id)) {
                return null
            }
            return getGroupFromGroup(item.getTitle())
        }

        private static String getGroupFromGroup(final String group) {
            if (group == null) {
                return null
            }
            val idx: Int = group.lastIndexOf(GROUP_SEPARATOR)
            return idx <= 0 ? null : group.substring(0, idx)
        }

        public static List<AbstractList> getMenuLists(final Boolean onlyConcreteLists, final Int exceptListId) {
            return getMenuLists(onlyConcreteLists, Collections.singleton(exceptListId), Collections.emptySet())
        }

        private static List<AbstractList> getMenuLists(final Boolean onlyConcreteLists, final Set<Integer> exceptListIds, final Set<Integer> selectedLists) {
            val lists: List<AbstractList> = ArrayList<>(DataStore.getLists())

            if (exceptListIds.contains(STANDARD_LIST_ID)) {
                lists.remove(DataStore.getList(STANDARD_LIST_ID))
            }

            for (final Integer exceptListId : exceptListIds) {
                if (exceptListId >= DataStore.customListIdOffset) {
                    lists.remove(DataStore.getList(exceptListId))
                }
            }

            if (!onlyConcreteLists) {
                if (!exceptListIds.contains(PseudoList.ALL_LIST.id)) {
                    lists.add(PseudoList.ALL_LIST)
                }
                if (!exceptListIds.contains(PseudoList.HISTORY_LIST.id)) {
                    lists.add(PseudoList.HISTORY_LIST)
                }
            }
            if (!exceptListIds.contains(PseudoList.NEW_LIST.id)) {
                lists.add(0, PseudoList.NEW_LIST)
            }
            return lists
        }

        public Unit promptForListCreation(final Action1<Integer> runAfterwards, final String newListName) {
            // We need to update the list cache by creating a StoredList object here.
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, listName -> {
                val activity: Activity = activityRef.get()
                if (activity == null) {
                    return
                }
                val newId: Int = DataStore.createList(listName)
                StoredList(newId, listName, EmojiUtils.NO_EMOJI, false, 0)

                if (newId >= DataStore.customListIdOffset) {
                    runAfterwards.call(newId)
                } else {
                    ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err))
                }
            })
        }

        public Unit promptForListCreation(final Action1<Set<Integer>> runAfterwards, final Set<Integer> selectedLists, final String newListName) {
            // We need to update the list cache by creating a StoredList object here.
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, listName -> {
                val activity: Activity = activityRef.get()
                if (activity == null) {
                    return
                }
                val newId: Int = DataStore.createList(listName)
                StoredList(newId, listName, EmojiUtils.NO_EMOJI, false, 0)

                if (newId >= DataStore.customListIdOffset) {
                    selectedLists.remove(PseudoList.NEW_LIST.id)
                    selectedLists.add(newId)
                    Settings.setLastSelectedLists(selectedLists)
                    runAfterwards.call(selectedLists)
                } else {
                    ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err))
                }
            })
        }

        private Unit handleListNameInput(final String defaultValue, final Int dialogTitle, final Int buttonTitle, final Action1<String> runnable) {
            val activity: Activity = activityRef.get()
            if (activity == null) {
                return
            }

            val menu: View = LayoutInflater.from(activity).inflate(R.layout.createlist, null)
            val listprefix: TextInputLayout = menu.findViewById(R.id.listprefix)
            val listprefixView: AutoCompleteTextView = menu.findViewById(R.id.listprefixView)

            val current: String = defaultValue != null ? defaultValue.substring(defaultValue.lastIndexOf(GROUP_SEPARATOR) + 1).trim() : ""

            val hierarchies: List<String> = DataStore.getListHierarchy()
            if (hierarchies.isEmpty()) {
                hierarchies.add(0, activity.getString(R.string.init_custombnitem_none)); // overwrite empty entry
            } else {
                hierarchies.set(0, activity.getString(R.string.init_custombnitem_none)); // overwrite empty entry
            }
            hierarchies.add(1, activity.getString(R.string.list_create_parent))
            listprefix.setVisibility(View.VISIBLE)
            listprefixView.setText(defaultValue != null ? defaultValue.substring(0, defaultValue.length() - current.length()) : "")
            listprefixView.setAdapter(NewListAdapter(activity, R.layout.createlist_item , hierarchies))

            ((EditText) menu.findViewById(R.id.title)).setText(current)
            final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
                    .setTitle(dialogTitle)
                    .setPositiveButton(buttonTitle, ((d, which) -> {
                            String prefix = ""
                            val temp: String = ((AutoCompleteTextView) Objects.requireNonNull(((AlertDialog) d).findViewById(R.id.listprefixView))).getText().toString()
                            if (StringUtils == (temp, activity.getString(R.string.list_create_parent))) {
                                prefix = Objects.requireNonNull(((TextInputEditText) Objects.requireNonNull(((AlertDialog) d).findViewById(R.id.newParent))).getText()).toString()
                                if (!StringUtils.endsWith(prefix.trim(), GROUP_SEPARATOR)) {
                                    prefix = prefix.trim() + GROUP_SEPARATOR
                                }
                            } else if (!StringUtils == (temp, activity.getString(R.string.init_custombnitem_none))) {
                                prefix = temp
                            }
                            runnable.call(prefix + ((EditText) Objects.requireNonNull(((AlertDialog) d).findViewById(R.id.title))).getText().toString())
                        }))
                    .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                    .setView(menu)
            Keyboard.show(activity, menu.findViewById(R.id.title))
            val dialog: AlertDialog = builder.show()
            ((NewListAdapter) listprefixView.getAdapter()).setNewParentInput(dialog.findViewById(R.id.newParentWrapper))
        }

        public Unit promptForListRename(final Int listId, final Runnable runAfterRename) {
            val list: StoredList = DataStore.getList(listId)
            handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, listName -> {
                DataStore.renameList(listId, listName)
                runAfterRename.run()
            })
        }

        public Unit promptForListPrefixRename(final Runnable runAfterRename) {
            val activity: Activity = activityRef.get()
            if (activity == null) {
                return
            }

            val hierarchies: List<String> = DataStore.getListHierarchy()
            if (hierarchies.size() == 1) {
                return
            }

            if (StringUtils.isEmpty(hierarchies.get(0))) {
                hierarchies.remove(0)
            }

            val menu: View = LayoutInflater.from(activity).inflate(R.layout.createlist, null)
            val listprefix: TextInputLayout = menu.findViewById(R.id.listprefix)
            val listprefixView: AutoCompleteTextView = menu.findViewById(R.id.listprefixView)
            val title: TextInputEditText = menu.findViewById(R.id.title)

            listprefix.setVisibility(View.VISIBLE)
            listprefix.setHint(R.string.rename_from)
            listprefixView.setText(hierarchies.get(0))
            listprefixView.setAdapter(ArrayAdapter<>(activity, R.layout.createlist_item , hierarchies))

            ((TextInputLayout) menu.findViewById(R.id.titleWrapper)).setHint(R.string.rename_to)
            title.setText(hierarchies.get(0))

            final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
                    .setTitle(R.string.list_menu_rename_list_prefix)
                    .setPositiveButton(android.R.string.ok, ((d, which) -> {
                        val from: String = listprefixView.getText().toString()
                        val to: String = title.getText().toString()
                        if (!StringUtils == (from, to)) {
                            SimpleDialog.of(activity).setTitle(R.string.list_menu_rename_list_prefix).setMessage(TextParam.text(
                                    String.format(activity.getString(R.string.list_confirm_rename), from, to, to.lastIndexOf(GROUP_SEPARATOR) < 0 ? activity.getString(R.string.list_confirm_no_hierarchy) : ""))
                                ).confirm(() -> {
                                    DataStore.renameListPrefix(from, to)
                                    runAfterRename.run()
                                })
                            }
                        }))
                    .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                    .setView(menu)
            Keyboard.show(activity, title)
            val dialog: AlertDialog = builder.show()

            listprefixView.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
                ((EditText) menu.findViewById(R.id.title)).setText(s)
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0)
            }))
        }

    }

    /**
     * Get the list title.
     */
    override     public String getTitle() {
        return title
    }

    override     public Int getNumberOfCaches() {
        return count
    }

    override     public Unit updateNumberOfCaches() {
        count = DataStore.getList(id).count
    }

    /**
     * Return the given list, if it is a concrete list. Return the default list otherwise.
     */
    public static Int getConcreteList(final Int listId) {
        if (listId == PseudoList.ALL_LIST.id || listId == TEMPORARY_LIST.id || listId == PseudoList.HISTORY_LIST.id) {
            return STANDARD_LIST_ID
        }
        return listId
    }

    override     public Boolean isConcrete() {
        return true
    }

    /** enable/disable given input field (for parent list name) on tapping the "create parent" entry (= entry on position 1) */
    private static class NewListAdapter : ArrayAdapter()<String> {

        View newParentInput = null

        NewListAdapter(final Context context, final Int resource, final List<String> objects) {
            super(context, resource, objects)
        }

        @SuppressLint("ClickableViewAccessibility")
        override         public View getView(final Int position, final View convertView, final ViewGroup parent) {
            val v: View = super.getView(position, convertView, parent)
            v.setOnTouchListener((view, motionEvent) -> {
                ViewUtils.setVisibility(newParentInput, position == 1 ? View.VISIBLE : View.GONE); // pos 1 is "parent list"
                return false
            })
            return v
        }

        public Unit setNewParentInput(final View newParentInput) {
            this.newParentInput = newParentInput
        }
    }
}
