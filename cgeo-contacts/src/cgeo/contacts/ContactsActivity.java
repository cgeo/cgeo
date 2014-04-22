package cgeo.contacts;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

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

        // search by nickname, exact
        List<Pair<Integer, String>> contacts = getContacts(nickName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME, false);

        // search by display name, exact
        if (contacts.isEmpty()) {
            contacts = getContacts(nickName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME, false);
        }

        // search by contained name parts
        if (contacts.isEmpty()) {
            contacts.addAll(getContacts(nickName, ContactsContract.Data.CONTENT_URI, ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME, true));
            contacts.addAll(getContacts(nickName, ContactsContract.Contacts.CONTENT_URI, BaseColumns._ID, ContactsContract.Contacts.DISPLAY_NAME, true));
        }

        if (contacts.isEmpty()) {
            showToast(getString(R.string.contact_not_found, nickName));
            finish();
            return;
        }

        if (contacts.size() > 1) {
            selectContact(contacts);
        }
        else {
            int contactId = contacts.get(0).first.intValue();
            openContactAndFinish(contactId);
        }
    }

    private void selectContact(final List<Pair<Integer, String>> contacts) {
        List<String> list = new ArrayList<String>();
        for (Pair<Integer, String> p : contacts) {
            list.add(p.second);
        }
        final CharSequence[] items = list.toArray(new CharSequence[list.size()]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.multiple_matches)
                .setItems(items, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        int contactId = contacts.get(which).first.intValue();
                        dialog.dismiss();
                        openContactAndFinish(contactId);
                    }
                })
                .create().show();
    }

    private void openContactAndFinish(int id) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(id));
        intent.setData(uri);
        startActivity(intent);
        finish();
    }

    private List<Pair<Integer, String>> getContacts(final @NonNull String searchName, Uri uri, final @NonNull String idColumnName, final @NonNull String selectionColumnName, boolean like) {
        final String[] projection = new String[] { idColumnName, selectionColumnName };
        final String selection = selectionColumnName + (like ? " LIKE" : " =") + " ? COLLATE NOCASE";
        final String[] selectionArgs = new String[] { like ? "%" + searchName + "%" : searchName };
        Cursor cursor = null;

        List<Pair<Integer, String>> result = new ArrayList<Pair<Integer, String>>();
        try {
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null);
            while (cursor != null && cursor.moveToNext()) {
                int foundId = cursor.getInt(0);
                String foundName = cursor.getString(1);
                result.add(new Pair<Integer, String>(Integer.valueOf(foundId), foundName));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "ContactsActivity.getContactId", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
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
                return StringUtils.EMPTY;
            }
            return URLDecoder.decode(param, CharEncoding.UTF_8).trim();
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "ContactsActivity.getParameter", e);
        }
        return StringUtils.EMPTY;
    }

}