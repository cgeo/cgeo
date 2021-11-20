package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.databinding.ViewSettingsAddBinding;
import cgeo.geocaching.ui.FastScrollListener;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ApplicationSettings;
import cgeo.geocaching.utils.SettingsUtils;
import static cgeo.geocaching.utils.SettingsUtils.getType;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParserException;

public class ViewSettingsActivity extends AbstractActivity {

    private ArrayAdapter<KeyValue> debugAdapter;
    private ArrayList<KeyValue> filteredItems;
    private ArrayList<KeyValue> allItems;
    private SharedPreferences prefs;
    private boolean editMode = false;
    private boolean searchMode = false;
    private TextWatcher searchWatcher = null;

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
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setCustomView(R.layout.view_settings_toolbar);
            ab.setDisplayShowCustomEnabled(true);
            final View v = ab.getCustomView();
            ((TextView) v.findViewById(R.id.title)).setText(R.string.view_settings);
        }

        allItems = new ArrayList<>();
        prefs = getSharedPreferences(ApplicationSettings.getPreferencesName(), MODE_PRIVATE);
        final Map<String, ?> keys = prefs.getAll();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            final Object value = entry.getValue();
            final String key = entry.getKey();
            final SettingsUtils.SettingsType type = getType(value);
            if (value != null) { // should not happen, but...
                allItems.add(new KeyValue(key, value.toString(), type));
            }
        }
        Collections.sort(allItems, (o1, o2) -> o1.key.compareTo(o2.key));
        filteredItems = new ArrayList<>();
        filteredItems.addAll(allItems);

        debugAdapter = new SettingsAdapter(this);
        final ListView list = new ListView(this);
        setContentView(list);
        list.setAdapter(debugAdapter);
        list.setOnScrollListener(new FastScrollListener(list));
    }

    private class SettingsAdapter extends ArrayAdapter<KeyValue> implements SectionIndexer {
        private HashMap<String, Integer> mapFirstPosition;
        private HashMap<String, Integer> mapSection;
        private String[] sections;
        private final HashSet<String> sensitiveKeys = Settings.getSensitivePreferenceKeys(getContext());

        SettingsAdapter(final Activity activity) {
            super(activity, 0, filteredItems);
            buildFastScrollIndex();
        }

        private void buildFastScrollIndex() {
            mapFirstPosition = new LinkedHashMap<>();
            for (int x = 0; x < filteredItems.size(); x++) {
                final String comparable = getComparable(x);
                if (!mapFirstPosition.containsKey(comparable)) {
                    mapFirstPosition.put(comparable, x);
                }
            }
            final ArrayList<String> sectionList = new ArrayList<>(mapFirstPosition.keySet());
            Collections.sort(sectionList);
            sections = new String[sectionList.size()];
            sectionList.toArray(sections);
            mapSection = new LinkedHashMap<>();
            for (int x = 0; x < sections.length; x++) {
                mapSection.put(sections[x], x);
            }
        }

        @Override
        @NonNull
        public Filter getFilter() {
            return new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(final CharSequence constraint, final FilterResults results) {
                    filteredItems.clear();
                    filteredItems.addAll((ArrayList<KeyValue>) results.values);
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    ArrayList<KeyValue> filtered = new ArrayList<>();
                    if (StringUtils.isBlank(constraint)) {
                        filtered = allItems;
                    } else {
                        final String check = constraint.toString().toLowerCase().trim();
                        final int max = allItems.size();
                        for (int i = 0; i < max; i++) {
                            final KeyValue data = allItems.get(i);
                            if (StringUtils.contains(data.key, check) || StringUtils.contains(data.value, check)) {
                                filtered.add(data);
                            }
                        }
                    }
                    final FilterResults results = new FilterResults();
                    results.count = filtered.size();
                    results.values = filtered;
                    return results;
                }
            };
        }

        @NonNull
        public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
            View v = convertView;
            if (null == convertView) {
                v = getLayoutInflater().inflate(R.layout.twotexts_twobuttons_item, parent, false);
            }

            final KeyValue keyValue = filteredItems.get(position);
            ((TextView) v.findViewById(R.id.title)).setText(keyValue.key);
            ((TextView) v.findViewById(R.id.detail)).setText(sensitiveKeys.contains(keyValue.key) ? "******" : keyValue.value);

            final MaterialButton buttonDelete = v.findViewById(R.id.button_right);
            buttonDelete.setIconResource(R.drawable.ic_menu_delete);
            buttonDelete.setOnClickListener(v2 -> deleteItem(position));
            buttonDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);

            final MaterialButton buttonEdit = v.findViewById(R.id.button_left);
            buttonEdit.setIconResource(R.drawable.ic_menu_edit);
            buttonEdit.setOnClickListener(v3 -> editItem(position));
            buttonEdit.setVisibility(editMode ? keyValue.type != SettingsUtils.SettingsType.TYPE_UNKNOWN ? View.VISIBLE : View.INVISIBLE : View.GONE);

            return v;
        }

        public int getPositionForSection(final int section) {
            final Integer position = mapFirstPosition.get(sections[section]);
            return null == position ? 0 : position;
        }

        public int getSectionForPosition(final int position) {
            final Integer section = mapSection.get(getComparable(position));
            return null == section ? 0 : section;
        }

        public Object[] getSections() {
            return sections;
        }

        @NonNull
        private String getComparable(final int position) {
            try {
                return filteredItems.get(position).key.substring(0, 1).toUpperCase(Locale.US);
            } catch (NullPointerException e) {
                return " ";
            }
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            buildFastScrollIndex();
        }
    }

    private void deleteItem(final int position) {
        final KeyValue keyValue = filteredItems.get(position);
        final String key = keyValue.key;
        SimpleDialog.of(this).setTitle(R.string.delete_setting).setMessage(R.string.delete_setting_warning, key).confirm((dialog, which) -> {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            editor.apply();
            debugAdapter.remove(keyValue);
        });
    }

    private void editItem(final int position) {
        final KeyValue keyValue = filteredItems.get(position);
        final String key = keyValue.key;
        final SettingsUtils.SettingsType type = keyValue.type;

        int inputType = 0;
        switch (type) {
            case TYPE_INTEGER:
            case TYPE_LONG:
                inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
                break;
            case TYPE_FLOAT:
                inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                break;
            default:
                inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
                break;
        }
        Dialogs.input(this, String.format(getString(R.string.edit_setting), key), keyValue.value, null, inputType, 1, 1, newValue -> {
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
        });
    }

    private void addItem() {
        final ViewSettingsAddBinding binding = ViewSettingsAddBinding.inflate(getLayoutInflater());
        final List<String> stringList = SettingsUtils.SettingsType.getStringList();
        final RadioGroup rg = binding.preferenceType;
        for (int i = 0; i < stringList.size(); i++) {
            final MaterialRadioButton rb = new MaterialRadioButton(this);
            rb.setText(stringList.get(i));
            rg.addView(rb);
        }

        Dialogs.newBuilder(this)
            .setTitle(R.string.add_setting)
            .setView(binding.getRoot())
            .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
            .setPositiveButton(android.R.string.ok, (d, which) -> {
                final String preferenceName = binding.preferenceName.getText().toString().trim();
                final int rbId = rg.getCheckedRadioButtonId();
                if (rbId == -1) {
                    Toast.makeText(this, R.string.add_setting_missing_type, Toast.LENGTH_SHORT).show();
                } else {
                    final SettingsUtils.SettingsType preferenceType = getType(((RadioButton) rg.findViewById(rbId)).getText().toString());
                    if (StringUtils.isBlank(preferenceName)) {
                        Toast.makeText(this, R.string.add_setting_missing_name, Toast.LENGTH_SHORT).show();
                    } else if (findItem(preferenceName) != -1) {
                        Toast.makeText(this, R.string.add_setting_already_exists, Toast.LENGTH_SHORT).show();
                    } else {
                        final KeyValue newItem = new KeyValue(preferenceName, preferenceType.getDefaultString(), preferenceType);
                        final SharedPreferences.Editor editor = prefs.edit();
                        try {
                            SettingsUtils.putValue(editor, newItem.type, newItem.key, newItem.value);
                            editor.apply();
                            final int position = findPosition(newItem.key);
                            debugAdapter.insert(newItem, position);
                            editItem(position);
                        } catch (XmlPullParserException e) {
                            showToast(R.string.edit_setting_error_unknown_type);
                        } catch (NumberFormatException e) {
                            showToast(String.format(getString(R.string.edit_setting_error_invalid_data), preferenceName));
                        }
                    }
                }
            })
            .create()
            .show();
    }

    private int findItem(final String key) {
        for (int i = 0; i < filteredItems.size(); i++) {
            if (StringUtils.equals(filteredItems.get(i).key, key)) {
                return i;
            }
        }
        return -1;
    }

    // position at which a new item would have to be inserted
    private int findPosition(final String key) {
        final int size = filteredItems.size();
        for (int i = 0; i < size; i++) {
            if (filteredItems.get(i).key.compareTo(key) >= 0) {
                return i;
            }
        }
        return size;
    }

    private void toggleSearch() {
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            searchMode = !searchMode;
            invalidateOptionsMenuCompatible();

            final View v = ab.getCustomView();
            final EditText et = v.findViewById(R.id.filter);
            if (searchMode) {
                et.setVisibility(View.VISIBLE);
                et.requestFocus();
                searchWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                        // nothing to do
                    }
                    @Override
                    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                        debugAdapter.getFilter().filter(s);
                    }
                    @Override
                    public void afterTextChanged(final Editable s) {
                        // nothing to do
                    }
                };
                et.addTextChangedListener(searchWatcher);
            } else {
                et.setVisibility(View.GONE);
                et.removeTextChangedListener(searchWatcher);
                debugAdapter.getFilter().filter("");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.view_settings, menu);
        menu.findItem(R.id.view_settings_edit).setVisible(!editMode);
        menu.findItem(R.id.view_settings_add).setVisible(editMode);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        menu.findItem(R.id.menu_gosearch).setIcon(searchMode ? R.drawable.ic_menu_search_off : R.drawable.ic_menu_search);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_gosearch) {
            toggleSearch();
        } else if (itemId == R.id.view_settings_edit && !editMode) {
            SimpleDialog.of(this).setTitle(R.string.activate_editmode_title).setMessage(R.string.activate_editmode_warning).confirm((dialog, which) -> {
                editMode = true;
                invalidateOptionsMenu();
                debugAdapter.notifyDataSetChanged();
            });
        } else if (itemId == R.id.view_settings_add && editMode) {
            addItem();
        } else {
            return false;
        }
        return true;
    }
}
