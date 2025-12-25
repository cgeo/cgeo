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

package cgeo.geocaching.filters.gui

import cgeo.geocaching.R
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.filters.core.DifficultyTerrainMatrixGeocacheFilter
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.CheckboxMatrixView
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.LocalizationUtils

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout

import androidx.annotation.NonNull

import org.apache.commons.lang3.tuple.ImmutablePair

class DifficultyTerrainMatrixFilterViewHolder : BaseFilterViewHolder()<DifficultyTerrainMatrixGeocacheFilter> {

    private var includeCheckbox: ImmutablePair<View, CheckBox> = null

    private var matrix: CheckboxMatrixView = null

    override     public View createView() {
        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        includeCheckbox = ViewUtils.createCheckboxItem(getActivity(), ll, TextParam.id(R.string.cache_filter_difficulty_terrain_include_caches_without_dt), null, null)
        includeCheckbox.right.setChecked(true)
        ll.addView(includeCheckbox.left)

        val fillButton: Button = ViewUtils.createButton(getActivity(), ll, TextParam.id(R.string.cache_filter_difficulty_terrain_matrix_fill_with_gc_data))
        val gcLiveMatrixEnabled: Boolean = GCConnector.getInstance().isActive() && Settings.isGCPremiumMember()
        fillButton.setEnabled(gcLiveMatrixEnabled)
        fillButton.setOnClickListener(v -> {
            if (gcLiveMatrixEnabled) {
                AndroidRxUtils.andThenOnUi(AndroidRxUtils.networkScheduler,
                    () -> GCConnector.getInstance().getNeededDifficultyTerrainCombisFor81Matrix(),
                    neededCombis -> matrix.changeData(true, changer -> {
                        for (ImmutablePair<Float, Float> neededCombi : neededCombis) {
                            changer.call(getRowColumnForDifficultyTerrain(neededCombi.left), getRowColumnForDifficultyTerrain(neededCombi.right), true)
                        }
                }))
            }
        })
        ll.addView(fillButton)

        //matrix
        final String[] labels = String[] { "1", ".", "2", ".", "3", ".", "4", ".", "5"}
        matrix = CheckboxMatrixView(getActivity())
        matrix.setRowsColumns(labels, labels)
        matrix.setLabels(LocalizationUtils.getString(R.string.cache_difficulty), LocalizationUtils.getString(R.string.cache_terrain))

        final LinearLayout.LayoutParams matrixLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        matrixLp.gravity = Gravity.CENTER
        ll.addView(matrix, matrixLp)

        return ll
    }

    private static Int getRowColumnForDifficultyTerrain(final Float difficultyTerrain) {
        if (difficultyTerrain == null || difficultyTerrain < 1 || difficultyTerrain > 5) {
            return -1
        }
        return (Int) (difficultyTerrain / 0.5f - 1.9f) 

    }

    override     public Unit setViewFromFilter(final DifficultyTerrainMatrixGeocacheFilter filter) {

        includeCheckbox.right.setChecked(filter.isIncludeCachesWoDt())
        matrix.changeData(false, changer -> {
            for (Int r = 0; r < 9; r++) {
                for (Int c = 0; c < 9; c++) {
                    changer.call(r, c, filter.hasDtCombi(getDtFromRowColumn(r), getDtFromRowColumn(c)))
                }
            }
        })
    }

    override     public DifficultyTerrainMatrixGeocacheFilter createFilterFromView() {
        val filter: DifficultyTerrainMatrixGeocacheFilter = createFilter()
        filter.setIncludeCachesWoDt(includeCheckbox.right.isChecked())
        filter.clearDtCombis()
        final Boolean[][] data = matrix.getData()
        for (Int r = 0; r < 9; r++) {
            for (Int c = 0; c < 9; c++) {
                if (data[r][c]) {
                    filter.addDtCombi(getDtFromRowColumn(r), getDtFromRowColumn(c))
                }
            }
        }
        return filter
    }

    public Float getDtFromRowColumn(final Int rowColumn) {
        return ((Float) rowColumn) * 0.5f + 1f
    }

 }
