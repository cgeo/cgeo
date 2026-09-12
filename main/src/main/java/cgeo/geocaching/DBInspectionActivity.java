package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.DbinspectionActivityBinding;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.extension.OneTimeDialogs;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.movingbits.grid.CellRef;
import com.movingbits.grid.ColumnType;
import com.movingbits.grid.DatabaseGrid;
import com.movingbits.grid.Grid;
import com.movingbits.grid.GridColumn;
import com.movingbits.grid.GridView;
import com.movingbits.grid.OnRowActionListener;
import com.movingbits.grid.SortCriterion;
import com.movingbits.grid.SortDirection;
import com.movingbits.grid.SqlGrid;
import com.movingbits.grid.SqlSnippetTarget;
import com.movingbits.grid.SqlStatement;
import org.apache.commons.lang3.Strings;
import org.json.JSONArray;
import org.json.JSONException;

public class DBInspectionActivity extends AbstractActionBarActivity  {

    private static final int ROWS_PER_PAGE = 10;

    // config settings
    private static final String CONFIG_PREFIX = "sql";
    private static final String CONFIG_SNIPPET = CONFIG_PREFIX + ".snippets";
    private static final String CONFIG_TABLE_PREFIX = CONFIG_PREFIX + ".table.";
    private static final int MAX_SNIPPETS = 5;

    private GridView gridView;
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

        databaseGrid = new SqlGrid()
                .onSnippetSave(this::storeSnippet)
                .onSnippetLoad(this::chooseSnippet)
                .onEditorClosed(executed -> setTitle(databaseGrid.hasCurrentTable() ? LocalizationUtils.getString(R.string.view_database) + ": " + databaseGrid.getCurrentTable() : LocalizationUtils.getString(R.string.dbi_sql_editor)))
                .setDatabase(DataStore.getDatabase(true))
                .onTablesLoaded(tables -> binding.activityContent.post(() -> {
                    if (databaseGrid != null && !databaseGrid.hasCurrentTable()) {
                        showTableSelection(tables);
                    }
                }))
                .onConfigurationChanged(json -> {
                    Settings.putStringDirect(databaseConfigKey(), json);
                    highlightButton(binding.buttonConfigColumns, grid().hasHiddenColumns());
                })
                .onSearchChanged(searchConfig -> highlightButton(binding.buttonSearch, !searchConfig.isEmpty()))
                .rowsPerPage(ROWS_PER_PAGE)
                .alternatingRowColors(true)
                .adjustableFixedBoundary(true)
                .onRowAction(this::askDeleteRow)
                .onRowActionPerformed((type, key, success) -> {
                    Log.e("SQL Action: type=" + type + " on " + keyText(key) + ": " + success);
                    if (success) {
                        showHint(LocalizationUtils.getString(R.string.dbi_delete_success, keyText(key)));
                    }
                })
                .onCellLongClick(this::showCell)
                .onSortChanged(order -> showHint(addSortIndicator(order)));
        databaseGrid.readState(savedInstanceState);
        gridView = new GridView(this);
        gridView.setGrid(databaseGrid);
        binding.gridContainer.addView(gridView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        gridView.readState(savedInstanceState);
        configureUI();
    }

    // ------------------------------------------------------ table selection

