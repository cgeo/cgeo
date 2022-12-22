package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheFilterGenericStringBinding;
import cgeo.geocaching.filters.core.StringFilter;
import cgeo.geocaching.filters.core.StringGeocacheFilter;
import cgeo.geocaching.search.AutoCompleteAdapter;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Func1;

import android.view.View;
import android.widget.AutoCompleteTextView;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.annotation.Nullable;

import java.util.Arrays;


public class StringFilterViewHolder<F extends StringGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final TextSpinner<StringFilter.StringFilterType> selectSpinner = new TextSpinner<>();
    private final Func1<String, String[]> autoTextCompleteFunction;

    private CacheFilterGenericStringBinding binding;


    public StringFilterViewHolder() {
        this(null);
    }

    public StringFilterViewHolder(@Nullable final Func1<String, String[]> autoTextCompleteFunction) {
        this.autoTextCompleteFunction = autoTextCompleteFunction;
    }

    @Override
    public void setViewFromFilter(final F filter) {
        final StringFilter stringFilter = filter.getStringFilter();
        binding.searchtext.getEditText().setText(stringFilter.getTextValue());
        selectSpinner.set(stringFilter.getFilterType());
        binding.matchCase.setChecked(stringFilter.isMatchCase());
    }

    @Override
    public View createView() {
        final View view = inflateLayout(R.layout.cache_filter_generic_string);

        this.binding = CacheFilterGenericStringBinding.bind(view);
        selectSpinner.setTextView(this.binding.select);
        selectSpinner
                .setValues(Arrays.asList(StringFilter.StringFilterType.values()))
                .setDisplayMapper(StringFilter.StringFilterType::toUserDisplayableString)
                .setChangeListener(sft -> {
                    final boolean textEnabled = sft != StringFilter.StringFilterType.IS_NOT_PRESENT && sft != StringFilter.StringFilterType.IS_PRESENT;
                    binding.searchtext.setVisibility(textEnabled ? VISIBLE : GONE);
                    binding.matchCase.setVisibility(textEnabled ? VISIBLE : GONE);
                }, true)
                .set(StringFilter.getDefaultFilterType());
        this.binding.itemInfo.setOnClickListener(d -> SimpleDialog.of(getActivity()).setMessage(R.string.cache_filter_stringfilter_info).show());

        //initialize autocomplete,
        if (this.autoTextCompleteFunction != null) {
            final AutoCompleteTextView actv = (AutoCompleteTextView) binding.searchtext.getEditText();
            final AutoCompleteAdapter adapter = new AutoCompleteAdapter(getActivity(), android.R.layout.simple_dropdown_item_1line, this.autoTextCompleteFunction);
            actv.setAdapter(adapter);
        }

        return view;
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final StringFilter stringFilter = filter.getStringFilter();
        stringFilter.setTextValue(binding.searchtext.getEditText().getText().toString());
        stringFilter.setFilterType(selectSpinner.get());
        stringFilter.setMatchCase(binding.matchCase.isChecked());
        return filter;
    }

}
