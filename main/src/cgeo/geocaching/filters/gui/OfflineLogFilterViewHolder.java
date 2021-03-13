package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheFilterOfflineLogBinding;
import cgeo.geocaching.filters.core.OfflineLogGeocacheFilter;
import cgeo.geocaching.filters.core.StringFilter;
import cgeo.geocaching.ui.TextSpinner;

import android.view.LayoutInflater;
import android.view.View;

import java.util.Arrays;

import org.apache.commons.text.WordUtils;


public class OfflineLogFilterViewHolder extends BaseFilterViewHolder<OfflineLogGeocacheFilter> {

    private final TextSpinner<StringFilter.StringFilterType> selectSpinner = new TextSpinner<>();

    private CacheFilterOfflineLogBinding binding;

    @Override
    public void setViewFromFilter(final OfflineLogGeocacheFilter filter) {
        final StringFilter stringFilter = filter.getLogTextFilter();
        binding.hasOfflineLog.setChecked(filter.hasOfflineLogFilter());
        binding.logStringFilter.text.setText(stringFilter.getTextValue());
        selectSpinner.set(stringFilter.getFilterType());
        binding.logStringFilter.matchCase.setChecked(stringFilter.isMatchCase());
    }

    @Override
    public View createView() {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.cache_filter_offline_log, null);
        this.binding = CacheFilterOfflineLogBinding.bind(view);
        selectSpinner.setSpinner(this.binding.logStringFilter.select)
            .setValues(Arrays.asList(StringFilter.StringFilterType.values()))
            .setDisplayMapper(v -> WordUtils.capitalizeFully(v.name().replace('_', ' ')));
        return view;
    }

    @Override
    public OfflineLogGeocacheFilter createFilterFromView() {
        final OfflineLogGeocacheFilter filter = createFilter();
        final StringFilter stringFilter = filter.getLogTextFilter();
        stringFilter.setTextValue(binding.logStringFilter.text.getText().toString());
        stringFilter.setFilterType(selectSpinner.get());
        stringFilter.setMatchCase(binding.logStringFilter.matchCase.isChecked());
        filter.setOfflineLogFilter(binding.hasOfflineLog.isChecked());
        return filter;
    }

}
