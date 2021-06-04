package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.ui.ButtonToggleGroup;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LogEntryFilterViewHolder extends BaseFilterViewHolder<LogEntryGeocacheFilter> {

    private ButtonToggleGroup inverseFoundBy;
    private EditText foundByText;
    private EditText logText;


    @Override
    public View createView() {

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        inverseFoundBy = new ButtonToggleGroup(getActivity());
        inverseFoundBy.addButtons(R.string.cache_filter_include, R.string.cache_filter_exclude);

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(5));
        ll.addView(inverseFoundBy, llp);

        final TextView label1 = new TextView(getActivity(), null, 0, R.style.text_label);
        label1.setText(R.string.cache_filter_log_entry_foundby);

        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(0));
        ll.addView(label1, llp);

        foundByText = new EditText(getActivity(), null, 0, R.style.edittext_full);
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(5));
        ll.addView(foundByText, llp);

        final TextView label2 = new TextView(getActivity(), null, 0, R.style.text_label);
        label2.setText(R.string.cache_filter_log_entry_logtext);

        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, 0);
        ll.addView(label2, llp);

        logText = new EditText(getActivity(), null, 0, R.style.edittext_full);
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(20));
        ll.addView(logText, llp);

        return ll;
    }

    @Override
    public void setViewFromFilter(final LogEntryGeocacheFilter filter) {
        foundByText.setText(filter.getFoundByUser());
        logText.setText(filter.getLogText());
        inverseFoundBy.setCheckedButtonByIndex(filter.isInverse() ? 1 : 0, true);
    }

    @Override
    public LogEntryGeocacheFilter createFilterFromView() {
        final LogEntryGeocacheFilter filter = createFilter();
        filter.setFoundByUser(foundByText.getText().toString());
        filter.setLogText(logText.getText().toString());
        filter.setInverse(inverseFoundBy.getCheckedButtonIndex() == 1);
        return filter;
    }

}
