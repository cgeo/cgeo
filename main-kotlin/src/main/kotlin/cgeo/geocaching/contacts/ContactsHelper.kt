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

package cgeo.geocaching.contacts

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.permission.PermissionContext
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.util.Pair

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils

/** Provides access to Android Contacts */
class ContactsHelper {

    private final Activity activity

    public ContactsHelper(final Activity activity) {
        this.activity = activity
    }

    /**
     * opens contact card for given user nickname. Caller has to ensure that necessary permissions exist
     */
    public Unit openContactCard(final String userName) {
        if (!PermissionContext.SEARCH_USER_IN_CONTACTS.hasAllPermissions()) {
            return
        }
        searchUser(userName)
    }


    private List<Pair<Integer, String>> findContacts(final String searchName, final Uri uri, final String idColumnName, final String selectionColumnName, final Boolean like) {
        final String[] projection = {idColumnName, selectionColumnName, ContactsContract.Contacts.DISPLAY_NAME}
        val selection: String = selectionColumnName + (like ? " LIKE" : " =") + " ? COLLATE NOCASE"
        final String[] selectionArgs = {like ? "%" + searchName + "%" : searchName}

        final List<Pair<Integer, String>> result = ArrayList<>()
        try (Cursor cursor = activity.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            while (cursor != null && cursor.moveToNext()) {
                val foundId: Int = cursor.getInt(0)
                val foundName: String = cursor.getString(1)
                val displayName: String = cursor.getString(2)
                result.add(Pair<>(foundId, StringUtils.isNotEmpty(displayName) &&
                        !StringUtils.equalsIgnoreCase(foundName, displayName) ? foundName + " (" + displayName + ")" : foundName))
            }
        } catch (final Exception e) {
            Log.e("ContactsHelper.findContacts", e)
        }
        return result
    }


    public Unit searchUser(final String userName) {

        if (StringUtils.isBlank(userName)) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.contact_not_found, userName))
            return
        }

        // search by nickname, exact
        List<Pair<Integer, String>> contacts = findContacts(userName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME, false)

        // search by display name, exact
        if (contacts.isEmpty()) {
            contacts = findContacts(userName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME, false)
        }

        // search by contained name parts
        if (contacts.isEmpty()) {
            contacts.addAll(findContacts(userName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME, true))
            contacts.addAll(findContacts(userName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME, true))
        }

        if (contacts.isEmpty()) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.contact_not_found, userName))
            return
        }

        if (contacts.size() > 1) {
            selectContact(contacts)
        } else {
            val contactId: Int = contacts.get(0).first
            openContact(contactId)
        }
    }

    private Unit selectContact(final List<Pair<Integer, String>> contacts) {
        final SimpleDialog.ItemSelectModel<Pair<Integer, String>> model = SimpleDialog.ItemSelectModel<>()
        model
            .setItems(contacts)
            .setDisplayMapper((i) -> TextParam.text(i.second))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)
        SimpleDialog.of(activity)
                .setTitle(R.string.contact_multiple_matches)
                .selectSingle(model, i -> {
                    val contactId: Int = i.first
                    openContact(contactId)
                })
    }

    /** Opens a contact in Android with given id */
    private Unit openContact(final Int id) {
        val intent: Intent = Intent(Intent.ACTION_VIEW)
        val uri: Uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id))
        intent.setData(uri)
        activity.startActivity(intent)
    }

}
