package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheFilterGenericStringBinding;
import cgeo.geocaching.filters.core.StringFilter;
import cgeo.geocaching.filters.core.StringGeocacheFilter;
import cgeo.geocaching.ui.TextSpinner;

import android.view.LayoutInflater;
import android.view.View;

import java.util.Arrays;

import org.apache.commons.text.WordUtils;


public class StringFilterViewHolder<F extends StringGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final TextSpinner<StringFilter.StringFilterType> selectSpinner = new TextSpinner<>();

    private CacheFilterGenericStringBinding binding;

    @Override
    public void setViewFromFilter(final F filter) {
        final StringFilter stringFilter = filter.getStringFilter();
        binding.text.setText(stringFilter.getTextValue());
        selectSpinner.set(stringFilter.getFilterType());
        binding.matchCase.setChecked(stringFilter.isMatchCase());
    }

    @Override
    public View createView() {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.cache_filter_generic_string, null);
        this.binding = CacheFilterGenericStringBinding.bind(view);
        selectSpinner.setSpinner(this.binding.select)
            .setValues(Arrays.asList(StringFilter.StringFilterType.values()))
            .setDisplayMapper(v -> WordUtils.capitalizeFully(v.name().replace('_', ' ')));
        return view;
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final StringFilter stringFilter = filter.getStringFilter();
        stringFilter.setTextValue(binding.text.getText().toString());
        stringFilter.setFilterType(selectSpinner.get());
        stringFilter.setMatchCase(binding.matchCase.isChecked());
        return filter;
    }

}
