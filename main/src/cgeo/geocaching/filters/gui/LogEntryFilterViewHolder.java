package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.ui.ButtonToggleGroup;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

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

        final Pair<View, EditText> foundByField = ViewUtils.createTextField(getActivity(), null, TextParam.id(R.string.cache_filter_log_entry_foundby), null, -1, 1, 1);
        foundByText = foundByField.second;
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(5));
        ll.addView(foundByField.first, llp);

        final Pair<View, EditText> logTextField = ViewUtils.createTextField(getActivity(), null, TextParam.id(R.string.cache_filter_log_entry_logtext), null, -1, 1, 1);
        logText = logTextField.second;
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(20));
        ll.addView(logTextField.first, llp);

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
