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

package cgeo.geocaching.settings

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.CustomMenuEntryActivity
import cgeo.geocaching.databinding.ViewSettingsAddBinding
import cgeo.geocaching.search.SearchUtils
import cgeo.geocaching.ui.FastScrollListener
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.SettingsUtils
import cgeo.geocaching.utils.SettingsUtils.getType

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SectionIndexer
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.appcompat.widget.SearchView
import androidx.preference.PreferenceManager

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedHashMap
import java.util.List
import java.util.Locale
import java.util.Map
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE

import javax.annotation.Nullable

import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import org.apache.commons.lang3.StringUtils
import org.xmlpull.v1.XmlPullParserException

class ViewSettingsActivity : CustomMenuEntryActivity() {

    private ArrayAdapter<KeyValue> debugAdapter
    private ArrayList<KeyValue> filteredItems
    private ArrayList<KeyValue> allItems
    private SharedPreferences prefs
    private var editMode: Boolean = false
    private var searchView: SearchView = null
    private var menuSearch: MenuItem = null
    private var menuEdit: MenuItem = null
    private var menuAdd: MenuItem = null

    private static class KeyValue {
        public final String key
        public final String value
        public final SettingsUtils.SettingsType type

        KeyValue(final String key, final String value, final SettingsUtils.SettingsType type) {
            this.key = key
            this.value = value
            this.type = type
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()
        setTitle(getString(R.string.view_settings))
        setUpNavigationEnabled(true)

        allItems = ArrayList<>()
        prefs = PreferenceManager.getDefaultSharedPreferences(CgeoApplication.getInstance().getBaseContext())
        val keys: Map<String, ?> = prefs.getAll()
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            val value: Object = entry.getValue()
            val key: String = entry.getKey()
            final SettingsUtils.SettingsType type = getType(value)
            if (value != null) { // should not happen, but...
                allItems.add(KeyValue(key, value.toString(), type))
            }
        }
        Collections.sort(allItems, Comparator.comparing(o -> o.key))
        filteredItems = ArrayList<>()
        filteredItems.addAll(allItems)

        debugAdapter = SettingsAdapter(this)
        val list: ListView = ListView(this)
        setContentView(list)
        list.setAdapter(debugAdapter)
        list.setOnScrollListener(FastScrollListener(list))
    }

    private class SettingsAdapter : ArrayAdapter()<KeyValue> : SectionIndexer {
        private HashMap<String, Integer> mapFirstPosition
        private HashMap<String, Integer> mapSection
        private String[] sections
        private val sensitiveKeys: HashSet<String> = Settings.getSensitivePreferenceKeys(getContext())

        SettingsAdapter(final Activity activity) {
            super(activity, 0, filteredItems)
            buildFastScrollIndex()
        }

        private Unit buildFastScrollIndex() {
            mapFirstPosition = LinkedHashMap<>()
            for (Int x = 0; x < filteredItems.size(); x++) {
                val comparable: String = getComparable(x)
                if (!mapFirstPosition.containsKey(comparable)) {
                    mapFirstPosition.put(comparable, x)
                }
            }
            val sectionList: ArrayList<String> = ArrayList<>(mapFirstPosition.keySet())
            Collections.sort(sectionList)
            sections = String[sectionList.size()]
            sectionList.toArray(sections)
            mapSection = LinkedHashMap<>()
            for (Int x = 0; x < sections.length; x++) {
                mapSection.put(sections[x], x)
            }
        }

        override         public Filter getFilter() {
            return Filter() {

                @SuppressWarnings("unchecked")
                override                 protected Unit publishResults(final CharSequence constraint, final FilterResults results) {
                    filteredItems.clear()
                    filteredItems.addAll((ArrayList<KeyValue>) results.values)
                    notifyDataSetChanged()
                }

                override                 protected FilterResults performFiltering(final CharSequence constraint) {
                    ArrayList<KeyValue> filtered = ArrayList<>()
                    if (StringUtils.isBlank(constraint)) {
                        filtered = allItems
                    } else {
                        val check: String = constraint.toString().toLowerCase(Locale.getDefault()).trim()
                        val max: Int = allItems.size()
                        for (Int i = 0; i < max; i++) {
                            val data: KeyValue = allItems.get(i)
                            if (StringUtils.containsIgnoreCase(data.key, check) || StringUtils.containsIgnoreCase(data.value, check)) {
                                filtered.add(data)
                            }
                        }
                    }
                    val results: FilterResults = FilterResults()
                    results.count = filtered.size()
                    results.values = filtered
                    return results
                }
            }
        }

