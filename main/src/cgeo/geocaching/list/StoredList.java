package cgeo.geocaching.list;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.ui.dialog.Dialogs;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.functions.Action1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class StoredList extends AbstractList {
    private static final int TEMPORARY_LIST_ID = 0;
    public static final StoredList TEMPORARY_LIST = new StoredList(TEMPORARY_LIST_ID, "<temporary>", 0); // Never displayed
    public static final int STANDARD_LIST_ID = 1;
    private final int count; // this value is only valid as long as the list is not changed by other database operations

    public StoredList(final int id, final String title, final int count) {
        super(id, title);
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
        private final CgeoApplication app;
        private final Resources res;

        public UserInterface(final @NonNull Activity activity) {
            this.activityRef = new WeakReference<>(activity);
            app = CgeoApplication.getInstance();
            res = app.getResources();
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final int exceptListId) {
            promptForListSelection(titleId, runAfterwards, onlyConcreteLists, exceptListId, ListNameMemento.EMPTY);
        }

        public void promptForListSelection(final int titleId, @NonNull final Action1<Integer> runAfterwards, final boolean onlyConcreteLists, final int exceptListId, final @NonNull ListNameMemento listNameMemento) {
            final List<AbstractList> lists = getMenuLists(onlyConcreteLists, exceptListId);

            final List<CharSequence> listsTitle = new ArrayList<>();
            for (final AbstractList list : lists) {
                listsTitle.add(list.getTitleAndCount());
            }

            final CharSequence[] items = new CharSequence[listsTitle.size()];

            final Activity activity = activityRef.get();
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(res.getString(titleId));
            builder.setItems(listsTitle.toArray(items), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int itemId) {
                    final AbstractList list = lists.get(itemId);
                    if (list == PseudoList.NEW_LIST) {
                        // create new list on the fly
                        promptForListCreation(runAfterwards, listNameMemento.getTerm());
                    }
                    else {
                        runAfterwards.call(lists.get(itemId).id);
                    }
                }
            });
            builder.create().show();
        }

        public static List<AbstractList> getMenuLists(final boolean onlyConcreteLists, final int exceptListId) {
            final List<AbstractList> lists = new ArrayList<>();
            lists.addAll(getSortedLists());

            if (exceptListId == StoredList.STANDARD_LIST_ID || exceptListId >= DataStore.customListIdOffset) {
                final StoredList exceptList = DataStore.getList(exceptListId);
                lists.remove(exceptList);
            }

            if (!onlyConcreteLists) {
                if (exceptListId != PseudoList.ALL_LIST.id) {
                    lists.add(PseudoList.ALL_LIST);
                }
                if (exceptListId != PseudoList.HISTORY_LIST.id) {
                    lists.add(PseudoList.HISTORY_LIST);
                }
            }
            if (exceptListId != PseudoList.NEW_LIST.id) {
                lists.add(PseudoList.NEW_LIST);
            }
            return lists;
        }

        @NonNull
        private static List<StoredList> getSortedLists() {
            final Collator collator = Collator.getInstance();
            final List<StoredList> lists = DataStore.getLists();
            Collections.sort(lists, new Comparator<StoredList>() {

                @Override
                public int compare(final StoredList lhs, final StoredList rhs) {
                    // have the standard list at the top
                    if (lhs.id == STANDARD_LIST_ID) {
                        return -1;
                    }
                    if (rhs.id == STANDARD_LIST_ID) {
                        return 1;
                    }
                    // otherwise sort alphabetical
                    return collator.compare(lhs.getTitle(), rhs.getTitle());
                }
            });
            return lists;
        }

        public void promptForListCreation(@NonNull final Action1<Integer> runAfterwards, final String newListName) {
            handleListNameInput(newListName, R.string.list_dialog_create_title, R.string.list_dialog_create, new Action1<String>() {

                // We need to update the list cache by creating a new StoredList object here.
                @SuppressWarnings("unused")
                @Override
                public void call(final String listName) {
                    final Activity activity = activityRef.get();
                    if (activity == null) {
                        return;
                    }
                    final int newId = DataStore.createList(listName);
                    new StoredList(newId, listName, 0);

                    if (newId >= DataStore.customListIdOffset) {
                        ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_ok));
                        runAfterwards.call(newId);
                    } else {
                        ActivityMixin.showToast(activity, res.getString(R.string.list_dialog_create_err));
                    }
                }
            });
        }

        private void handleListNameInput(final String defaultValue, final int dialogTitle, final int buttonTitle, final Action1<String> runnable) {
            final Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            Dialogs.input(activity, dialogTitle, defaultValue, buttonTitle, new Action1<String>() {

                @Override
                public void call(final String input) {
                    // remove whitespaces added by autocompletion of Android keyboard
                    final String listName = StringUtils.trim(input);
                    if (StringUtils.isNotBlank(listName)) {
                        runnable.call(listName);
                    }
                }
            });
        }

        public void promptForListRename(final int listId, @NonNull final Runnable runAfterRename) {
            final StoredList list = DataStore.getList(listId);
            handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, new Action1<String>() {

                @Override
                public void call(final String listName) {
                    DataStore.renameList(listId, listName);
                    runAfterRename.run();
                }
            });
        }

    }

    /**
     * Get the list title.
     */
    @Override
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
