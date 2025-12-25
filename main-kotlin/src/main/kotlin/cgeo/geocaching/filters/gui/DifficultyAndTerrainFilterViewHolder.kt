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
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.TerrainGeocacheFilter
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils

import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout

import androidx.annotation.NonNull

import org.apache.commons.lang3.tuple.ImmutablePair

class DifficultyAndTerrainFilterViewHolder : BaseFilterViewHolder()<DifficultyAndTerrainGeocacheFilter> {

    private var diffView: ItemRangeSelectorViewHolder<Float, DifficultyGeocacheFilter> = null
    private var terrainView: ItemRangeSelectorViewHolder<Float, TerrainGeocacheFilter> = null
    private var includeCheckbox: ImmutablePair<View, CheckBox> = null

    override     public View createView() {
        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        includeCheckbox = ViewUtils.createCheckboxItem(getActivity(), ll, TextParam.id(R.string.cache_filter_difficulty_terrain_include_caches_without_dt), null, null)
        includeCheckbox.right.setChecked(false)
        ll.addView(includeCheckbox.left)

        diffView = createItemRangeSelectorLayout(GeocacheFilterType.DIFFICULTY, TextParam.text("D"), ll)
        terrainView = createItemRangeSelectorLayout(GeocacheFilterType.TERRAIN, TextParam.text("T"), ll)
        terrainView.removeScaleLegend()

        return ll
    }

    override     public Unit setViewFromFilter(final DifficultyAndTerrainGeocacheFilter filter) {
        if (diffView != null) {
            diffView.setViewFromFilter(filter.difficultyGeocacheFilter)
            includeCheckbox.right.setChecked(Boolean.TRUE == (filter.difficultyGeocacheFilter.getIncludeSpecialNumber()))
        }
        if (terrainView != null) {
            terrainView.setViewFromFilter(filter.terrainGeocacheFilter)
        }
    }

    override     public DifficultyAndTerrainGeocacheFilter createFilterFromView() {
        val filter: DifficultyAndTerrainGeocacheFilter = createFilter()
        filter.difficultyGeocacheFilter = diffView.createFilterFromView()
        filter.terrainGeocacheFilter = terrainView.createFilterFromView()

        val include: Boolean = includeCheckbox.right.isChecked() ? true : null
        filter.difficultyGeocacheFilter.setSpecialNumber(0f, include)
        filter.terrainGeocacheFilter.setSpecialNumber(0f, include)
        return filter
    }

    @SuppressWarnings("unchecked")
    private <T : IGeocacheFilter()> ItemRangeSelectorViewHolder<Float, T> createItemRangeSelectorLayout(final GeocacheFilterType filterType, final TextParam textParam, final ViewGroup viewGroup) {
        // create view holder
        val linearLayout: LinearLayout = LinearLayout(getActivity())
        linearLayout.setOrientation(LinearLayout.HORIZONTAL)
        viewGroup.addView(linearLayout)

        // create label
        linearLayout.addView(ViewUtils.createTextItem(getActivity(), R.style.text_label, textParam))

        // create range selector
        val rangeView: ItemRangeSelectorViewHolder<Float, T> =
                (ItemRangeSelectorViewHolder<Float, T>) FilterViewHolderCreator.createFor(filterType, getActivity())
        linearLayout.addView(rangeView.getView(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return rangeView
    }

    override     public Unit setAdvancedMode(final Boolean isAdvanced) {
        includeCheckbox.left.setVisibility(isAdvanced ? View.VISIBLE : View.GONE)
        if (!isAdvanced) {
            includeCheckbox.right.setChecked(false)
        }
    }

    override     public Boolean canBeSwitchedToBasicLossless() {
        return !includeCheckbox.right.isChecked()
    }

}
