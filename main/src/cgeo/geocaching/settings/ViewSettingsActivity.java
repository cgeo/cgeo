package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.SettingsUtils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import org.xmlpull.v1.XmlPullParserException;

public class ViewSettingsActivity extends AbstractActivity {

    private ArrayAdapter<KeyValue> debugAdapter;
    private ArrayList<KeyValue> items;
    private SharedPreferences prefs;
    private boolean editMode = false;

    private static class KeyValue {
        public final String key;
        public final String value;
        public final SettingsUtils.SettingsType type;

        KeyValue(final String key, final String value, final SettingsUtils.SettingsType type) {
            this.key = key;
            this.value = value;
            this.type = type;
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(getString(R.string.view_settings));

        items = new ArrayList<>();
        prefs = getSharedPreferences(ApplicationSettings.getPreferencesName(), MODE_PRIVATE);
        final Map<String, ?> keys = prefs.getAll();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            final Object value = entry.getValue();
            final String key = entry.getKey();
            final SettingsUtils.SettingsType type = SettingsUtils.getType(value);
            items.add(new KeyValue(key, value.toString(), type));
        }
        Collections.sort(items, (o1, o2) -> o1.key.compareTo(o2.key));

        debugAdapter = new ArrayAdapter<KeyValue>(this, 0, items) {
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View v = convertView;
                if (null == convertView) {
                    v = getLayoutInflater().inflate(R.layout.view_settings_item, null, false);
                }
                final KeyValue keyValue = items.get(position);
                ((TextView) v.findViewById(R.id.settings_key)).setText(keyValue.key);
                ((TextView) v.findViewById(R.id.settings_value)).setText(keyValue.value);

                final View buttonDelete = v.findViewById(R.id.settings_delete);
                buttonDelete.setOnClickListener(v2 -> deleteItem(position));
                buttonDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);

                final View buttonEdit = v.findViewById(R.id.settings_edit);
                buttonEdit.setOnClickListener(v3 -> editItem(position));
                buttonEdit.setVisibility(editMode ? keyValue.type != SettingsUtils.SettingsType.TYPE_UNKNOWN ? View.VISIBLE : View.INVISIBLE : View.GONE);

                return v;
            }
        };
        final ListView list = new ListView(this);
        setContentView(list);
        list.setAdapter(debugAdapter);
    }

    private void deleteItem(final int position) {
        final KeyValue keyValue = items.get(position);
        final String key = keyValue.key;
        Dialogs.confirm(this, R.string.delete_setting, String.format(getString(R.string.delete_setting_warning), key), (dialog, which) -> {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            editor.apply();
            debugAdapter.remove(keyValue);
        });
    }

    private void editItem(final int position) {
        final KeyValue keyValue = items.get(position);
        final String key = keyValue.key;
        final SettingsUtils.SettingsType type = keyValue.type;

        int inputType = 0;
        switch (type) {
            case TYPE_INTEGER:
            case TYPE_LONG:
                inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL;
                break;
            case TYPE_FLOAT:
                inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                break;
            default:
                inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
                break;
        }
        final EditText editText = new EditText(this);
        editText.setInputType(inputType);
        editText.setText(keyValue.value);

        new AlertDialog.Builder(this)
            .setTitle(String.format(getString(R.string.edit_setting), key))
            .setView(editText)
            .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                final String newValue = editText.getText().toString();
                final SharedPreferences.Editor editor = prefs.edit();
                try {
                    SettingsUtils.putValue(editor, type, key, newValue);
                    editor.apply();
                    debugAdapter.remove(keyValue);
                    debugAdapter.insert(new KeyValue(key, newValue, type), position);
                } catch (XmlPullParserException e) {
                    showToast(R.string.edit_setting_error_unknown_type);
                } catch (NumberFormatException e) {
                    showToast(String.format(getString(R.string.edit_setting_error_invalid_data), newValue));
                }
            })
            .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> { })
            .show()
        ;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.view_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.view_settings_edit) {
            if (editMode) {
                editMode = false;
                debugAdapter.notifyDataSetChanged();
            } else {
                Dialogs.confirm(this, R.string.activate_editmode_title, R.string.activate_editmode_warning, (dialog, which) -> {
                    editMode = true;
                    debugAdapter.notifyDataSetChanged();
                });
            }
            return true;
        }
        return false;
    }

}
