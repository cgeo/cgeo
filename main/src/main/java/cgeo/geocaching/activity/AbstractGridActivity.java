package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.DbinspectionActivityBinding;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;

import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.movingbits.grid.Grid;
import com.movingbits.grid.GridView;
import com.movingbits.grid.SortCriterion;
import com.movingbits.grid.SortDirection;

/**
 * Common base for activities showing a {@link Grid} with the control bar of
 * {@code dbinspection_activity.xml} (paging, column configuration, search).
 */
public abstract class AbstractGridActivity extends AbstractActionBarActivity {

    protected DbinspectionActivityBinding binding;
    protected GridView gridView;
    private ColorStateList buttoncolorDefault;
    private int accentColor;
    private Toast hint;

    /** inflates layout, sets title and home button - call this first in onCreate */
    protected void setupLayout(@StringRes final int title) {
        binding = DbinspectionActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(title);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    /** creates grid view for given grid, restores view state and configures the common part of the control bar */
    protected void attachGrid(@NonNull final Grid grid, @Nullable final Bundle savedInstanceState) {
        gridView = new GridView(this);
        gridView.setGrid(grid);
        binding.gridContainer.addView(gridView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        gridView.readState(savedInstanceState);
        configureControlBar();
    }

    /** configure common buttons of control bar at the bottom */
    private void configureControlBar() {
        binding.controlBar.setVisibility(View.VISIBLE);
        binding.prevPage.setOnClickListener(v -> gridView.previousPage());
        binding.nextPage.setOnClickListener(v -> gridView.nextPage());
        binding.buttonConfigColumns.setOnClickListener(v -> gridView.showColumnSettings());
        binding.buttonSearch.setOnClickListener(v -> gridView.showSearch());

        // The ordinary color comes from the theme; highlighting uses the accent color.
        buttoncolorDefault = binding.buttonConfigColumns.getIconTint();
        accentColor = MaterialColors.getColor(binding.buttonConfigColumns, com.google.android.material.R.attr.colorSecondary, buttoncolorDefault.getDefaultColor());
        refreshButtonHighlights();

        gridView.setOnPageChangedListener((page, numPages) -> {
            binding.pageNumOfNum.setText(LocalizationUtils.getString(R.string.dbi_pageNumOfNum, page + 1, numPages));
            binding.prevPage.setEnabled(page > 0);
            binding.nextPage.setEnabled(page + 1 < numPages);
        });
    }

    /** get current grid */
    protected Grid grid() {
        return gridView.getGrid();
    }

    /** highlight column config and search buttons according to current grid state */
    protected void refreshButtonHighlights() {
        highlightButton(binding.buttonConfigColumns, grid().hasHiddenColumns());
        highlightButton(binding.buttonSearch, grid().isSearching());
    }

    /** highlights button */
    protected void highlightButton(final MaterialButton button, final boolean highlight) {
        if (button != null && buttoncolorDefault != null) {
            button.setIconTint(highlight ? ColorStateList.valueOf(accentColor) : buttoncolorDefault);
        }
    }

    /** Add sort indicator to column title */
    protected String addSortIndicator(final List<SortCriterion> order) {
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
    protected void showHint(final String text) {
        if (hint != null) {
            hint.cancel();
        }
        hint = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        hint.show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (gridView != null) {
            grid().addState(outState);
            gridView.addState(outState);
        }
    }
}
