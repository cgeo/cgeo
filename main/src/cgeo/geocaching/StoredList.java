package cgeo.geocaching;

import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.utils.RunnableWithArgument;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class StoredList {
    public static final int TEMPORARY_LIST_ID = 0;
    public static final int STANDARD_LIST_ID = 1;

    public final int id;
    public final String title;
    private final int count; // this value is only valid as long as the list is not changed by other database operations

    public StoredList(int id, String title, int count) {
        this.id = id;
        this.title = title;
        this.count = count;
    }

    public String getTitleAndCount() {
        return title + " [" + count + "]";
    }

    public static class UserInterface {
        private final IAbstractActivity activity;
        private final cgeoapplication app;
        private final Resources res;

        public UserInterface(final IAbstractActivity activity) {
            this.activity = activity;
            app = cgeoapplication.getInstance();
            res = app.getResources();
        }

        public void promptForListSelection(final int titleId, final RunnableWithArgument<Integer> runAfterwards) {
            final List<StoredList> lists = app.getLists();

            if (lists == null) {
                return;
            }

            final List<CharSequence> listsTitle = new ArrayList<CharSequence>();
            for (StoredList list : lists) {
                listsTitle.add(list.getTitleAndCount());
            }
            listsTitle.add("<" + res.getString(R.string.list_menu_create) + ">");

            final CharSequence[] items = new CharSequence[listsTitle.size()];

            AlertDialog.Builder builder = new AlertDialog.Builder((Activity) activity);
            builder.setTitle(res.getString(titleId));
            builder.setItems(listsTitle.toArray(items), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int itemId) {
                    if (itemId >= lists.size()) {
                        // create new list on the fly
                        promptForListCreation(runAfterwards);
                    }
                    else {
                        if (runAfterwards != null) {
                            runAfterwards.run(lists.get(itemId).id);
                        }
                    }
                }
            });
            builder.create().show();
        }

        public void promptForListCreation(final RunnableWithArgument<Integer> runAfterwards) {
            handleListNameInput("", R.string.list_dialog_create_title, R.string.list_dialog_create, new RunnableWithArgument<String>() {

                @Override
                public void run(final String listName) {
                    final int newId = app.createList(listName);

                    if (newId >= cgData.customListIdOffset) {
                        activity.showToast(res.getString(R.string.list_dialog_create_ok));
                        if (runAfterwards != null) {
                            runAfterwards.run(newId);
                        }
                    } else {
                        activity.showToast(res.getString(R.string.list_dialog_create_err));
                    }
                }
            });
        }

        private void handleListNameInput(final String defaultValue, int dialogTitle, int buttonTitle, final RunnableWithArgument<String> runnable) {
            final AlertDialog.Builder alert = new AlertDialog.Builder((Activity) activity);
            final View view = ((Activity) activity).getLayoutInflater().inflate(R.layout.list_create_dialog, null);
            final EditText input = (EditText) view.findViewById(R.id.text);
            input.setText(defaultValue);

            alert.setTitle(dialogTitle);
            alert.setView(view);
            alert.setPositiveButton(buttonTitle, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // remove whitespaces added by autocompletion of Android keyboard
                    String listName = StringUtils.trim(input.getText().toString());
                    if (StringUtils.isNotBlank(listName)) {
                        runnable.run(listName);
                    }
                }
            });
            alert.setNegativeButton(res.getString(R.string.list_dialog_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });

            alert.show();
        }

        public void promptForListRename(final int listId, final Runnable runAfterRename) {
            final StoredList list = app.getList(listId);
            handleListNameInput(list.title, R.string.list_dialog_rename_title, R.string.list_dialog_rename, new RunnableWithArgument<String>() {

                @Override
                public void run(final String listName) {
                    app.renameList(listId, listName);
                    if (runAfterRename != null) {
                        runAfterRename.run();
                    }
                }
            });
        }
    }
}
