package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.DbinspectionActivityBinding;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatSpinner;

import java.util.Arrays;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.movingbits.dbinspection.DBInspectionToolkit;
import org.apache.commons.lang3.StringUtils;

public class DBInspectionActivity extends AbstractActionBarActivity implements AdapterView.OnItemSelectedListener {

    private static final String BUNDLE_TOOLKIT = "BUNDLE_TOOLKIT";
    private static final int FAST_JUMP_PAGES = 10;

    private DbinspectionActivityBinding binding;
    private DBInspectionToolkit toolkit;
    private String titleSelectTable = "";

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        binding = DbinspectionActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.view_database);

        if (savedInstanceState != null) {
            toolkit = savedInstanceState.getParcelable(BUNDLE_TOOLKIT);
        }
        if (toolkit == null) {
            toolkit = new DBInspectionToolkit();
        }
        titleSelectTable = getString(R.string.dbi_select_title);
        toolkit.init(this, DataStore.getDatabase(true), titleSelectTable, 10);
        toolkit.prepareBlankTable(binding.tableData.getId());
        super.onCreate(savedInstanceState);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        setDimensions(ViewUtils.dpToPixel(40)); // will be adjusted in onItemSelected);

        // initialize table selector
        final AppCompatSpinner spinner = binding.tableSpinner;
        spinner.setOnItemSelectedListener(this);
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, toolkit.getTablenames());
        spinner.setAdapter(spinnerAdapter);

        // prepare UI elements
        binding.tableButtonBack.setEnabled(false);
        binding.tableButtonBack.setOnClickListener(v -> pagination(toolkit.getOffset() - toolkit.getItemsPerPage()));
        binding.tableButtonBack.setOnLongClickListener(v -> pagination(toolkit.getOffset() - FAST_JUMP_PAGES * toolkit.getItemsPerPage()));
        final View.OnClickListener editSearch = v -> Dialogs.input(DBInspectionActivity.this, getString(R.string.dbi_search_title), toolkit.getSearchTerm(), "", n -> {
            final String newSearchTerm = n.trim();
            final boolean[] currentSelection = toolkit.getSearchColumnSelection();
            if (StringUtils.isBlank(newSearchTerm)) {
                // skip column selection for empty search term
                onSearchConfirmed(newSearchTerm, currentSelection);
            } else {
                final CharSequence[] items = toolkit.getAvailableSearchColumns();
                final boolean[] newSelection = Arrays.copyOf(currentSelection, items.length);
                new MaterialAlertDialogBuilder(DBInspectionActivity.this)
                        .setTitle(R.string.dbi_search_columnSelection)
                        .setMultiChoiceItems(items, currentSelection, (dialog1, which, isChecked) -> newSelection[which] = isChecked)
                        .setPositiveButton(android.R.string.ok, (d2, w2) -> onSearchConfirmed(newSearchTerm, newSelection))
                        .show();
            }
        });
        binding.tableButtonSearch.setEnabled(false);
        binding.tableButtonSearch.setOnClickListener(editSearch);
        binding.searchTerm.setOnClickListener(editSearch);
        binding.tableButtonForward.setEnabled(false);
        binding.tableButtonForward.setOnClickListener(v -> pagination(toolkit.getOffset() + toolkit.getItemsPerPage()));
        binding.tableButtonForward.setOnLongClickListener(v -> pagination(toolkit.getOffset() + FAST_JUMP_PAGES * toolkit.getItemsPerPage()));

        toolkit.setOnColumnHeaderLongClickListener(columnInfo -> {
                SimpleDialog
                        .of(this)
                        .setTitle(TextParam.id(R.string.dbi_columnproperties_title))
                        .setMessage(TextParam.text(String.format(getString(R.string.dbi_columnproperties_message), columnInfo.name, columnInfo.type, columnInfo.storageClass)))
                        .show();
                return true;
        });
        toolkit.setOnFieldLongClickListener((columnInfo, row, inputType, currentValue, isPartOfPrimaryKey) -> {
            if (isPartOfPrimaryKey) {
                ViewUtils.showShortToast(null, String.format(getString(R.string.dbi_edit_error_pkfield), columnInfo.name));
                return true;
            } else {
                Dialogs.input(this, String.format(getString(R.string.dbi_edit_title), columnInfo.name, row), currentValue, null, inputType, 1, 1, newValue -> {
                    if (toolkit.persistData(row, columnInfo.name, newValue)) {
                        ViewUtils.showShortToast(this, R.string.dbi_edit_ok);
                    } else {
                        ViewUtils.showToast(this, R.string.dbi_edit_error);
                    }
                });
            }
            return true;
        });
        toolkit.setUpdateTableDataHandler(this::updateTableData);
    }

    private void onSearchConfirmed(final String newSearchTerm, final boolean[] newSearchColumnSelection) {
        if (!StringUtils.equals(newSearchTerm, toolkit.getSearchTerm()) || (!Arrays.equals(toolkit.getSearchColumnSelection(), newSearchColumnSelection))) {
            toolkit.setSearchTerm(newSearchTerm, newSearchColumnSelection);
            updateTableData(null);
        }
        binding.searchTerm.setText(newSearchTerm);
    }

    private boolean pagination(final int newOffset) {
        toolkit.setOffset(Math.max(0, newOffset));
        updateTableData(null);
        return true;
    }

    /**
     * On selecting a spinner item
     */
    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
        final String item = parent.getItemAtPosition(position).toString();
        if (StringUtils.isBlank(item) || StringUtils.equals(item, titleSelectTable)) {
            onNothingSelected(parent);
            return;
        }
        setDimensions(Math.max(ViewUtils.dpToPixel(40), binding.tableData.getHeight() / (toolkit.getItemsPerPage() + 1)));
        updateTableData(item);
    }

    private void setDimensions(final int pxHeight) {
        toolkit.setDimensions(ViewUtils.dpToPixel(10), ViewUtils.dpToPixel(10), pxHeight);
    }

    private boolean updateTableData(@Nullable final String resetToTable) {
        final boolean moreDataAvailable = toolkit.updateTableDataDefault(resetToTable);
        binding.tableButtonBack.setEnabled(toolkit.getOffset() > 0);
        binding.tableButtonSearch.setEnabled(true);
        binding.tableButtonForward.setEnabled(moreDataAvailable);
        binding.searchTerm.setText(toolkit.getSearchTerm());
        return moreDataAvailable;
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {
        // do nothing
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            return ActivityMixin.navigateUp(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        DataStore.releaseDatabase(true);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_TOOLKIT, toolkit);
    }
}
