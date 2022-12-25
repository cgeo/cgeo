package cgeo.geocaching.contacts;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/** Provides access to Android Contacts */
public class ContactsHelper {

    public static final boolean LEGACY_MODE = true; //TODO: can be set to false when SDKmin=23, see #13777

    private final Activity activity;

    public ContactsHelper(final Activity activity) {
        this.activity = activity;
    }

    /**
     * opens contact card for given user nickname. Caller has to ensure that necessary permissions exist
     */
    public void openContactCard(@NonNull final String userName) {
        if (!PermissionContext.SEARCH_USER_IN_CONTACTS.hasAllPermissions()) {
            return;
        }
        searchUser(userName);
    }


    @NonNull
    private List<Pair<Integer, String>> findContacts(@NonNull final String searchName, final Uri uri, @NonNull final String idColumnName, @NonNull final String selectionColumnName, final boolean like) {
        final String[] projection = {idColumnName, selectionColumnName, ContactsContract.Contacts.DISPLAY_NAME};
        final String selection = selectionColumnName + (like ? " LIKE" : " =") + " ? COLLATE NOCASE";
        final String[] selectionArgs = {like ? "%" + searchName + "%" : searchName};

        final List<Pair<Integer, String>> result = new ArrayList<>();
        try (Cursor cursor = activity.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            while (cursor != null && cursor.moveToNext()) {
                final int foundId = cursor.getInt(0);
                final String foundName = cursor.getString(1);
                final String displayName = cursor.getString(2);
                result.add(new Pair<>(foundId, StringUtils.isNotEmpty(displayName) &&
                        !StringUtils.equalsIgnoreCase(foundName, displayName) ? foundName + " (" + displayName + ")" : foundName));
            }
        } catch (final Exception e) {
            Log.e("ContactsHelper.findContacts", e);
        }
        return result;
    }


    public void searchUser(final String userName) {

        if (StringUtils.isBlank(userName)) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.contact_not_found, userName));
            return;
        }

        // search by nickname, exact
        List<Pair<Integer, String>> contacts = findContacts(userName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME, false);

        // search by display name, exact
        if (contacts.isEmpty()) {
            contacts = findContacts(userName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME, false);
        }

        // search by contained name parts
        if (contacts.isEmpty()) {
            contacts.addAll(findContacts(userName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME, true));
            contacts.addAll(findContacts(userName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME, true));
        }

        if (contacts.isEmpty()) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.contact_not_found, userName));
            return;
        }

        if (contacts.size() > 1) {
            selectContact(contacts);
        } else {
            final int contactId = contacts.get(0).first;
            openContact(contactId);
        }
    }

    private void selectContact(@NonNull final List<Pair<Integer, String>> contacts) {
        SimpleDialog.of(activity)
                .setTitle(R.string.contact_multiple_matches)
                .selectSingle(contacts, (i, p) -> TextParam.text(i.second), -1, SimpleDialog.SingleChoiceMode.NONE, (i, p) -> {
                    final int contactId = i.first;
                    openContact(contactId);
                });
    }

    /** Opens a contact in Android with given id */
    private void openContact(final int id) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
        intent.setData(uri);
        activity.startActivity(intent);
    }

}
