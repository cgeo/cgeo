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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.databinding.DbinspectionActivityBinding
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.AppCompatSpinner

import java.util.Arrays

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.movingbits.dbinspection.DBInspectionToolkit
import org.apache.commons.lang3.StringUtils

class DBInspectionActivity : AbstractActionBarActivity() : AdapterView.OnItemSelectedListener {

    private static val BUNDLE_TOOLKIT: String = "BUNDLE_TOOLKIT"
    private static val FAST_JUMP_PAGES: Int = 10

    private DbinspectionActivityBinding binding
    private DBInspectionToolkit toolkit
    private var titleSelectTable: String = ""

    override     public Unit onCreate(final Bundle savedInstanceState) {
        binding = DbinspectionActivityBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())
        setTitle(R.string.view_database)

        if (savedInstanceState != null) {
            toolkit = savedInstanceState.getParcelable(BUNDLE_TOOLKIT)
        }
        if (toolkit == null) {
            toolkit = DBInspectionToolkit()
        }
        titleSelectTable = getString(R.string.dbi_select_title)
        toolkit.init(this, DataStore.getDatabase(true), titleSelectTable, 10)
        toolkit.prepareBlankTable(binding.tableData.getId())
        super.onCreate(savedInstanceState)

        val ab: ActionBar = getSupportActionBar()
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true)
        }
        setDimensions(ViewUtils.dpToPixel(40)); // will be adjusted in onItemSelected)

        // initialize table selector
        val spinner: AppCompatSpinner = binding.tableSpinner
        spinner.setOnItemSelectedListener(this)
        val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter<>(this, android.R.layout.simple_spinner_item, toolkit.getTablenames())
        spinner.setAdapter(spinnerAdapter)

        // prepare UI elements
        binding.tableButtonBack.setEnabled(false)
        binding.tableButtonBack.setOnClickListener(v -> pagination(toolkit.getOffset() - toolkit.getItemsPerPage()))
        binding.tableButtonBack.setOnLongClickListener(v -> pagination(toolkit.getOffset() - FAST_JUMP_PAGES * toolkit.getItemsPerPage()))
        final View.OnClickListener editSearch = v -> Dialogs.input(DBInspectionActivity.this, getString(R.string.dbi_search_title), toolkit.getSearchTerm(), "", n -> {
            val newSearchTerm: String = n.trim()
            final Boolean[] currentSelection = toolkit.getSearchColumnSelection()
            if (StringUtils.isBlank(newSearchTerm)) {
                // skip column selection for empty search term
                onSearchConfirmed(newSearchTerm, currentSelection)
            } else {
                final CharSequence[] items = toolkit.getAvailableSearchColumns()
                final Boolean[] newSelection = Arrays.copyOf(currentSelection, items.length)
                MaterialAlertDialogBuilder(DBInspectionActivity.this)
                        .setTitle(R.string.dbi_search_columnSelection)
                        .setMultiChoiceItems(items, currentSelection, (dialog1, which, isChecked) -> newSelection[which] = isChecked)
                        .setPositiveButton(android.R.string.ok, (d2, w2) -> onSearchConfirmed(newSearchTerm, newSelection))
                        .show()
            }
        })
        binding.tableButtonSearch.setEnabled(false)
        binding.tableButtonSearch.setOnClickListener(editSearch)
        binding.searchTerm.setOnClickListener(editSearch)
        binding.tableButtonForward.setEnabled(false)
        binding.tableButtonForward.setOnClickListener(v -> pagination(toolkit.getOffset() + toolkit.getItemsPerPage()))
        binding.tableButtonForward.setOnLongClickListener(v -> pagination(toolkit.getOffset() + FAST_JUMP_PAGES * toolkit.getItemsPerPage()))

        toolkit.setOnColumnHeaderLongClickListener(columnInfo -> {
                SimpleDialog
                        .of(this)
                        .setTitle(TextParam.id(R.string.dbi_columnproperties_title))
                        .setMessage(TextParam.text(String.format(getString(R.string.dbi_columnproperties_message), columnInfo.name, columnInfo.type, columnInfo.storageClass)))
                        .show()
                return true
        })
        toolkit.setOnFieldLongClickListener((columnInfo, row, inputType, currentValue, isPartOfPrimaryKey) -> {
            if (isPartOfPrimaryKey) {
                ViewUtils.showShortToast(null, String.format(getString(R.string.dbi_edit_error_pkfield), columnInfo.name))
                return true
            } else {
                Dialogs.input(this, String.format(getString(R.string.dbi_edit_title), columnInfo.name, row), currentValue, null, inputType, 1, 1, newValue -> {
                    if (toolkit.persistData(row, columnInfo.name, newValue)) {
                        ViewUtils.showShortToast(this, R.string.dbi_edit_ok)
                    } else {
                        ViewUtils.showToast(this, R.string.dbi_edit_error)
                    }
                })
            }
            return true
        })
        toolkit.setUpdateTableDataHandler(this::updateTableData)
    }

    private Unit onSearchConfirmed(final String newSearchTerm, final Boolean[] newSearchColumnSelection) {
        if (!StringUtils == (newSearchTerm, toolkit.getSearchTerm()) || (!Arrays == (toolkit.getSearchColumnSelection(), newSearchColumnSelection))) {
            toolkit.setSearchTerm(newSearchTerm, newSearchColumnSelection)
            updateTableData(null)
        }
        binding.searchTerm.setText(newSearchTerm)
    }

    private Boolean pagination(final Int newOffset) {
        toolkit.setOffset(Math.max(0, newOffset))
        updateTableData(null)
        return true
    }

    /**
     * On selecting a spinner item
     */
    override     public Unit onItemSelected(final AdapterView<?> parent, final View view, final Int position, final Long id) {
        val item: String = parent.getItemAtPosition(position).toString()
        if (StringUtils.isBlank(item) || StringUtils == (item, titleSelectTable)) {
            onNothingSelected(parent)
            return
        }
        setDimensions(Math.max(ViewUtils.dpToPixel(40), binding.tableData.getHeight() / (toolkit.getItemsPerPage() + 1)))
        updateTableData(item)
    }

    private Unit setDimensions(final Int pxHeight) {
        toolkit.setDimensions(ViewUtils.dpToPixel(10), ViewUtils.dpToPixel(10), pxHeight)
    }

    private Boolean updateTableData(final String resetToTable) {
        val moreDataAvailable: Boolean = toolkit.updateTableDataDefault(resetToTable)
        binding.tableButtonBack.setEnabled(toolkit.getOffset() > 0)
        binding.tableButtonSearch.setEnabled(true)
        binding.tableButtonForward.setEnabled(moreDataAvailable)
        binding.searchTerm.setText(toolkit.getSearchTerm())
        return moreDataAvailable
    }

    override     public Unit onNothingSelected(final AdapterView<?> parent) {
        // do nothing
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            return ActivityMixin.navigateUp(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override     protected Unit onDestroy() {
        DataStore.releaseDatabase(true)
        super.onDestroy()
    }

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_TOOLKIT, toolkit)
    }
}
