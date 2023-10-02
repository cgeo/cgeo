package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;


public class NamedFilterFilterViewHolder extends BaseFilterViewHolder<NamedFilterGeocacheFilter> {

    private final TextSpinner<GeocacheFilter> selectSpinner = new TextSpinner<>();

    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        final Button button = ViewUtils.createButton(getActivity(), ll, TextParam.text(""));
        button.setMinimumWidth(ViewUtils.dpToPixel(200));
        selectSpinner.setTextView(button);

        final List<GeocacheFilter> namedFilters = new ArrayList<>(GeocacheFilter.Storage.getStoredFilters());
        TextUtils.sortListLocaleAware(namedFilters, GeocacheFilter::getName);
        namedFilters.add(0, null);
        selectSpinner
                .setValues(namedFilters)
                .set(null)
                .setDisplayMapper(f ->
                        f == null ? LocalizationUtils.getString(R.string.cache_filter_userdisplay_none) : f.getName());
        final LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llParams.bottomMargin = ViewUtils.dpToPixel(10);
        ll.addView(button, llParams);

        return ll;
    }

    @Override
    public void setViewFromFilter(final NamedFilterGeocacheFilter filter) {
        selectSpinner.set(filter.getNamedFilter());
    }

    @Override
    public NamedFilterGeocacheFilter createFilterFromView() {
        final NamedFilterGeocacheFilter filter = createFilter();
        filter.setNamedFilter(selectSpinner.get());
        return filter;
    }

}
