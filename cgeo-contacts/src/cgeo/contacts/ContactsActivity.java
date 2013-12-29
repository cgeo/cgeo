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

        int contactId = getContactId(nickName);
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

    private int getContactId(final String searchName) {
        Cursor contactCursor = null;
        int foundId = 0;
        try {
            contactCursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            int idColumn = contactCursor.getColumnIndex(BaseColumns._ID);
            while (contactCursor.moveToNext()) {
                int contactId = contactCursor.getInt(idColumn);
                String where = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                String[] params = new String[] { String.valueOf(contactId), ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE };
                Cursor nicknameCursor = null;
                try {
                    nicknameCursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, where, params, null);
                    while (nicknameCursor.moveToNext()) {
                        String nicknameName = nicknameCursor.getString(nicknameCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));
                        if (StringUtils.equalsIgnoreCase(nicknameName, searchName)) {
                            foundId = contactId;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    if (nicknameCursor != null) {
                        nicknameCursor.close();
                    }
                }
                if (foundId != 0) {
                    break;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (contactCursor != null) {
                contactCursor.close();
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