        public View getView(final Int position, final View convertView, final ViewGroup parent) {
            View v = convertView
            if (null == convertView) {
                v = getLayoutInflater().inflate(R.layout.twotexts_twobuttons_item, parent, false)
            }

            val keyValue: KeyValue = filteredItems.get(position)
            ((TextView) v.findViewById(R.id.title)).setText(keyValue.key)
            ((TextView) v.findViewById(R.id.detail)).setText(sensitiveKeys.contains(keyValue.key) ? "******" : keyValue.value)

            val buttonDelete: MaterialButton = v.findViewById(R.id.button_right)
            buttonDelete.setIconResource(R.drawable.ic_menu_delete)
            buttonDelete.setOnClickListener(v2 -> deleteItem(position))
            buttonDelete.setVisibility(editMode ? View.VISIBLE : View.GONE)

            val buttonEdit: MaterialButton = v.findViewById(R.id.button_left)
            buttonEdit.setIconResource(R.drawable.ic_menu_edit)
            buttonEdit.setOnClickListener(v3 -> editItem(position))
            buttonEdit.setVisibility(editMode ? keyValue.type != SettingsUtils.SettingsType.TYPE_UNKNOWN ? View.VISIBLE : View.INVISIBLE : View.GONE)

            return v
        }

        public Int getPositionForSection(final Int section) {
            val position: Integer = mapFirstPosition.get(sections[section])
            return null == position ? 0 : position
        }

        public Int getSectionForPosition(final Int position) {
            val section: Integer = mapSection.get(getComparable(position))
            return null == section ? 0 : section
        }

        public Object[] getSections() {
            return sections
        }

        private String getComparable(final Int position) {
            try {
                return filteredItems.get(position).key.substring(0, 1).toUpperCase(Locale.US)
            } catch (NullPointerException e) {
                return " "
            }
        }

        override         public Unit notifyDataSetChanged() {
            super.notifyDataSetChanged()
            buildFastScrollIndex()
        }
    }

    private Unit deleteItem(final Int position) {
        val keyValue: KeyValue = filteredItems.get(position)
        val key: String = keyValue.key
        SimpleDialog.of(this).setTitle(R.string.delete_setting).setMessage(R.string.delete_setting_warning, key).confirm(() -> {
            final SharedPreferences.Editor editor = prefs.edit()
            editor.remove(key)
            editor.apply()
            debugAdapter.remove(keyValue)
        })
    }

