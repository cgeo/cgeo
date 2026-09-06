package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.DbinspectionActivityBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.movingbits.grid.CellRef;
import com.movingbits.grid.ColumnType;
import com.movingbits.grid.DatabaseGrid;
import com.movingbits.grid.Grid;
import com.movingbits.grid.GridColumn;
import com.movingbits.grid.GridView;
import com.movingbits.grid.SortCriterion;
import com.movingbits.grid.SortDirection;

public class DBInspectionActivity extends AbstractActionBarActivity  {

    private static final int ROWS_PER_PAGE = 10;
    private static final String CONFIG_PREFIX = "dbinspection";

    private GridView gridView;
    /** Set while the database demo is running; {@code null} otherwise. */
    private DatabaseGrid databaseGrid;
    private ColorStateList buttoncolorDefault;
    private int accentColor;
    private Toast hint;
    private DbinspectionActivityBinding binding;

    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DbinspectionActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.view_database);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        databaseGrid = new DatabaseGrid()
                .setDatabase(DataStore.getDatabase(true))
                .onTablesLoaded(tables -> binding.activityContent.post(() -> showTableSelection(tables)))
                .onConfigurationChanged(json -> {
                    Settings.putStringDirect(databaseConfigKey(), json);
                    highlightButton(binding.buttonConfigColumns, grid().hasHiddenColumns());
                })
                .onSearchChanged(searchConfig -> highlightButton(binding.buttonSearch, !searchConfig.isEmpty()))
                .rowsPerPage(ROWS_PER_PAGE)
                .alternatingRowColors(true)
                .adjustableFixedBoundary(true)
                .onCellLongClick(this::showCell)
                .onSortChanged(order -> showHint(addSortIndicator(order)));
        gridView = new GridView(this);
        gridView.setGrid(databaseGrid);
        binding.gridContainer.addView(gridView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        configureUI();
    }

    // ------------------------------------------------------ table selection

    /** table selection for DatabaseGrid */
    private void showTableSelection(final List<String> tables) {
        if (tables.isEmpty()) {
            showHint(LocalizationUtils.getString(R.string.dbi_no_tables));
            return;
        }
        final String[] names = tables.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dbi_select_table)
                .setItems(names, (dialog, which) -> selectTable(names[which]))
                .show();
    }

    /** select table, initialize configuration with table-specific config key */
    private void selectTable(final String table) {
        if (databaseGrid == null || !databaseGrid.setCurrentTable(table)) {
            return;
        }
        databaseGrid.configuration(Settings.getStringDirect(databaseConfigKey(), ""));
        gridView.setCurrentPage(0);
        gridView.refresh();
        highlightButton(binding.buttonConfigColumns, grid().hasHiddenColumns());
        highlightButton(binding.buttonSearch, grid().isSearching());
        setTitle(LocalizationUtils.getString(R.string.view_database) + ": " + table);
    }

    /** each table has an individual config key */
    private String databaseConfigKey() {
        return CONFIG_PREFIX + "." + (databaseGrid == null ? "" : databaseGrid.getCurrentTable());
    }

    // ---------------------------------------------------------- cell dialogs

    /** long tap on cell opens edit/view mask */
    private void showCell(final CellRef cell, final String[] row) {
        final GridColumn column = grid().getColumn(cell.columnIndex());
        final String text = cell.dataIndex() < row.length && row[cell.dataIndex()] != null ? row[cell.dataIndex()] : "";
        if (cell.readOnly() || column.getType() == ColumnType.UNKNOWN) {
            SimpleDialog.of(this)
                    .setTitle(TextParam.text(LocalizationUtils.getString(R.string.dbi_read_only, column.getTitle(), cell.rowIndex() + 1)))
                    .setMessage(TextParam.text(text)).show();
        } else {
            final int inputType = switch (column.getType()) {
                case INTEGER -> InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
                case FLOAT -> InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                default -> InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            };
            Dialogs.input(this, LocalizationUtils.getString(R.string.dbi_edit_title, column.getTitle(), cell.rowIndex() + 1), text, null, inputType, 1, text.length() > 50 ? 20 : 1, newValue -> saveCell(column, cell.rowIndex(), newValue));
        }
    }

    private void saveCell(final GridColumn column, final int rowIndex, final String value) {
        final boolean saved = databaseGrid != null && databaseGrid.persistData(rowIndex, column.getName(), value);
        showHint(LocalizationUtils.getString(saved ? R.string.dbi_edit_ok : R.string.dbi_edit_error, column.getTitle()));
        if (saved) {
            gridView.refresh();
        }
    }

    /** get current configuration */
    private Grid grid() {
        return gridView.getGrid();
    }

    /** Add sort indicator to column title */
    private String addSortIndicator(final List<SortCriterion> order) {
        if (order.isEmpty()) {
            return LocalizationUtils.getString(R.string.dbi_sort_reset);
        }
        final StringBuilder text = new StringBuilder();
        for (SortCriterion criterion : order) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(grid().getColumn(criterion.columnIndex()).getTitle())
                    .append(criterion.direction() == SortDirection.ASCENDING ? " ▲" : " ▼");
        }
        return LocalizationUtils.getString(R.string.dbi_sorted_by, text.toString());
    }

    /** show hint (and cancel current hint, if available, to avoid stacking) */
    private void showHint(final String text) {
        if (hint != null) {
            hint.cancel();
        }
        hint = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        hint.show();
    }

    /** configure control bar at the bottom */
    private void configureUI() {
        binding.controlBar.setVisibility(View.VISIBLE);
        binding.prevPage.setOnClickListener(v -> gridView.previousPage());
        binding.nextPage.setOnClickListener(v -> gridView.nextPage());
        binding.buttonConfigColumns.setOnClickListener(v -> gridView.showColumnSettings());
        binding.buttonSearch.setOnClickListener(v -> gridView.showSearch());

        // The table selection only exists when there are tables to choose from.
        binding.buttonSelectTable.setVisibility(databaseGrid == null ? View.GONE : View.VISIBLE);
        binding.buttonSelectTable.setOnClickListener(v -> showTableSelection(databaseGrid.getTables()));

        // The ordinary color comes from the theme; highlighting uses the accent color.
        buttoncolorDefault = binding.buttonConfigColumns.getIconTint();
        accentColor = MaterialColors.getColor(binding.buttonConfigColumns, com.google.android.material.R.attr.colorSecondary, buttoncolorDefault.getDefaultColor());
        highlightButton(binding.buttonConfigColumns, grid().hasHiddenColumns());
        highlightButton(binding.buttonSearch, grid().isSearching());

        gridView.setOnPageChangedListener((page, numPages) -> {
            binding.pageNumOfNum.setText(LocalizationUtils.getString(R.string.dbi_pageNumOfNum, page + 1, numPages));
            binding.prevPage.setEnabled(page > 0);
            binding.nextPage.setEnabled(page + 1 < numPages);
        });
    }

    /** highlights button */
    private void highlightButton(final MaterialButton button, final boolean highlight) {
        if (button != null) {
            button.setIconTint(highlight ? ColorStateList.valueOf(accentColor) : buttoncolorDefault);
        }
    }

    @Override
    protected void onDestroy() {
        DataStore.releaseDatabase(true);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putParcelable(BUNDLE_TOOLKIT, toolkit);
    }
}
