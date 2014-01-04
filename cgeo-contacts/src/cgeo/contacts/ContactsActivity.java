package cgeo.contacts;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public final class ContactsActivity extends Activity {
    static final String LOG_TAG = "cgeo.contacts";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        final String nickName = getParameter(uri, IContacts.PARAM_NAME);
        if (StringUtils.isEmpty(nickName)) {
            finish();
            return;
        }

        // search by nickname
        int contactId = getContactId(nickName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME);

        // search by display name
        if (contactId == 0) {
            contactId = getContactId(nickName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME);
        }

        if (contactId == 0) {
            showToast(getString(R.string.contact_not_found, nickName));
            finish();
            return;
        }

        openContact(contactId);
        finish();
    }

    private void openContact(int id) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
        intent.setData(uri);
        startActivity(intent);
    }

    private int getContactId(final String searchName, Uri uri, final String idColumnName, final String selectionColumnName) {
        int foundId = 0;
        final String[] projection = new String[] { idColumnName };
        final String selection = selectionColumnName + " = ? COLLATE NOCASE";
        final String[] selectionArgs = new String[] { searchName };
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToNext()) {
                foundId = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "ContactsActivity.getContactId", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return foundId;
    }

    public final void showToast(final String text) {
        final Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);

        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
        toast.show();
    }

    private static String getParameter(final Uri uri, final String paramKey) {
        try {
            final String param = uri.getQueryParameter(paramKey);
            if (param == null) {
                return "";
            }
            return URLDecoder.decode(param, CharEncoding.UTF_8).trim();
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "ContactsActivity.getParameter", e);
        }
        return "";
    }

}