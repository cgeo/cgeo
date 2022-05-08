package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.TerrainGeocacheFilter;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class DifficultyAndTerrainFilterViewHolder extends BaseFilterViewHolder<DifficultyAndTerrainGeocacheFilter> {

    private ItemRangeSelectorViewHolder<Float, DifficultyGeocacheFilter> diffView = null;
    private ItemRangeSelectorViewHolder<Float, TerrainGeocacheFilter> terrainView = null;
    private ImmutablePair<View, CheckBox> includeCheckbox = null;

    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        includeCheckbox = ViewUtils.createCheckboxItem(getActivity(), ll, TextParam.id(R.string.cache_filter_difficulty_terrain_include_caches_without_dt), null, null);
        includeCheckbox.right.setChecked(false);
        ll.addView(includeCheckbox.left);

        diffView = createItemRangeSelectorLayout(GeocacheFilterType.DIFFICULTY, TextParam.text("D"), ll);
        terrainView = createItemRangeSelectorLayout(GeocacheFilterType.TERRAIN, TextParam.text("T"), ll);
        terrainView.removeScaleLegend();

        return ll;
    }

    @Override
    public void setViewFromFilter(@NonNull final DifficultyAndTerrainGeocacheFilter filter) {
        if (diffView != null) {
            diffView.setViewFromFilter(filter.difficultyGeocacheFilter);
            includeCheckbox.right.setChecked(Boolean.TRUE.equals(filter.difficultyGeocacheFilter.getIncludeSpecialNumber()));
        }
        if (terrainView != null) {
            terrainView.setViewFromFilter(filter.terrainGeocacheFilter);
        }
    }

    @Override
    public DifficultyAndTerrainGeocacheFilter createFilterFromView() {
        final DifficultyAndTerrainGeocacheFilter filter = createFilter();
        filter.difficultyGeocacheFilter = (DifficultyGeocacheFilter) diffView.createFilterFromView();
        filter.terrainGeocacheFilter = (TerrainGeocacheFilter) terrainView.createFilterFromView();

        final Boolean include = includeCheckbox.right.isChecked() ? true : null;
        filter.difficultyGeocacheFilter.setSpecialNumber(0f, include);
        filter.terrainGeocacheFilter.setSpecialNumber(0f, include);
        return filter;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private <T extends IGeocacheFilter> ItemRangeSelectorViewHolder<Float, T> createItemRangeSelectorLayout(final GeocacheFilterType filterType, final TextParam textParam, final ViewGroup viewGroup) {
        // create view holder
        final LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        viewGroup.addView(linearLayout);

        // create label
        linearLayout.addView(ViewUtils.createTextItem(getActivity(), R.style.text_label, textParam));

        // create range selector
        final ItemRangeSelectorViewHolder<Float, T> rangeView =
                (ItemRangeSelectorViewHolder<Float, T>) FilterViewHolderCreator.createFor(filterType, getActivity());
        linearLayout.addView(rangeView.getView(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return rangeView;
    }

    @Override
    public void setAdvancedMode(final boolean isAdvanced) {
        includeCheckbox.left.setVisibility(isAdvanced ? View.VISIBLE : View.GONE);
        if (!isAdvanced) {
            includeCheckbox.right.setChecked(false);
        }
    }

    @Override
    public boolean canBeSwitchedToBasicLossless() {
        return !includeCheckbox.right.isChecked();
    }

}