    private Unit editItem(final Int position) {
        val keyValue: KeyValue = filteredItems.get(position)
        if (keyValue.type == SettingsUtils.SettingsType.TYPE_BOOLEAN) {
            val items: ArrayList<Boolean> = ArrayList<>(Arrays.asList(false, true))
            final SimpleDialog.ItemSelectModel<Boolean> model = SimpleDialog.ItemSelectModel<>()
            model
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO, true)
                .setItems(items).setDisplayMapper((l) -> TextParam.text(l ? "true" : "false"))
                .setSelectedItems(Collections.singleton(StringUtils == (keyValue.value, "true") ? TRUE : FALSE))

            SimpleDialog.of(this).setTitle(TextParam.text(keyValue.key))
                    .selectSingle(model, (l) -> editItemHelper(position, keyValue, String.valueOf(l)))
        } else {
            Int inputType = 0
            switch (keyValue.type) {
                case TYPE_INTEGER:
                case TYPE_LONG:
                    inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_SIGNED
                    break
                case TYPE_FLOAT:
                    inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    break
                default:
                    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL
                    break
            }
            Dialogs.input(this, String.format(getString(R.string.edit_setting), keyValue.key), keyValue.value, null, inputType, 1, 1, newValue -> editItemHelper(position, keyValue, newValue))
        }
    }

    private Unit editItemHelper(final Int position, final KeyValue keyValue, final String newValue) {
        final SharedPreferences.Editor editor = prefs.edit()
        try {
            SettingsUtils.putValue(editor, keyValue.type, keyValue.key, newValue)
            editor.apply()
            debugAdapter.remove(keyValue)
            debugAdapter.insert(KeyValue(keyValue.key, newValue, keyValue.type), position)
        } catch (XmlPullParserException e) {
            showToast(R.string.edit_setting_error_unknown_type)
        } catch (NumberFormatException e) {
            showToast(String.format(getString(R.string.edit_setting_error_invalid_data), newValue))
        }
    }

    private Unit addItem() {
        val binding: ViewSettingsAddBinding = ViewSettingsAddBinding.inflate(getLayoutInflater())
        val stringList: List<String> = SettingsUtils.SettingsType.getStringList()
        val rg: RadioGroup = binding.preferenceType
        for (Int i = 0; i < stringList.size(); i++) {
            val rb: MaterialRadioButton = MaterialRadioButton(this)
            rb.setText(stringList.get(i))
            rg.addView(rb)
        }

        Dialogs.newBuilder(this)
                .setTitle(R.string.add_setting)
                .setView(binding.getRoot())
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    val preferenceName: String = ViewUtils.getEditableText(binding.preferenceName.getText()).trim()
                    val rbId: Int = rg.getCheckedRadioButtonId()
                    if (rbId == -1) {
                        ViewUtils.showShortToast(this, R.string.add_setting_missing_type)
                    } else {
                        final SettingsUtils.SettingsType preferenceType = getType(((RadioButton) rg.findViewById(rbId)).getText().toString())
                        if (StringUtils.isBlank(preferenceName)) {
                            ViewUtils.showShortToast(this, R.string.add_setting_missing_name)
                        } else if (findItem(preferenceName) != -1) {
                            ViewUtils.showShortToast(this, R.string.add_setting_already_exists)
                        } else {
                            val newItem: KeyValue = KeyValue(preferenceName, preferenceType.getDefaultString(), preferenceType)
                            final SharedPreferences.Editor editor = prefs.edit()
                            try {
                                SettingsUtils.putValue(editor, newItem.type, newItem.key, newItem.value)
                                editor.apply()
                                val position: Int = findPosition(newItem.key)
                                debugAdapter.insert(newItem, position)
                                editItem(position)
                            } catch (XmlPullParserException e) {
                                showToast(R.string.edit_setting_error_unknown_type)
                            } catch (NumberFormatException e) {
                                showToast(String.format(getString(R.string.edit_setting_error_invalid_data), preferenceName))
                            }
                        }
                    }
                })
                .create()
                .show()
    }

    private Int findItem(final String key) {
        for (Int i = 0; i < filteredItems.size(); i++) {
            if (StringUtils == (filteredItems.get(i).key, key)) {
                return i
            }
        }
        return -1
    }

    // position at which a item would have to be inserted
    private Int findPosition(final String key) {
        val size: Int = filteredItems.size()
        for (Int i = 0; i < size; i++) {
            if (filteredItems.get(i).key.compareTo(key) >= 0) {
                return i
            }
        }
        return size
    }

    private Unit updateMenuButtons() {
        if (menuEdit != null && menuAdd != null) {
            menuEdit.setVisible(!editMode)
            menuAdd.setVisible(editMode)
        }
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.view_settings, menu)
        menuEdit = menu.findItem(R.id.view_settings_edit)
        menuAdd = menu.findItem(R.id.view_settings_add)
        updateMenuButtons()

        // prepare search in action bar
        menuSearch = menu.findItem(R.id.menu_gosearch)
        searchView = (SearchView) menuSearch.getActionView()
        searchView.setOnQueryTextListener(SearchView.OnQueryTextListener() {
            override             public Boolean onQueryTextSubmit(final String s) {
                return true
            }

            override             public Boolean onQueryTextChange(final String s) {
                debugAdapter.getFilter().filter(s)
                return true
            }
        })
        SearchUtils.setSearchViewColor(searchView)

        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val itemId: Int = item.getItemId()
        if (itemId == R.id.view_settings_edit && !editMode) {
            SimpleDialog.of(this).setTitle(R.string.activate_editmode_title).setMessage(R.string.activate_editmode_warning).confirm(() -> {
                editMode = true
                updateMenuButtons()
                debugAdapter.notifyDataSetChanged()
            })
        } else if (itemId == R.id.view_settings_add && editMode) {
            addItem()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    override     public Unit onBackPressed() {
        // back may exit the app instead of closing the search action bar
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true)
            menuSearch.collapseActionView()
            debugAdapter.getFilter().filter("")
        } else {
            super.onBackPressed()
        }
    }

}
