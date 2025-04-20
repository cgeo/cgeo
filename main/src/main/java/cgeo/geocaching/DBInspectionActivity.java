package cgeo.geocaching;

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
import net.movingbits.dbinspection.DBInspectionBaseActivity;
import org.apache.commons.lang3.StringUtils;

public class DBInspectionActivity extends DBInspectionBaseActivity implements AdapterView.OnItemSelectedListener {

    private DbinspectionActivityBinding binding;

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        binding = DbinspectionActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.view_database);
        prepareBlankTable(binding.tableData.getId());
        titleSelectTable = getString(R.string.dbi_select_title);
        database = DataStore.getDatabase(true);
        super.onCreate(savedInstanceState);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // set dimensions
        pxMargin = ViewUtils.dpToPixel(10);
        pxCharWidth = ViewUtils.dpToPixel(10);
        pxHeight = ViewUtils.dpToPixel(40); // will be adjusted in onItemSelected

        // initialize table selector
        final AppCompatSpinner spinner = binding.tableSpinner;
        spinner.setOnItemSelectedListener(this);
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getTablenames());
        spinner.setAdapter(spinnerAdapter);

        // prepare UI elements
        binding.tableButtonBack.setEnabled(false);
        binding.tableButtonBack.setOnClickListener(v -> pagination(offset - itemsPerPage));
        binding.tableButtonBack.setOnLongClickListener(v -> pagination(offset - 10 * itemsPerPage));
        final View.OnClickListener editSearch = v -> Dialogs.input(DBInspectionActivity.this, getString(R.string.dbi_search_title), getSearchTerm(), "", n -> {
            final String newSearchTerm = n.trim();
            final boolean[] currentSelection = getSearchColumnSelection();
            if (StringUtils.isBlank(newSearchTerm)) {
                // skip column selection for empty search term
                onSearchConfirmed(newSearchTerm, currentSelection);
            } else {
                final CharSequence[] items = getAvailableSearchColumns();
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
        binding.tableButtonForward.setOnClickListener(v -> pagination(offset + itemsPerPage));
        binding.tableButtonForward.setOnLongClickListener(v -> pagination(offset + 10 * itemsPerPage));
    }

    private void onSearchConfirmed(final String newSearchTerm, final boolean[] newSearchColumnSelection) {
        if (!StringUtils.equals(newSearchTerm, getSearchTerm()) || (!Arrays.equals(getSearchColumnSelection(), newSearchColumnSelection))) {
            setSearchTerm(newSearchTerm, newSearchColumnSelection);
            updateTableData(null);
        }
        binding.searchTerm.setText(newSearchTerm);
    }

    private boolean pagination(final int newOffset) {
        offset = Math.max(0, newOffset);
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
        pxHeight = Math.max(ViewUtils.dpToPixel(40), binding.tableData.getHeight() / (itemsPerPage + 1));
        updateTableData(item);
    }

    @Override
    protected boolean updateTableData(@Nullable final String resetToTable) {
        final boolean moreDataAvailable = super.updateTableData(resetToTable);
        binding.tableButtonBack.setEnabled(offset > 0);
        binding.tableButtonSearch.setEnabled(true);
        binding.tableButtonForward.setEnabled(moreDataAvailable);
        binding.searchTerm.setText(getSearchTerm());
        return moreDataAvailable;
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {
        // do nothing
    }

    @Override
    protected boolean onColumHeaderLongClickListener(final ColumnInfo columnInfo) {
        SimpleDialog
                .of(this)
                .setTitle(TextParam.id(R.string.dbi_columnproperties_title))
                .setMessage(TextParam.text(String.format(getString(R.string.dbi_columnproperties_message), columnInfo.name, columnInfo.type, columnInfo.storageClass)))
                .show();
        return true;
    }

    @Override
    protected boolean onFieldLongClickListener(final ColumnInfo columnInfo, final int row, final int inputType, final String currentValue, final boolean isPartOfPrimaryKey) {
        if (isPartOfPrimaryKey) {
            ViewUtils.showShortToast(null, String.format(getString(R.string.dbi_edit_error_pkfield), columnInfo.name));
            return true;
        } else {
            Dialogs.input(this, String.format(getString(R.string.dbi_edit_title), columnInfo.name, row), currentValue, null, inputType, 1, 1, newValue -> {
                if (persistData(row, columnInfo.name, newValue)) {
                    ViewUtils.showShortToast(this, R.string.dbi_edit_ok);
                } else {
                    ViewUtils.showToast(this, R.string.dbi_edit_error);
                }
            });
        }
        return true;
    }

    @Override
    protected void setSearchTerm(final String newSearchTerm) {
        super.setSearchTerm(newSearchTerm);
        binding.searchTerm.setText(newSearchTerm);
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
        if (database != null) {
            DataStore.releaseDatabase(true);
        }
        super.onDestroy();
    }

}