    /** table selection for DatabaseGrid */
    private void showTableSelection(final List<String> tables) {
        if (tables.isEmpty()) {
            showHint(LocalizationUtils.getString(R.string.dbi_no_tables));
            return;
        }
        final SimpleDialog.ItemSelectModel<String> model = new SimpleDialog.ItemSelectModel<>();
        model.setItems(tables).setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);
        SimpleDialog.of(this).setTitle(R.string.dbi_select_table).selectSingle(model, this::selectTable);
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
        return CONFIG_TABLE_PREFIX + (databaseGrid == null ? "_" : databaseGrid.getCurrentTable());
    }

    // ------------------------------------------------------------ row action

    /** confirmation dialog for delete row */
    private void askDeleteRow(final int number, final int type, final Map<String, String> key, final Map<String, String> columns) {
        if (type != OnRowActionListener.DELETE || databaseGrid == null || key.isEmpty()) {
            return;
        }
        SimpleDialog.of(this)
                .setTitle(R.string.dbi_delete_row_title)
                .setMessage(TextParam.text(LocalizationUtils.getString(R.string.dbi_delete_row_question, number, keyText(key)) + "\n\n" + columnsText(columns)))
                .setPositiveButton(TextParam.id(R.string.delete))
                .confirm(() -> databaseGrid.performRowAction(type, key));
    }

    /** The rest of the row, one field per line, for the question. */
    private static String columnsText(final Map<String, String> columns) {
        final StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> field : columns.entrySet()) {
            text.append(text.length() == 0 ? "" : "\n").append(field.getKey()).append(": ").append(field.getValue());
        }
        return text.toString();
    }

    /** The primary key as one line, for the hint and the log. */
    private static String keyText(final Map<String, String> key) {
        final StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> field : key.entrySet()) {
            text.append(text.length() == 0 ? "" : ", ").append(field.getKey()).append("=").append(field.getValue());
        }
        return text.toString();
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

    // -------------------------------------------------------------- snippets

    /** keep last MAX_SNIPPETS in preferences */
    private void storeSnippet(final String statement) {
        final List<String> stored = storedSnippets();
        // The same statement twice would only take a place away from another one.
        stored.remove(statement);
        stored.add(0, statement);
        while (stored.size() > MAX_SNIPPETS) {
            stored.remove(stored.size() - 1);
        }
        final JSONArray array = new JSONArray();
        for (String snippet : stored) {
            array.put(snippet);
        }
        Settings.putStringDirect(CONFIG_SNIPPET, array.toString());
        showHint(LocalizationUtils.getString(R.string.dbi_sqleditor_snippet_saved));
    }

    /** select a snippet and update editor */
    private void chooseSnippet(final SqlSnippetTarget editor) {
        final List<String> stored = storedSnippets();
        if (stored.isEmpty()) {
            showHint(LocalizationUtils.getString(R.string.dbi_sqleditor_no_snippets));
            return;
        }


        final SimpleDialog.ItemSelectModel<String> model = new SimpleDialog.ItemSelectModel<>();
        model.setItems(stored).setDisplayMapper((which) -> {
            for (String snippet : stored) {
                if (Strings.CI.equals(snippet, which)) {
                    return TextParam.text(SqlStatement.parse(snippet).render().sql());
                }
            }
            return TextParam.text("?");
        }).setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);
        SimpleDialog.of(this).setTitle(R.string.dbi_sqleditor_load_snippet).setNeutralButton(TextParam.id(R.string.cancel)).selectSingle(model, editor::load);
    }

    /** get stored snippets from preferences, most recent one first. */
    private List<String> storedSnippets() {
        final List<String> stored = new ArrayList<>();
        try {
            final JSONArray array = new JSONArray(Settings.getStringDirect(CONFIG_SNIPPET, ""));
            for (int i = 0; i < array.length(); i++) {
                final String snippet = array.optString(i, "");
                if (!snippet.isEmpty()) {
                    stored.add(snippet);
                }
            }
        } catch (JSONException ignore) {
            // Nothing has been stored yet, or not in a form that can be read.
        }
        return stored;
    }

    // -------------------------------------------------------------- other

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

        // The table selection and SQLEditor button only exist when there are tables to choose from.
        binding.buttonSelectTable.setVisibility(databaseGrid == null ? View.GONE : View.VISIBLE);
        binding.buttonSelectTable.setOnClickListener(v -> showTableSelection(databaseGrid.getTables()));
        binding.buttonSqlEditor.setVisibility(databaseGrid == null ? View.GONE : View.VISIBLE);
        binding.buttonSqlEditor.setOnClickListener(v -> showSqlEditor());

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

    private void showSqlEditor() {
        Dialogs.basicOneTimeMessage(this, OneTimeDialogs.DialogType.SQLEDITOR_WARNING, () -> {
            setTitle(LocalizationUtils.getString(R.string.dbi_sql_editor));
            gridView.showSqlEditor();
        }, true);
    }

    /** highlights button */
    private void highlightButton(final MaterialButton button, final boolean highlight) {
        if (button != null) {
            button.setIconTint(highlight ? ColorStateList.valueOf(accentColor) : buttoncolorDefault);
        }
    }

    @Override
    protected void onDestroy() {
        if (databaseGrid != null) {
            databaseGrid.close();
        }
        DataStore.releaseDatabase(true);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        grid().addState(outState);
        gridView.addState(outState);
    }
}
