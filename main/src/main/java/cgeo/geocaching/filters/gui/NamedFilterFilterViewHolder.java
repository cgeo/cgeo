package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.FilterUtils;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;


public class NamedFilterFilterViewHolder extends BaseFilterViewHolder<NamedFilterGeocacheFilter> {

    private NamedFilter selectedFilter = null;
    private Button selectButton;

    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        selectButton = ViewUtils.createButton(getActivity(), ll, TextParam.text(""));
        selectButton.setMinimumWidth(ViewUtils.dpToPixel(200));
        updateButtonLabel();

        selectButton.setOnClickListener(v ->
            FilterUtils.openDialogSelectNamedFilter(
                getActivity(),
                TextParam.id(R.string.named_filter_select_title),
                filter -> {
                    selectedFilter = filter;
                    updateButtonLabel();
                }));

        final LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llParams.bottomMargin = ViewUtils.dpToPixel(10);
        ll.addView(selectButton, llParams);

        return ll;
    }

    private void updateButtonLabel() {
        if (selectButton != null) {
            selectButton.setText(selectedFilter == null
                    ? LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
                    : selectedFilter.getNameAndMarker());
        }
    }

    @Override
    public void setViewFromFilter(final NamedFilterGeocacheFilter filter) {
        selectedFilter = NamedFilter.getById(filter.getNamedFilterId());
        updateButtonLabel();
    }

    @Override
    public NamedFilterGeocacheFilter createFilterFromView() {
        final NamedFilterGeocacheFilter filter = createFilter();
        filter.setNamedFilterId(selectedFilter == null ? 0 : selectedFilter.getId());
        return filter;
    }

}
